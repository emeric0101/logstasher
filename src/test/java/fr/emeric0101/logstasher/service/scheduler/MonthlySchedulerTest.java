package fr.emeric0101.logstasher.service.scheduler;

import fr.emeric0101.logstasher.entity.Batch;
import fr.emeric0101.logstasher.service.Scheduler.MonthlyScheduler;
import fr.emeric0101.logstasher.service.Scheduler.SchedulerInterface;
import fr.emeric0101.logstasher.service.Scheduler.WeeklyScheduler;
import org.junit.Test;

import java.util.Arrays;
import java.util.Calendar;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MonthlySchedulerTest extends SchedulerTestAbstract {

    @Test
    public void test() {
        // current time = start time , no archive
        Calendar current = Calendar.getInstance();
        current.set(Calendar.DAY_OF_MONTH, 12);
        current.set(Calendar.HOUR_OF_DAY, 9);
        current.set(Calendar.MINUTE, 00);

        Batch batch = new Batch();
        batch.setStartHour(9);
        batch.setStartMinute(00);

        // 12th
        batch.setMonthDate(12);
        SchedulerInterface dailyScheduler = new MonthlyScheduler(batch, executionArchiveService);
        assertTrue(dailyScheduler.isToBeExecuted(current));

        // bad time
        batch.setStartHour(16);
        assertFalse(dailyScheduler.isToBeExecuted(current));

        // bad day
        batch.setMonthDate(12);
        current.set(Calendar.DAY_OF_MONTH, 15);
        assertFalse(dailyScheduler.isToBeExecuted(current));


    }
}
