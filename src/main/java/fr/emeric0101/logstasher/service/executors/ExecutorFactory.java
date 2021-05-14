package fr.emeric0101.logstasher.service.executors;

import fr.emeric0101.logstasher.configuration.LogstashProperties;
import fr.emeric0101.logstasher.entity.ExecutorEnum;
import fr.emeric0101.logstasher.service.executors.logstash.LogstashInstance;
import fr.emeric0101.logstasher.service.executors.talend.TalendInstance;
import org.springframework.stereotype.Service;

@Service
public class ExecutorFactory {
    LogstashProperties logstashProperties;

    public ExecutorFactory(LogstashProperties logstashProperties) {
        this.logstashProperties = logstashProperties;
    }

    public ExecutorInterface createInstance(ExecutorEnum executorEnum) {
        switch (executorEnum) {
            case LOGSTASH_BATCH:
                return new LogstashInstance(logstashProperties.getPath(), "batch");
            case LOGSTASH_PIPELINE:
                return new LogstashInstance(logstashProperties.getPath(), "pipeline");
            case TALEND:
                    return new TalendInstance();
            default:
                throw new RuntimeException("Unknown executor");
        }
    }
}
