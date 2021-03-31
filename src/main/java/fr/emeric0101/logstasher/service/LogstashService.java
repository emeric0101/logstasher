package fr.emeric0101.logstasher.service;

import fr.emeric0101.logstasher.configuration.LogstashProperties;
import fr.emeric0101.logstasher.dto.ExecutionBatch;
import fr.emeric0101.logstasher.dto.LogstashRunning;
import fr.emeric0101.logstasher.entity.Pipeline;
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
public class LogstashService {

    @Autowired
    LogstashProperties logstashProperties;

    Map<String, LogstashInstance> instances = new ConcurrentHashMap<>();

    @Autowired
    SimpMessagingTemplate template;

    /**
     * Create or get the logstash instance
     * @param instance
     * @return
     */
    public LogstashInstance getInstance(String instance) {
        if (!instances.containsKey(instance)) {
            LogstashInstance logstashInstance = new LogstashInstance(logstashProperties, instance);
            instances.put(instance, logstashInstance);
        }
        return instances.get(instance);
    }

    public void stop(String instance) {
        if (instances.containsKey(instance)) {
            this.instances.get(instance).stopLogstash();
        }
    }

    public void clearBuffer(String instance) {
        if (instances.containsKey(instance)) {
            this.instances.get(instance).clearBuffer();
        }
    }

    public void startBatches(String instance, ExecutionBatch currentBatch, BiConsumer<Integer, Boolean> endCallback, Consumer<String> logAddLines, Runnable startedCallback) {
        this.getInstance(instance).start(currentBatch, null, endCallback, logAddLines, startedCallback);
    }

    public void startPipeline(String instance, List<Pipeline> pipelines, BiConsumer<Integer, Boolean> endCallback, Consumer<String> logAddLines, Runnable startedCallback) {
        this.getInstance(instance).start(null, pipelines,  endCallback, logAddLines, startedCallback);
    }

    public LogstashRunning getRunning(String instance) {
        if (instances.containsKey(instance)) {
            LogstashRunning running = new LogstashRunning();
            LogstashInstance logstashInstance = this.instances.get(instance);
            running.setState(logstashInstance.getState());
            running.setBuffer(logstashInstance.getBuffer());
            if (logstashInstance.getCurrentBatch() != null) {
                running.setBatch(logstashInstance.getCurrentBatch().getBatch());
            }
            running.setPipelines(logstashInstance.getCurrentPipelines());
            running.setInstance(instance);
            running.setStarted(logstashInstance.getStartDate());
            return running;
        }
        return null;
    }

    /**
     * Send the current state the user
     */
    public void sendState(String instance) {
        LogstashRunning logstashRunning = getRunning(instance);
        if (logstashRunning == null) {
            return;
        }
        template.convertAndSend("/state", logstashRunning);
    }


    public List<LogstashRunning> getRunningAll() {
        return instances.keySet().parallelStream().map(e -> getRunning(e)).collect(Collectors.toList());
    }

    public boolean isAlive(String instance) {
        return getInstance(instance).isAlive();
    }
}
