package fr.emeric0101.logstasher.service.scheduler;

import fr.emeric0101.logstasher.LogstasherApplication;
import fr.emeric0101.logstasher.entity.Batch;
import fr.emeric0101.logstasher.entity.ExecutionArchive;
import fr.emeric0101.logstasher.entity.ExecutorEnum;
import fr.emeric0101.logstasher.service.ExecutionArchiveService;
import fr.emeric0101.logstasher.service.ExecutionService;
import fr.emeric0101.logstasher.service.Scheduler.DailyScheduler;
import fr.emeric0101.logstasher.service.batch.BatchExecutionService;
import fr.emeric0101.logstasher.service.executors.ExecutorInterface;
import fr.emeric0101.logstasher.service.executors.test.TestInstance;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Arrays;
import java.util.Calendar;
import java.util.concurrent.ExecutorService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

@ActiveProfiles("test")
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = LogstasherApplication.class)
public class BatchExecutionServiceTest {

    @Autowired
    BatchExecutionService batchExecutionService;

    @Autowired
    ExecutionService executorService;

    @Test
    public void whenTwoBatchesPlannedAtSameTime() throws InterruptedException {
        Calendar current = Calendar.getInstance();
        Batch batch1 = new Batch();
        batch1.setId("Batch 1");
        batch1.setStartHour(current.get(Calendar.HOUR_OF_DAY));
        batch1.setStartMinute(current.get(Calendar.MINUTE) - DailyScheduler.INTERVAL_MINUTES);
        batch1.setExecutor(ExecutorEnum.TEST);

        Batch batch2 = new Batch();
        batch2.setId("Batch 2");
        batch2.setStartHour(current.get(Calendar.HOUR_OF_DAY));
        batch2.setStartMinute(current.get(Calendar.MINUTE) - DailyScheduler.INTERVAL_MINUTES);
        batch2.setExecutor(ExecutorEnum.TEST);

        ExecutorInterface instance = executorService.getInstance(ExecutorEnum.TEST);

        batchExecutionService.startFromQueue(ExecutorEnum.TEST, Arrays.asList(batch1), true);
        batchExecutionService.startFromQueue(ExecutorEnum.TEST, Arrays.asList(batch2), true);
        assertEquals(instance.getCurrentBatch().getBatch().getId(), batch1.getId());
        ExecutionArchive executionArchiveBatch1 = instance.getCurrentBatch().getArchive();
        instance.stop();
        assertEquals(instance.getCurrentBatch().getBatch().getId(), batch2.getId());
        ExecutionArchive executionArchiveBatch2 = instance.getCurrentBatch().getArchive();

        // compare start time of both
        assertNotEquals(executionArchiveBatch1.getStartTime(), executionArchiveBatch2.getStartTime());





    }
}
