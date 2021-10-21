package fr.emeric0101.logstasher.service;

import fr.emeric0101.logstasher.entity.Batch;
import fr.emeric0101.logstasher.entity.ExecutorEnum;
import fr.emeric0101.logstasher.entity.RecurrenceEnum;
import fr.emeric0101.logstasher.repository.BatchRepository;
import fr.emeric0101.logstasher.service.Scheduler.DailyScheduler;
import fr.emeric0101.logstasher.service.Scheduler.MonthlyScheduler;
import fr.emeric0101.logstasher.service.Scheduler.SchedulerInterface;
import fr.emeric0101.logstasher.service.Scheduler.WeeklyScheduler;
import fr.emeric0101.logstasher.service.batch.BatchExecutionService;
import org.elasticsearch.ElasticsearchStatusException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class BatchService {
    @Autowired
    BatchRepository repository;

    @Autowired
    GeneratorService generatorService;

    @Autowired
    BatchExecutionService batchExecutionService;

    @Autowired
    ExecutionArchiveService executionArchiveService;


    public List<Batch> findAll() {
        try {
            PageRequest page = PageRequest.of(0, 1000, Sort.Direction.ASC, "order");
            return StreamSupport.stream(repository.findAll(page).spliterator(), false).collect(Collectors.toList());
        } catch (ElasticsearchStatusException e) {
            return List.of();
        }

    }

    public void save(Batch p) {
        this.repository.save(p);

    }

    public void delete(String id) {
        this.repository.deleteById(id);

    }

    public void start(String id) {
        Optional<Batch> batch = repository.findById(id);
        if (!batch.isPresent()) {
            throw new RuntimeException("Batch not found");
        }
        // Which type of batch (talend, logstash ?)
        batchExecutionService.startFromQueue(batch.get().getExecutor(), Arrays.asList(batch.get()), false);
    }

    /**
     * Create and send queue to logstash by scheduler strategy
     * @param time
     */
    public void startScheduledBatched(Calendar time) {


        List<Batch> allBatches = findAllActive();

        List<Batch> batchesToBeExecuted = new LinkedList<>();

        for (Batch batch: allBatches) {
            // strategy ?
            if (batch.getRecurrence() == null) {
                // Fix legacy
                batch.setRecurrence(RecurrenceEnum.Daily);
                repository.save(batch);
            }
            SchedulerInterface stragety;
            switch (batch.getRecurrence()) {
                case Daily:
                    stragety = new DailyScheduler(batch, executionArchiveService);
                    break;
                case Weekly:
                    stragety = new WeeklyScheduler(batch, executionArchiveService);
                    break;
                case Monthly:
                    stragety = new MonthlyScheduler(batch, executionArchiveService);
                    break;
                default:
                    throw new RuntimeException("Unknown recursive param");
            }
            boolean toBeExecuted = stragety.isToBeExecuted(time);
            if (toBeExecuted) {
                batchesToBeExecuted.add(batch);
            }
        }
        // group by executor
        Map<ExecutorEnum, List<Batch>> batchesGroupedByExecutor = batchesToBeExecuted.stream().collect(Collectors.groupingBy(b -> b.getExecutor()));
        // start all executors
        batchesGroupedByExecutor.entrySet().forEach(key -> {
            batchExecutionService.startFromQueue(key.getKey(), key.getValue(), true);
        });
    }

    private List<Batch> findAllActive() {
        return repository.findAllActive();
    }
}
