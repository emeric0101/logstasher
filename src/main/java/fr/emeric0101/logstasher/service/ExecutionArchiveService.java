package fr.emeric0101.logstasher.service;

import fr.emeric0101.logstasher.entity.ExecutionArchive;
import fr.emeric0101.logstasher.entity.Batch;
import fr.emeric0101.logstasher.entity.Pipeline;
import fr.emeric0101.logstasher.repository.ExecutionArchiveRepository;
import org.elasticsearch.action.search.SearchPhaseExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class ExecutionArchiveService {

    @Autowired
    ExecutionArchiveRepository executionArchiveRepository;

    public ExecutionArchive saveArchive(Batch batch, List<Pipeline> pipelines, Date startTime, Date endTime, String state) {
        return executionArchiveRepository.save(new ExecutionArchive(){{
            setBatch(batch);
            setPipeline(pipelines);
            setStartTime(startTime);
            setEndTime(endTime);
            setState(state);
        }});
    }

    public void save(ExecutionArchive currentExecutionArchive) {
        executionArchiveRepository.save(currentExecutionArchive);
    }

    public List<ExecutionArchive> findToday() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        PageRequest pageRequest = PageRequest.of(0, 1000, Sort.Direction.DESC, "startTime");
        try {
            return executionArchiveRepository.findInterval(cal.getTimeInMillis(), Calendar.getInstance().getTimeInMillis(), pageRequest);
        } catch (SearchPhaseExecutionException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public List<ExecutionArchive> findLastWeek() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.add(Calendar.DATE, -7);
        PageRequest pageRequest = PageRequest.of(0, 1000, Sort.Direction.DESC, "startTime");
        return StreamSupport.stream(
                executionArchiveRepository.findInterval(cal.getTimeInMillis(), Calendar.getInstance().getTimeInMillis(), pageRequest)
                .spliterator(), false).collect(Collectors.toList());

    }

    public void clear() {
        executionArchiveRepository.deleteAll();
    }

    public void initArchive() {
        clear();
        save(new ExecutionArchive(){{
            setState("INIT");
            setStartTime(new Date());
        }});
    }
}
