package fr.emeric0101.logstasher.service.executors.test;

import fr.emeric0101.logstasher.dto.ExecutionBatch;
import fr.emeric0101.logstasher.entity.Pipeline;
import fr.emeric0101.logstasher.service.executors.ExecutorAbstract;
import fr.emeric0101.logstasher.service.executors.ExecutorStateEnum;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Slf4j
public class TestInstance extends ExecutorAbstract {
    ExecutorStateEnum state = ExecutorStateEnum.STOPPED;
    BiConsumer<Integer, Boolean> endCallback;
    @Override
    public void clearData() {

    }

    @Override
    public void start(ExecutionBatch batch, List<Pipeline> pipelines, BiConsumer<Integer, Boolean> endCallback, Consumer<String> logAddLines, Runnable startedCallback) {
        initialize(batch, logAddLines);
        log.info("Start Test Instance : " + batch.getBatch().getId());
        this.endCallback = endCallback;
        state = ExecutorStateEnum.RUNNING;
    }

    @Override
    public void stop() {
        log.info("Stop Test Instance");
        state = ExecutorStateEnum.STOPPED;

        endCallback.accept(0, true);

    }

    @Override
    public ExecutorStateEnum getState() {
        return state;
    }

    @Override
    public boolean isAlive() {
        return state == ExecutorStateEnum.RUNNING;
    }
}
