package fr.emeric0101.logstasher.service.Scheduler;

import fr.emeric0101.logstasher.entity.Batch;

import java.time.LocalDateTime;
import java.util.Calendar;

public interface SchedulerInterface {
    boolean isToBeExecuted(Calendar time);
}
