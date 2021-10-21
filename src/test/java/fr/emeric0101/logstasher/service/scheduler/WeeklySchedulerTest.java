package fr.emeric0101.logstasher.service.scheduler;

import fr.emeric0101.logstasher.entity.Batch;
import fr.emeric0101.logstasher.entity.ExecutionArchive;
import fr.emeric0101.logstasher.service.Scheduler.SchedulerInterface;
import fr.emeric0101.logstasher.service.Scheduler.WeeklyScheduler;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Calendar;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class WeeklySchedulerTest extends SchedulerTestAbstract{


    @Test
    public void test() {


        // current time = start time , no archive
        Calendar current = Calendar.getInstance();
        current.setWeekDate(2021, 1, Calendar.MONDAY);
        current.set(Calendar.HOUR_OF_DAY, 9);
        current.set(Calendar.MINUTE, 00);

        Batch batch = new Batch();
        batch.setStartHour(9);
        batch.setStartMinute(00);

        // Monday, Tuesday
        batch.setWeekDays(Arrays.asList(Calendar.MONDAY, Calendar.TUESDAY));
        SchedulerInterface dailyScheduler = new WeeklyScheduler(batch, executionArchiveService);
        assertTrue(dailyScheduler.isToBeExecuted(current));

        // add archives
        ExecutionArchive archive = new ExecutionArchive();
        archive.setBatch(batch);
        Calendar archiveTime = (Calendar)current.clone(); // localdatetime is immutable
        archiveTime.add(Calendar.MINUTE, WeeklyScheduler.INTERVAL_MINUTES/2);
        archive.setStartTime(current);
        archives.add(archive);
        assertFalse(dailyScheduler.isToBeExecuted(current));
        // now test the tuesday
        current = (Calendar)current.clone();
        current.setWeekDate(2021, 1, Calendar.TUESDAY);
        assertTrue(dailyScheduler.isToBeExecuted(current));

        // now test the Wednesday
        current = (Calendar)current.clone();
        current.setWeekDate(2021, 1, Calendar.WEDNESDAY);
        assertFalse(dailyScheduler.isToBeExecuted(current));

        // now test the Sunday
        current = (Calendar)current.clone();
        current.setWeekDate(2021, 1, Calendar.SUNDAY);
        assertFalse(dailyScheduler.isToBeExecuted(current));

        // now test monday hour too early
        // now test the Wednesday
        current = (Calendar)current.clone();
        current.setWeekDate(2021, 1, Calendar.MONDAY);
        current.set(Calendar.HOUR_OF_DAY, 2);
        assertFalse(dailyScheduler.isToBeExecuted(current));


    }
}
