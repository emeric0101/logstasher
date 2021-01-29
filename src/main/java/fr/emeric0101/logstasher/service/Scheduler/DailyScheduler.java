package fr.emeric0101.logstasher.service.Scheduler;

import fr.emeric0101.logstasher.entity.Batch;
import fr.emeric0101.logstasher.entity.ExecutionArchive;
import fr.emeric0101.logstasher.service.ExecutionArchiveService;

import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.List;

public class DailyScheduler implements SchedulerInterface{

    public static final int INTERVAL_MINUTES = 10;

    protected Batch batch;
    protected ExecutionArchiveService executionArchiveService;

    public DailyScheduler(Batch batch, ExecutionArchiveService executionArchiveService) {
        this.batch = batch;
        this.executionArchiveService = executionArchiveService;
    }


    @Override
    public boolean isToBeExecuted(Calendar time) {
        // check the time
        int currentMinutes = time.get(Calendar.MINUTE) + time.get(Calendar.HOUR_OF_DAY)*60;
        int batchMinutes = batch.getStartHour()*60 + batch.getStartMinute();

        if (currentMinutes >= batchMinutes && currentMinutes <= batchMinutes + INTERVAL_MINUTES) {
            // now check if the batch has already runned in the last interval * 2
            List<ExecutionArchive> archives = executionArchiveService.findInInterval(time, INTERVAL_MINUTES, batch.getId());
            // find in archive
            if (archives.isEmpty()) {
                return true;
            }
        }
        return false;
    }
}
