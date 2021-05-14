package fr.emeric0101.logstasher.service.executors;

import fr.emeric0101.logstasher.dto.ExecutionBatch;
import fr.emeric0101.logstasher.entity.Pipeline;
import fr.emeric0101.logstasher.exception.LogstashNotFoundException;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public interface ExecutorInterface {
    void clearData();

    void start(ExecutionBatch batch, List<Pipeline> pipelines, BiConsumer<Integer, Boolean> endCallback, Consumer<String> logAddLines, Runnable startedCallback);

    void stop();

    ConcurrentLinkedQueue<String> getBuffer();

    void clearBuffer();

    ExecutorStateEnum getState();

    String getStartDate();

    ExecutionBatch getCurrentBatch();

    boolean isAlive();
}
