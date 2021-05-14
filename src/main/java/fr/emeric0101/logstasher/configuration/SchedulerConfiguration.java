package fr.emeric0101.logstasher.configuration;

import java.util.Calendar;

import fr.emeric0101.logstasher.service.BatchService;
import fr.emeric0101.logstasher.service.batch.BatchExecutionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
@Component
public class SchedulerConfiguration {
    @Autowired
    BatchService batchService;

    @Autowired
    BatchExecutionService batchExecutionService;

    @Scheduled(fixedRate = 30000)
    public void pollingEvent() {
        batchService.startScheduledBatched(Calendar.getInstance());
        batchExecutionService.dogWatch();
    }


}
