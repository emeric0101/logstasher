package fr.emeric0101.logstasher.service;

import fr.emeric0101.logstasher.entity.BatchArchive;
import fr.emeric0101.logstasher.entity.Batch;
import fr.emeric0101.logstasher.repository.BatchArchiveRepository;
import org.elasticsearch.action.search.SearchPhaseExecutionException;
import org.elasticsearch.index.query.QueryShardException;
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
public class ArchiveService {

    @Autowired
    BatchArchiveRepository batchArchiveRepository;

    public BatchArchive saveArchive(Batch batch, Date startTime, Date endTime, String state) {
        return batchArchiveRepository.save(new BatchArchive(){{
            setBatch(batch);
            setStartTime(startTime);
            setEndTime(endTime);
            setState(state);
        }});
    }

    public void save(BatchArchive currentBatchArchive) {
        batchArchiveRepository.save(currentBatchArchive);
    }

    public List<BatchArchive> findToday() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        PageRequest pageRequest = PageRequest.of(0, 1000, Sort.Direction.DESC, "startTime");
        try {
            return batchArchiveRepository.findInterval(cal.getTimeInMillis(), Calendar.getInstance().getTimeInMillis(), pageRequest);
        } catch (SearchPhaseExecutionException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public List<BatchArchive> findLastWeek() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.add(Calendar.DATE, -7);
        PageRequest pageRequest = PageRequest.of(0, 1000, Sort.Direction.DESC, "startTime");
        return StreamSupport.stream(
                batchArchiveRepository.findInterval(cal.getTimeInMillis(), Calendar.getInstance().getTimeInMillis(), pageRequest)
                .spliterator(), false).collect(Collectors.toList());

    }
}
