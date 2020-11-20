package fr.emeric0101.logstasher.service;

import fr.emeric0101.logstasher.configuration.LogstashProperties;
import fr.emeric0101.logstasher.dto.LogstashRunning;
import fr.emeric0101.logstasher.entity.Pipeline;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;


@Service
public class PipelineExecutionService {
    @Autowired
    LogstashService logstashService;

    @Autowired
    PipelineService pipelineService;

    @Autowired
    ExecutionQueueSerializer executionQueueSerializer;



    final String INSTANCE = "_pipeline";

    String startDate = null;


    public void restart() {
        logstashService.stop(INSTANCE);



        // get All pipeline
        Iterable<Pipeline> pipelinesIter = pipelineService.findAll();
        List<Pipeline> pipelines = StreamSupport.stream(pipelinesIter.spliterator(), true).filter(e -> e.isActivated()).collect(Collectors.toList());

        // start logstash instance for pipeline
        logstashService.startPipeline(INSTANCE, pipelines, (retval) -> {
            logstashService.sendState(INSTANCE);
            executionQueueSerializer.saveLog(startDate, "Pipelines", "End with " + retval);

        }, (newLineLog) -> {
            // logCallback
            logstashService.sendState(INSTANCE);
            executionQueueSerializer.saveLog(startDate, "Pipelines", newLineLog);
        });
        logstashService.sendState(INSTANCE);

    }



    public void stop() {
        logstashService.stop(INSTANCE);
    }
}
