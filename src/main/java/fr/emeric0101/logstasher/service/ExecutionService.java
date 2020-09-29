package fr.emeric0101.logstasher.service;

import fr.emeric0101.logstasher.dto.ExecutionQueue;
import fr.emeric0101.logstasher.dto.LogstashRunning;
import fr.emeric0101.logstasher.dto.RestRequest;
import fr.emeric0101.logstasher.entity.Batch;
import fr.emeric0101.logstasher.entity.BatchArchive;
import fr.emeric0101.logstasher.repository.BatchRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
public class ExecutionService {


    @Autowired
    SimpMessagingTemplate template;

    @Autowired
    ExecutionQueueSerializer executionQueueSerializer;

    @Autowired
    ArchiveService archiveService;

    @Autowired
    LogstashService logstashService;

    @Autowired
    RestService restService;

    @Autowired
    BatchRepository batchRepository;

    @Autowired
            MailService mailService;

    String startDate = null;

    private Iterator<ExecutionQueue.ExecutionBatch> queueIterator;
    private ExecutionQueue.ExecutionBatch currentBatch;
    private ExecutionQueue currentQueue;
    private BatchArchive currentBatchArchive;



    /**
     * If a batchQueue is running, go into next step
     *
     * @param exitValue
     */
    private void processEnded(int exitValue) {
        if (currentBatch != null) {
            if (exitValue != 0) {
                currentBatch.setState("ERROR");
            } else {
                currentBatch.setState("DONE");
            }
            currentBatchArchive.setEndTime(new Date());
            currentBatchArchive.setState(currentBatch.getState());
            archiveService.save(currentBatchArchive);
            sendState();
            // next step ?
            if (queueIterator.hasNext()) {
                currentBatch = queueIterator.next();
                // start again :)
                startBatch(currentBatch);
            } else {
                // batch has error ?
                // save the log of the current batch
                if (currentQueue.getQueue().stream().anyMatch(e -> e.getState().equals("ERROR"))) {
                    changeState("ERROR");
                } else {
                    changeState("DONE");
                }
                if (currentQueue != null) {
                    currentQueue.getEndCallback().accept("DONE");
                }
                currentBatch = null;
                currentQueue = null;
            }

        } else {
            // pipeline
            if (exitValue != 0) {
                changeState("ERROR");

            } else {
                changeState("STOPPED");

            }
        }


    }


    public void restart() {
        startDate = executionQueueSerializer.getDate();

        stopLogstash(false);
        logstashService.clearBuffer();
        startBatch(null);
    }


    public LogstashRunning getRunning() {
        LogstashRunning running = new LogstashRunning();
        running.setState(logstashService.getState());
        running.setQueue(currentQueue);
        running.setBuffer(logstashService.getBuffer());

        return running;
    }

    /**
     *
     * @param queue
     */
    public void startFromQueue(ExecutionQueue queue) {
        startDate = executionQueueSerializer.getDate();
        if (!queue.getQueue().iterator().hasNext()) {
            return;
        }
        stopLogstash(false);
        logstashService.clearBuffer();
        currentQueue = queue;

        queueIterator = queue.getQueue().iterator();

        currentBatch = queueIterator.next();
        startBatch(currentBatch);
    }

    public void stopLogstash(boolean continueQueue) {
        logstashService.stopLogstash();

        if (currentBatchArchive != null && currentBatchArchive.getEndTime() == null) {
            currentBatchArchive.setEndTime(new Date());
            currentBatchArchive.setState("INTERRUPTED");
            archiveService.save(currentBatchArchive);
            sendState();
        }

        // continue queue ?
        if (continueQueue && queueIterator != null && queueIterator.hasNext()) {
            currentBatch = queueIterator.next();
            // start again :)
            startBatch(currentBatch);
        } else {
            currentQueue = null;
            currentBatch = null;
            currentBatchArchive = null;
            // trigge queue ended
        }

    }

    private void startBatch(ExecutionQueue.ExecutionBatch currentBatch) {
        // Hook before running
        runHookRequests(currentBatch.getBatch(), false);
        // save archive
        currentBatchArchive = archiveService.saveArchive(currentBatch.getBatch(), new Date(), null, currentBatch.getState());
        executionQueueSerializer.saveLog(startDate, currentBatch.getBatch().getId(), "Starting batch");
        changeState("STARTING");

        logstashService.start(currentBatch,
                (retval) -> {
                    if (retval == 0) {
                        currentBatchArchive.setState("DONE");
                    } else {
                        if (!currentBatch.getState().equals("INTERRUPTED")) {
                            currentBatchArchive.setState("ERROR");
                            mailService.sendSimpleMessage("emeric.baveux.external@airbus.com", "Logstasher : error on batch " + currentBatch.getBatch().getId(),
                                    "The batch " + currentBatch.getBatch().getId() + " got an error\n\n" + currentBatch.getOutput());
                        }
                    }
                    currentBatchArchive.setEndTime(new Date());
                    archiveService.save(currentBatchArchive);
                    sendState();

                    executionQueueSerializer.saveLog(startDate, currentBatch.getBatch().getId(), "End with " + retval);
                    // hook after running
                    runHookRequests(currentBatch.getBatch(), true);
                    processEnded(retval);

                },
                (newLineLog) -> {
                    changeState("RUNNING");
                    executionQueueSerializer.saveLog(startDate, currentBatch.getBatch().getId(), newLineLog);

                });
    }


    private void changeState(String state) {
        if (currentBatchArchive != null) {
            currentBatchArchive.setState(state);
            currentBatch.setState(state);
            archiveService.save(currentBatchArchive);
        }

        sendState();
    }

    /**
     * Send the current state the user
     */
    private void sendState() {
        template.convertAndSend("/state", getRunning());
    }


    /**
     * Execution hook from a request
     * @param batch
     * @param after after or before hook ?
     */
    private void runHookRequests(Batch batch, boolean after) {
        if (batch.getEntyRequests() == null) {
            return;
        }
        for (RestRequest restRequest : batch.getEntyRequests().stream().filter(e -> e.getType().equals(after ? "AFTER" : "BEFORE")).collect(Collectors.toList())) {
            try {
                String result = restService.sendRequest(restRequest);
                executionQueueSerializer.saveLog(startDate, currentBatch.getBatch().getId(), "HOOK REQUEST " + restRequest.getUrl() + "\n\tResult : " + result);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Check that no batch is stucked in logstash (timeout in minute)
     */
    public void dogWatch() {
        Date date = new Date();
        if (this.currentBatch != null && (date.getTime() - this.currentBatchArchive.getStartTime().getTime()) > currentBatch.getBatch().getTimeout() * 60000) {
            stopLogstash(true);
        }
    }

    public void init() {
        archiveService.clear();
        archiveService.save(new BatchArchive(){{
            setState("INIT");
            setStartTime(new Date());
        }});
        batchRepository.save(new Batch() {{
            setId("test");
            setActivated(false);
            setContent("input {} output {}");
            setOrder(0);
            setTimeout(60);
        }});
    }

    public void clear() {
        archiveService.clear();
        archiveService.save(new BatchArchive(){{
            setState("INIT");
            setStartTime(new Date());
        }});
    }
}
