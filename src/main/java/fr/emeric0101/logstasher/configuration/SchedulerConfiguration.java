package fr.emeric0101.logstasher.configuration;

import java.time.LocalDateTime;

import fr.emeric0101.logstasher.service.BatchService;
import fr.emeric0101.logstasher.service.BatchExecutionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
@Component
public class SchedulerConfiguration {
    private static final Logger log = LoggerFactory.getLogger(SchedulerConfiguration.class);

    @Autowired
    BatchService batchService;

    @Autowired
    BatchExecutionService batchExecutionService;

    @Scheduled(fixedRate = 30000)
    public void pollingEvent() {
        batchService.startScheduledBatched(LocalDateTime.now().getHour(), LocalDateTime.now().getMinute());
        batchExecutionService.dogWatch();
    }


}
