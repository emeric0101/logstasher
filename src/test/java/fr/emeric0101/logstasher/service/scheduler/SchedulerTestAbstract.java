package fr.emeric0101.logstasher.service.scheduler;

import fr.emeric0101.logstasher.entity.ExecutionArchive;
import fr.emeric0101.logstasher.repository.ExecutionArchiveRepository;
import fr.emeric0101.logstasher.service.ExecutionArchiveService;
import org.junit.Before;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

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


    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        when(archiveRepository.findInterval(anyLong(), anyLong(), any())).then(args -> findArchive(args.getArgument(0), args.getArgument(1)));

    }

    List<ExecutionArchive> archives = new LinkedList<>();

    List<ExecutionArchive> findArchive(long start, long end) {
        return archives.stream().filter(e -> e.getStartTime().getTimeInMillis() >= start && e.getStartTime().getTimeInMillis() < end).collect(Collectors.toList());
    }
}
