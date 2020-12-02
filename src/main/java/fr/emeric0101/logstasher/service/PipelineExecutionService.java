package fr.emeric0101.logstasher.service;

import fr.emeric0101.logstasher.configuration.LogstashProperties;
import fr.emeric0101.logstasher.dto.LogstashRunning;
import fr.emeric0101.logstasher.entity.ExecutionArchive;
import fr.emeric0101.logstasher.entity.Pipeline;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
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

    @Autowired
    ExecutionArchiveService executionArchiveService;

    @Autowired
    MailService mailService;



    final String INSTANCE = "_pipeline";

    String startDate = null;
    private ExecutionArchive currentExecutionArchive;


    public void restart() {
        logstashService.stop(INSTANCE);



        // get All pipeline
        Iterable<Pipeline> pipelinesIter = pipelineService.findAll();
        List<Pipeline> pipelines = StreamSupport.stream(pipelinesIter.spliterator(), true).filter(e -> e.isActivated()).collect(Collectors.toList());

        // create archive entry
        currentExecutionArchive = executionArchiveService.saveArchive(null, pipelines, new Date(), null, "STARTING");


        // start logstash instance for pipeline
        logstashService.startPipeline(INSTANCE, pipelines, (retval, successfullyStarted) -> {
            logstashService.sendState(INSTANCE);
            if (retval == 0 && successfullyStarted) {
                currentExecutionArchive.setState("DONE");
            } else {
                currentExecutionArchive.setState("ERROR");
                mailService.sendSimpleMessage("emeric.baveux.external@airbus.com", "Logstasher : error on pipeline",
                        "The pipeline got an error\n\n");

            }
            currentExecutionArchive.setEndTime(new Date());
            executionArchiveService.save(currentExecutionArchive);
            executionQueueSerializer.saveLog(startDate, "Pipelines", "End with " + retval);
            logstashService.sendState(INSTANCE);

        }, (newLineLog) -> {
            // logCallback
            logstashService.sendState(INSTANCE);
            executionQueueSerializer.saveLog(startDate, "Pipelines", newLineLog);
        });
        logstashService.sendState(INSTANCE);

    }



    public void stop() {
        logstashService.stop(INSTANCE);
        logstashService.clearBuffer(INSTANCE);
    }
}
