package fr.emeric0101.logstasher.service;

import fr.emeric0101.logstasher.dto.ExecutionQueue;
import fr.emeric0101.logstasher.entity.BatchArchive;
import fr.emeric0101.logstasher.entity.Batch;
import fr.emeric0101.logstasher.repository.BatchRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
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
    ArchiveService archiveService;

    Date scheduledWorkingSince = null;


    public List<Batch> findAll() {
        PageRequest page = PageRequest.of(0, 1000, Sort.Direction.ASC, "order");
        return StreamSupport.stream(repository.findAll(page).spliterator(), false).collect(Collectors.toList());
    }

    public void save(Batch p) {
        this.repository.save(p);
        this.generatorService.generateBatches();

    }

    public void delete(String id) {
        this.repository.deleteById(id);
        this.generatorService.generateBatches();

    }

    public void start(String id) {
        Optional<Batch> batch = repository.findById(id);
        if (!batch.isPresent()) {
            throw new RuntimeException("Batch not found");
        }
        ExecutionQueue queue = new ExecutionQueue(new ArrayList<Batch>(){{add(batch.get());}}, r -> {});
        batchExecutionService.startFromQueue(queue);
    }

    public void restartBatches() {
        PageRequest page = PageRequest.of(0, 1000, Sort.Direction.ASC, "order");
        List<Batch> batches = StreamSupport.stream(repository.findAllActive(page).spliterator(), false).collect(Collectors.toList());

        ExecutionQueue queue = new ExecutionQueue(batches, r -> {});
        batchExecutionService.startFromQueue(queue);
    }


    public void startScheduledBatched(int hour, int minute) {
        // skip if already working

        if (scheduledWorkingSince != null && (new Date()).getTime() - scheduledWorkingSince.getTime() > 3600*1000) {
            System.out.println("FATAL ERROR : Timeout on scheduler, this must never happen !!!! Probably unable to shutdown logstash automatically");
            scheduledWorkingSince = null;
        }

        if (scheduledWorkingSince != null) {return;}
        PageRequest page = PageRequest.of(0, 1000, Sort.Direction.ASC, "order");
        List<Batch> batches = StreamSupport.stream(repository.findAllActive(page).spliterator(), false).collect(Collectors.toList());
        List<BatchArchive> batchArchives = archiveService.findToday().stream().filter(e -> e.getBatch() != null).collect(Collectors.toList());

        // get only batch in the current period
        if (batches == null) {
            return;
        }
        batches = batches.stream().filter(e -> Math.abs((hour*60+minute) - (e.getStartHour()*60+e.getStartMinute())) < 5).collect(Collectors.toList());

        // batches already runs today ?
        if (batchArchives != null) {
            batches = batches.stream().filter(e -> batchArchives.stream().noneMatch(a -> a.getBatch().getId().equals(e.getId()))).collect(Collectors.toList());
        }

        if (batches.isEmpty()) {
            return;
        }

        scheduledWorkingSince = new Date();


        // ajouter une file d'attente d'exécution

        ExecutionQueue executionQueue = new ExecutionQueue(batches, (r) -> scheduledWorkingSince = null);
        batchExecutionService.startFromQueue(executionQueue);
    }
}
