package fr.emeric0101.logstasher.service;

import fr.emeric0101.logstasher.entity.ExecutionArchive;
import fr.emeric0101.logstasher.entity.ExecutionArchiveTypeEnum;
import fr.emeric0101.logstasher.entity.ExecutorEnum;
import fr.emeric0101.logstasher.entity.Pipeline;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;


@Service
public class PipelineExecutionService {
    @Autowired
    ExecutionService executionService;

    @Autowired
    PipelineService pipelineService;

    @Autowired
    ExecutionQueueSerializer executionQueueSerializer;

    @Autowired
    ExecutionArchiveService executionArchiveService;

    @Autowired
    MailService mailService;



    final ExecutorEnum executor = ExecutorEnum.LOGSTASH_PIPELINE;

    private ExecutionArchive currentExecutionArchive;


    public void restart() {
        executionService.stop(executor);



        // get All pipeline
        Iterable<Pipeline> pipelinesIter = pipelineService.findAll();
        List<Pipeline> pipelines = StreamSupport.stream(pipelinesIter.spliterator(), true).filter(e -> e.isActivated()).collect(Collectors.toList());

        // create archive entry
        currentExecutionArchive = executionArchiveService.saveArchive(null, pipelines, Calendar.getInstance(), null, "STARTING", ExecutionArchiveTypeEnum.MANUAL);


        // start logstash instance for pipeline
        executionService.startPipeline(pipelines, (retval, successfullyStarted) -> {
            executionService.sendState(executor);
            if (retval == 0 && successfullyStarted) {
                currentExecutionArchive.setState("DONE");
            } else {
                currentExecutionArchive.setState("ERROR");
                mailService.sendSimpleMessage("emeric.baveux.external@airbus.com", "Logstasher : error on pipeline",
                        "The pipeline got an error\n\n");

            }
            currentExecutionArchive.setEndTime(Calendar.getInstance());
            String logPath = executionQueueSerializer.saveLog(executionService.getInstance(ExecutorEnum.LOGSTASH_PIPELINE).getStartDate(), "Pipelines", "End with " + retval);
            currentExecutionArchive.setLogPath(logPath);
            executionArchiveService.save(currentExecutionArchive);

            executionService.sendState(executor);

        }, (newLineLog) -> {
            // logCallback
            executionService.sendState(executor);
            executionQueueSerializer.saveLog(executionService.getInstance(ExecutorEnum.LOGSTASH_PIPELINE).getStartDate(), "Pipelines", newLineLog);
        }, () -> {
            executionService.sendState(executor);
        });
        executionService.sendState(executor);

    }



    public void stop() {
        executionService.stop(executor);
        executionService.clearBuffer(executor);
    }
}
