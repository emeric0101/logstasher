package fr.emeric0101.logstasher.service.batch;

import fr.emeric0101.logstasher.dto.ExecutionBatch;
import fr.emeric0101.logstasher.dto.RestRequest;
import fr.emeric0101.logstasher.entity.Batch;
import fr.emeric0101.logstasher.entity.ExecutionArchive;
import fr.emeric0101.logstasher.entity.ExecutionArchiveTypeEnum;
import fr.emeric0101.logstasher.entity.ExecutorEnum;
import fr.emeric0101.logstasher.repository.BatchRepository;
import fr.emeric0101.logstasher.service.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

public class BatchExecutionQueue {

    private ExecutionBatch currentBatch;

    ConcurrentLinkedDeque<ExecutionBatch> queueManager = new ConcurrentLinkedDeque();

    ExecutionQueueSerializer executionQueueSerializer;
    ExecutionArchiveService executionArchiveService;
    RestService restService;
    MailService mailService;
    ExecutionService executionService;

    ExecutorEnum executor;

    public BatchExecutionQueue(ExecutorEnum executor,
                               ExecutionQueueSerializer executionQueueSerializer, ExecutionArchiveService executionArchiveService, RestService restService, MailService mailService, ExecutionService executionService) {
        this.executionQueueSerializer = executionQueueSerializer;
        this.executionArchiveService = executionArchiveService;
        this.restService = restService;
        this.mailService = mailService;
        this.executionService = executionService;
        this.executor = executor;
    }

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
            executionService.sendState(executor);

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
     * logstash or talend as batch instance running
     * @return
     */
    private boolean isRunning() {
        return executionService.isAlive(executor);
    }




    /**
     *
     * @param queue
     */
    public void startFromQueue(List<Batch> queue, boolean automaticallyStarted) {
        executionService.clearBuffer(executor);
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

    public void stop(boolean continueQueue) {

        executionService.stop(executor);

        if (currentBatch.getArchive() != null && currentBatch.getArchive().getEndTime() == null) {
            currentBatch.getArchive().setEndTime(Calendar.getInstance());
            currentBatch.getArchive().setState("INTERRUPTED");
            executionArchiveService.save(currentBatch.getArchive());
            executionService.sendState(executor);
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
        // set start event
        currentBatch.getArchive().setStartTime(Calendar.getInstance());
        executionQueueSerializer.saveLog(currentBatch.getBatch().getId(), "Starting batch");

        executionService.sendState(executor);


        executionService.startBatches(currentBatch,
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
                    executionService.sendState(executor);

                    String logPath = executionQueueSerializer.saveLog(currentBatch.getBatch().getId(), "End with " + retval);
                    currentBatch.getArchive().setLogPath(logPath);
                    executionArchiveService.save(currentBatch.getArchive());


                    // hook after running
                    runHookRequests(currentBatch.getBatch(), true);
                    processEnded(retval);

                },
                (newLineLog) -> {
                    changeState("RUNNING");
                    executionQueueSerializer.saveLog(currentBatch.getBatch().getId(), newLineLog);

                }, () -> {
                    executionService.sendState(executor);
                });

    }


    private void changeState(String state) {
        if (currentBatch != null && currentBatch.getArchive() != null) {
            currentBatch.getArchive().setState(state);
            currentBatch.setState(state);
            executionArchiveService.save(currentBatch.getArchive());
        }

        executionService.sendState(executor);
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
                executionQueueSerializer.saveLog(currentBatch.getBatch().getId(), "HOOK REQUEST " + restRequest.getUrl() + "\n\tResult : " + result);
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
            stop(true);
        }
    }

}
