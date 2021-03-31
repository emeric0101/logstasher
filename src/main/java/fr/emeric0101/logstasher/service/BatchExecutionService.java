package fr.emeric0101.logstasher.service;

import fr.emeric0101.logstasher.dto.ExecutionBatch;
import fr.emeric0101.logstasher.dto.RestRequest;
import fr.emeric0101.logstasher.entity.Batch;
import fr.emeric0101.logstasher.entity.ExecutionArchive;
import fr.emeric0101.logstasher.entity.ExecutionArchiveTypeEnum;
import fr.emeric0101.logstasher.repository.BatchRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
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

    private ExecutionBatch currentBatch;

    ConcurrentLinkedDeque<ExecutionBatch> queueManager = new ConcurrentLinkedDeque();




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
            currentBatch.getArchive().setEndTime(Calendar.getInstance());
            currentBatch.getArchive().setState(currentBatch.getState());
            executionArchiveService.save(currentBatch.getArchive());
            logstashService.sendState(INSTANCE);

            nextBatchFromQueue();


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
     * logstash instance running
     * @return
     */
    private boolean isRunning() {
        return logstashService.isAlive(INSTANCE);
    }




    /**
     *
     * @param queue
     */
    public void startFromQueue(List<Batch> queue, boolean automaticallyStarted) {
        startDate = executionQueueSerializer.getDate();
        logstashService.clearBuffer(INSTANCE);
        // Add to the current queue or replace the empty queue
        queueManager.addAll(queue.stream().map(e -> new ExecutionBatch(e, automaticallyStarted,
                executionArchiveService.saveArchive(e, null, Calendar.getInstance(), null,  "PLANIFIED", automaticallyStarted ? ExecutionArchiveTypeEnum.AUTO : ExecutionArchiveTypeEnum.MANUAL))).collect(Collectors.toList()));
        // start batch if not running
        if (!isRunning()) {
            nextBatchFromQueue();
        }
    }

    /**
     * Execute the next batch of the current queue only if not running
     */
    private void nextBatchFromQueue() {
        if (isRunning()) {
            return;
        }
        if (!queueManager.isEmpty()) {
            currentBatch = queueManager.poll();
            startBatch(currentBatch);
        } else {
            currentBatch = null;
        }
    }

    public void stopLogstash(boolean continueQueue) {

        logstashService.stop(INSTANCE);

        if (currentBatch.getArchive() != null && currentBatch.getArchive().getEndTime() == null) {
            currentBatch.getArchive().setEndTime(Calendar.getInstance());
            currentBatch.getArchive().setState("INTERRUPTED");
            executionArchiveService.save(currentBatch.getArchive());
            logstashService.sendState(INSTANCE);
        }

        if (continueQueue) {
            nextBatchFromQueue();
            queueManager.clear();
        }


    }

    private void startBatch(ExecutionBatch currentBatch) {
        if (isRunning()) {
            throw new RuntimeException("Batch already run");
        }
        // Hook before running
        runHookRequests(currentBatch.getBatch(), false);

        executionQueueSerializer.saveLog(startDate, currentBatch.getBatch().getId(), "Starting batch");

        logstashService.sendState(INSTANCE);


        logstashService.startBatches(INSTANCE, currentBatch,
                (retval, successfullyStarted) -> {
                    if (retval == 0 && successfullyStarted) {
                        currentBatch.getArchive().setState("DONE");
                    } else {
                        if (!currentBatch.getState().equals("INTERRUPTED")) {
                            currentBatch.getArchive().setState("ERROR");
                            mailService.sendSimpleMessage("emeric.baveux.external@airbus.com", "Logstasher : error on batch " + currentBatch.getBatch().getId(),
                                    "The batch " + currentBatch.getBatch().getId() + " got an error\n\n" + currentBatch.getOutput());
                        }
                    }
                    currentBatch.getArchive().setEndTime(Calendar.getInstance());
                    executionArchiveService.save(currentBatch.getArchive());
                    logstashService.sendState(INSTANCE);

                    executionQueueSerializer.saveLog(startDate, currentBatch.getBatch().getId(), "End with " + retval);
                    // hook after running
                    runHookRequests(currentBatch.getBatch(), true);
                    processEnded(retval);

                },
                (newLineLog) -> {
                    changeState("RUNNING");
                    executionQueueSerializer.saveLog(startDate, currentBatch.getBatch().getId(), newLineLog);

                }, () -> {
                    logstashService.sendState(INSTANCE);
                });

    }


    private void changeState(String state) {
        if (currentBatch != null && currentBatch.getArchive() != null) {
            currentBatch.getArchive().setState(state);
            currentBatch.setState(state);
            executionArchiveService.save(currentBatch.getArchive());
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
        if (this.currentBatch != null && this.currentBatch.getArchive() != null &&
                (Calendar.getInstance().getTimeInMillis() - this.currentBatch.getArchive().getStartTime().getTimeInMillis())
                        > currentBatch.getBatch().getTimeout() * 60000) {
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
            setStartTime(Calendar.getInstance());
        }});
    }
}
