package fr.emeric0101.logstasher.service;

import fr.emeric0101.logstasher.dto.ExecutionQueue;
import fr.emeric0101.logstasher.dto.RestRequest;
import fr.emeric0101.logstasher.entity.Batch;
import fr.emeric0101.logstasher.entity.ExecutionArchive;
import fr.emeric0101.logstasher.repository.BatchRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class BatchExecutionService {


    @Autowired
    SimpMessagingTemplate template;

    @Autowired
    ExecutionQueueSerializer executionQueueSerializer;

    @Autowired
    ExecutionArchiveService executionArchiveService;

    @Autowired
    RestService restService;

    @Autowired
    BatchRepository batchRepository;

    @Autowired
    MailService mailService;

    @Autowired
    LogstashService logstashService;


    String startDate = null;

    private Iterator<ExecutionQueue.ExecutionBatch> queueIterator;
    private ExecutionQueue.ExecutionBatch currentBatch;
    private ExecutionQueue currentQueue;
    private ExecutionArchive currentExecutionArchive;

    final String INSTANCE = "_batch";

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
            currentExecutionArchive.setEndTime(new Date());
            currentExecutionArchive.setState(currentBatch.getState());
            executionArchiveService.save(currentExecutionArchive);
            logstashService.sendState(INSTANCE);
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
        logstashService.clearBuffer(INSTANCE);
        currentQueue = queue;

        queueIterator = queue.getQueue().iterator();

        currentBatch = queueIterator.next();
        startBatch(currentBatch);
    }

    public void stopLogstash(boolean continueQueue) {
        logstashService.stop(INSTANCE);

        if (currentExecutionArchive != null && currentExecutionArchive.getEndTime() == null) {
            currentExecutionArchive.setEndTime(new Date());
            currentExecutionArchive.setState("INTERRUPTED");
            executionArchiveService.save(currentExecutionArchive);
            logstashService.sendState(INSTANCE);
        }

        // continue queue ?
        if (continueQueue && queueIterator != null && queueIterator.hasNext()) {
            currentBatch = queueIterator.next();
            // start again :)
            startBatch(currentBatch);
        } else {
            currentQueue = null;
            currentBatch = null;
            currentExecutionArchive = null;
            // trigge queue ended
        }

    }

    private void startBatch(ExecutionQueue.ExecutionBatch currentBatch) {
        // Hook before running
        runHookRequests(currentBatch.getBatch(), false);
        // save archive
        currentExecutionArchive = executionArchiveService.saveArchive(currentBatch.getBatch(), null, new Date(), null, currentBatch.getState());
        executionQueueSerializer.saveLog(startDate, currentBatch.getBatch().getId(), "Starting batch");

        logstashService.startBatches(INSTANCE, currentBatch,
                (retval, successfullyStarted) -> {
                    if (retval == 0 && successfullyStarted) {
                        currentExecutionArchive.setState("DONE");
                    } else {
                        if (!currentBatch.getState().equals("INTERRUPTED")) {
                            currentExecutionArchive.setState("ERROR");
                            mailService.sendSimpleMessage("emeric.baveux.external@airbus.com", "Logstasher : error on batch " + currentBatch.getBatch().getId(),
                                    "The batch " + currentBatch.getBatch().getId() + " got an error\n\n" + currentBatch.getOutput());
                        }
                    }
                    currentExecutionArchive.setEndTime(new Date());
                    executionArchiveService.save(currentExecutionArchive);
                    logstashService.sendState(INSTANCE);

                    executionQueueSerializer.saveLog(startDate, currentBatch.getBatch().getId(), "End with " + retval);
                    // hook after running
                    runHookRequests(currentBatch.getBatch(), true);
                    processEnded(retval);

                },
                (newLineLog) -> {
                    changeState("RUNNING");
                    executionQueueSerializer.saveLog(startDate, currentBatch.getBatch().getId(), newLineLog);

                });
        changeState("STARTING");

    }


    private void changeState(String state) {
        if (currentExecutionArchive != null) {
            currentExecutionArchive.setState(state);
            currentBatch.setState(state);
            executionArchiveService.save(currentExecutionArchive);
        }

        logstashService.sendState(INSTANCE);
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
        if (this.currentBatch != null && (date.getTime() - this.currentExecutionArchive.getStartTime().getTime()) > currentBatch.getBatch().getTimeout() * 60000) {
            stopLogstash(true);
        }
    }

    public void init() {
        batchRepository.save(new Batch() {{
            setId("test");
            setActivated(false);
            setContent("input {} output {}");
            setOrder(0);
            setTimeout(60);
        }});
    }

    public void clear() {
        executionArchiveService.clear();
        executionArchiveService.save(new ExecutionArchive(){{
            setState("INIT");
            setStartTime(new Date());
        }});
    }
}
