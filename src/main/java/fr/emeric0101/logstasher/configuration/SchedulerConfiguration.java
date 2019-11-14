package fr.emeric0101.logstasher.configuration;

import java.text.SimpleDateFormat;
import java.util.Date;

import fr.emeric0101.logstasher.service.BatchService;
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

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    @Scheduled(cron = "0 30 23 * * ?")
    public void reportCurrentTime() {
        batchService.restartBatches();
    }


}
