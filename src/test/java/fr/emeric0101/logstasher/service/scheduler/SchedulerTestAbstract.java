package fr.emeric0101.logstasher.service.scheduler;

import fr.emeric0101.logstasher.entity.ExecutionArchive;
import fr.emeric0101.logstasher.repository.ExecutionArchiveRepository;
import fr.emeric0101.logstasher.service.ExecutionArchiveService;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

public abstract class SchedulerTestAbstract {
    @Mock
    protected ExecutionArchiveRepository archiveRepository;

    @InjectMocks
    protected ExecutionArchiveService executionArchiveService;


    @BeforeEach
    public void init() {
        MockitoAnnotations.initMocks(this);
        when(archiveRepository.findInterval(any(), any(), any())).then(args -> findArchive(args.getArgument(0), args.getArgument(1)));

    }

    List<ExecutionArchive> archives = new LinkedList<>();

    List<ExecutionArchive> findArchive(Date start, Date end) {
        return archives.stream().filter(e -> e.getStartTime().getTime().getTime() >= start.getTime() && e.getStartTime().getTime().getTime() < end.getTime()).collect(Collectors.toList());
    }
}
