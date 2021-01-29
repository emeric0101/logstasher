package fr.emeric0101.logstasher.service.Scheduler;

import fr.emeric0101.logstasher.entity.Batch;
import fr.emeric0101.logstasher.service.ExecutionArchiveService;

import java.util.Calendar;

public class WeeklyScheduler extends DailyScheduler {
    public static final int INTERVAL_MINUTES = 10;

    public WeeklyScheduler(Batch batch, ExecutionArchiveService executionArchiveService) {
        super(batch, executionArchiveService);
    }

    @Override
    public boolean isToBeExecuted(Calendar time) {
        // add constraint with the weekday
        if (batch.getWeekDays() == null) {
            return false;
        }
        return batch.getWeekDays().contains(time.get(Calendar.DAY_OF_WEEK))
                && super.isToBeExecuted(time);
    }
}
