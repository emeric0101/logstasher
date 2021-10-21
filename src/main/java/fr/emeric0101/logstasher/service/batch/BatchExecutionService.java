package fr.emeric0101.logstasher.service.batch;


import fr.emeric0101.logstasher.entity.Batch;

import fr.emeric0101.logstasher.entity.ExecutorEnum;

import fr.emeric0101.logstasher.repository.BatchRepository;
import fr.emeric0101.logstasher.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class BatchExecutionService {

    BatchRepository batchRepository;
    ExecutionQueueSerializer executionQueueSerializer;
    ExecutionArchiveService executionArchiveService;
    RestService restService;
    MailService mailService;
    ExecutionService executionService;


    Map<ExecutorEnum, BatchExecutionQueue> batchExecutionQueues = new HashMap<>();

    public BatchExecutionService(BatchRepository batchRepository, ExecutionQueueSerializer executionQueueSerializer, ExecutionArchiveService executionArchiveService, RestService restService, MailService mailService, ExecutionService executionService) {
        this.batchRepository = batchRepository;
        this.executionQueueSerializer = executionQueueSerializer;
        this.executionArchiveService = executionArchiveService;
        this.restService = restService;
        this.mailService = mailService;
        this.executionService = executionService;
    }

    /**
     * Run batch lists
     * @param executor
     * @param batches
     * @param automaticallyStarted
     */
    public void startFromQueue(ExecutorEnum executor, List<Batch> batches, boolean automaticallyStarted) {
        // is the queue already exists
        if (!batchExecutionQueues.containsKey(executor)) {
            // create the queue
            BatchExecutionQueue batchExecutionQueue = new BatchExecutionQueue(
                    executor, executionQueueSerializer, executionArchiveService, restService,  mailService, executionService
            );
            batchExecutionQueues.put(executor, batchExecutionQueue);
        }
        BatchExecutionQueue currentQueue = batchExecutionQueues.get(executor);
        // check that all batch are with the good executor
        if (batches.stream().anyMatch(e -> !e.getExecutor().equals(executor))) {
            throw new RuntimeException("Several executors found in the same list");
        }
        // start the queue
        currentQueue.startFromQueue(batches, automaticallyStarted);
    }

    /**
     * Perform timeout check
     */
    public void dogWatch() {
        batchExecutionQueues.values().forEach(BatchExecutionQueue::dogWatch);
    }

    /**
     * Init batch repository with test data
     */
    public void init() {
        var batch = new Batch();
        batch.setId("test");
        batch.setActivated(false);
        batch.setContent("input {} output {}");
        batch.setOrder(0);
        batch.setTimeout(60);
        batchRepository.save(batch);
    }

    /**
     * Stop all queues
     * Fix me : add ability to stop only talend or logstash queue
     * @param continueQueue
     */
    public void stop(ExecutorEnum executorEnum, boolean continueQueue) {
        if (batchExecutionQueues.containsKey(executorEnum)) {
            batchExecutionQueues.get(executorEnum).stop(continueQueue);
        }
    }
}
