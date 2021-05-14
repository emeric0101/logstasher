package fr.emeric0101.logstasher.service;

import fr.emeric0101.logstasher.configuration.LogstashProperties;
import fr.emeric0101.logstasher.dto.ExecutionBatch;
import fr.emeric0101.logstasher.dto.LogstashRunning;
import fr.emeric0101.logstasher.entity.ExecutorEnum;
import fr.emeric0101.logstasher.entity.Pipeline;
import fr.emeric0101.logstasher.service.executors.ExecutorFactory;
import fr.emeric0101.logstasher.service.executors.ExecutorInterface;
import fr.emeric0101.logstasher.service.executors.logstash.LogstashInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Manage logstash instances
 */

@Service
public class ExecutionService {

    @Autowired
    LogstashProperties logstashProperties;

    Map<ExecutorEnum, ExecutorInterface> instances = new ConcurrentHashMap<>();

    @Autowired
    SimpMessagingTemplate template;

    @Autowired
    ExecutorFactory executorFactory;

    /**
     * Create or get the logstash instance
     * @param executor
     * @return
     */
    public ExecutorInterface getInstance(ExecutorEnum executor) {
        if (!instances.containsKey(executor)) {
            ExecutorInterface ExecutorInterface = executorFactory.createInstance(executor);
            instances.put(executor, ExecutorInterface);
        }
        return instances.get(executor);
    }

    public void stop(ExecutorEnum executor) {
        if (instances.containsKey(executor)) {
            this.instances.get(executor).stop();
        }
    }

    public void clearBuffer(ExecutorEnum executor) {
        if (instances.containsKey(executor)) {
            this.instances.get(executor).clearBuffer();
        }
    }

    /**
     * Start a batch instance
     * @param currentBatch
     * @param endCallback
     * @param logAddLines
     * @param startedCallback
     */
    public void startBatches(ExecutionBatch currentBatch, BiConsumer<Integer, Boolean> endCallback, Consumer<String> logAddLines, Runnable startedCallback) {
        this.getInstance(currentBatch.getBatch().getExecutor()).start(currentBatch, null, endCallback, logAddLines, startedCallback);
    }

    /**
     * Pipeline only supported by logstash yet
     * @param pipelines
     * @param endCallback
     * @param logAddLines
     * @param startedCallback
     */
    public void startPipeline(List<Pipeline> pipelines, BiConsumer<Integer, Boolean> endCallback, Consumer<String> logAddLines, Runnable startedCallback) {
        this.getInstance(ExecutorEnum.LOGSTASH_PIPELINE).start(null, pipelines,  endCallback, logAddLines, startedCallback);
    }

    public LogstashRunning getRunning(ExecutorEnum instance) {
        ExecutorInterface executorInterface = getInstance(instance);

        if (executorInterface != null) {
            LogstashRunning running = new LogstashRunning();
            running.setState(executorInterface.getState().name());
            running.setBuffer(executorInterface.getBuffer().stream().collect(Collectors.toList()));
            if (executorInterface.getCurrentBatch() != null) {
                running.setBatch(executorInterface.getCurrentBatch().getBatch());
            }
            // Get pipeline if LogstashInstance
            if (executorInterface instanceof LogstashInstance) {
                running.setPipelines(((LogstashInstance)executorInterface).getCurrentPipelines());

            }
            running.setInstance(instance.name());
            running.setStarted(executorInterface.getStartDate());
            return running;
        }
        return null;
    }


    /**
     * Send the current state the user
     */
    public void sendState(ExecutorEnum executor) {
        LogstashRunning logstashRunning = getRunning(executor);
        if (logstashRunning == null) {
            return;
        }
        template.convertAndSend("/state", logstashRunning);
    }

    public List<LogstashRunning> getRunningAll() {
        return instances.keySet().parallelStream().map(e -> getRunning(e)).collect(Collectors.toList());
    }

    public boolean isAlive(ExecutorEnum executor) {
        return getInstance(executor).isAlive();
    }
}
