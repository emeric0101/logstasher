package fr.emeric0101.logstasher.service.scheduler;

import fr.emeric0101.logstasher.entity.Batch;
import fr.emeric0101.logstasher.entity.ExecutionArchive;
import fr.emeric0101.logstasher.repository.ExecutionArchiveRepository;
import fr.emeric0101.logstasher.service.ExecutionArchiveService;
import fr.emeric0101.logstasher.service.Scheduler.DailyScheduler;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

public class DailySchedulerTest extends SchedulerTestAbstract {



    @Test
    public void test() {


        // current time = start time , no archive
        Calendar current = Calendar.getInstance();
        Batch batch = new Batch();
        batch.setStartHour(current.get(Calendar.HOUR_OF_DAY));
        batch.setStartMinute(current.get(Calendar.MINUTE) -DailyScheduler.INTERVAL_MINUTES);
        DailyScheduler dailyScheduler = new DailyScheduler(batch, executionArchiveService);
        assertTrue(dailyScheduler.isToBeExecuted(current));
        // add archives
        ExecutionArchive archive = new ExecutionArchive();
        archive.setBatch(batch);
        Calendar archiveTime = (Calendar)current.clone(); // localdatetime is immutable
        archiveTime.add(Calendar.MINUTE, dailyScheduler.INTERVAL_MINUTES/2);
        archive.setStartTime(current);
        archives.add(archive);
        assertFalse(dailyScheduler.isToBeExecuted(current));

        // too soon
        archives.clear();
        current.add(Calendar.MINUTE, -100);
        assertFalse(dailyScheduler.isToBeExecuted(current));

        // too late
        current.add(Calendar.MINUTE, +300);
        assertFalse(dailyScheduler.isToBeExecuted(current));

    }




}
