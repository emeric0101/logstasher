package fr.emeric0101.logstasher.service.Scheduler;

import fr.emeric0101.logstasher.entity.Batch;
import fr.emeric0101.logstasher.service.ExecutionArchiveService;

import java.util.Calendar;

public class MonthlyScheduler extends DailyScheduler {

    public MonthlyScheduler(Batch batch, ExecutionArchiveService executionArchiveService) {
        super(batch, executionArchiveService);
    }

    @Override
    public boolean isToBeExecuted(Calendar time) {
        if (batch.getMonthDate() == null) {
            return false;
        }
        return batch.getMonthDate().equals(time.get(Calendar.DAY_OF_MONTH))
                && super.isToBeExecuted(time);
    }
}
