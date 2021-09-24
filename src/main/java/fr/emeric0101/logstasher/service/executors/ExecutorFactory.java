package fr.emeric0101.logstasher.service.executors;

import fr.emeric0101.logstasher.configuration.LogstashProperties;
import fr.emeric0101.logstasher.entity.ExecutorEnum;
import fr.emeric0101.logstasher.service.GeneratorService;
import fr.emeric0101.logstasher.service.executors.logstash.LogstashInstance;
import fr.emeric0101.logstasher.service.executors.talend.TalendInstance;
import fr.emeric0101.logstasher.service.executors.test.TestInstance;
import org.springframework.stereotype.Service;

@Service
public class ExecutorFactory {
    LogstashProperties logstashProperties;
    GeneratorService generatorService;


    public ExecutorFactory(LogstashProperties logstashProperties, GeneratorService generatorService) {
        this.logstashProperties = logstashProperties;
        this.generatorService = generatorService;
    }

    public ExecutorInterface createInstance(ExecutorEnum executorEnum) {
        switch (executorEnum) {
            case LOGSTASH_BATCH:
                return new LogstashInstance(logstashProperties.getPath(), "batch", generatorService);
            case LOGSTASH_PIPELINE:
                return new LogstashInstance(logstashProperties.getPath(), "pipeline", generatorService);
            case TALEND:
                return new TalendInstance();
            case TEST:
                return new TestInstance();
            default:
                throw new RuntimeException("Unknown executor");
        }
    }
}
