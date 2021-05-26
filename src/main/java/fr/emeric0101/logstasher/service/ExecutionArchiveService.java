package fr.emeric0101.logstasher.service;

import fr.emeric0101.logstasher.entity.ExecutionArchive;
import fr.emeric0101.logstasher.entity.Batch;
import fr.emeric0101.logstasher.entity.ExecutionArchiveTypeEnum;
import fr.emeric0101.logstasher.entity.Pipeline;
import fr.emeric0101.logstasher.repository.ExecutionArchiveRepository;
import org.apache.commons.io.FileUtils;
import org.elasticsearch.action.search.SearchPhaseExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class ExecutionArchiveService {

    @Autowired
    ExecutionArchiveRepository executionArchiveRepository;

    /**
     *
     * @param batch
     * @param pipelines
     * @param startTime
     * @param endTime
     * @param state
     * @param type AUTO or MANUAL
     * @return
     */
    public ExecutionArchive saveArchive(Batch batch, List<Pipeline> pipelines, Calendar startTime, Calendar endTime, String state, ExecutionArchiveTypeEnum type) {
        Calendar expectedStart = null;
        // compute expected date
        if (batch != null && batch.getStartHour() != null && batch.getStartMinute() != null) {
            Calendar setDate = Calendar.getInstance();
            Calendar currentDate = Calendar.getInstance();
            expectedStart = Calendar.getInstance();
            expectedStart.set(
                    currentDate.get(Calendar.YEAR),
                    currentDate.get(Calendar.MONTH),
                    currentDate.get(Calendar.DATE),
                    batch.getStartHour(),
                    batch.getStartMinute()
            );
        }


        Calendar finalExpectedStart = expectedStart;
        return executionArchiveRepository.save(new ExecutionArchive(){{
            setBatch(batch);
            setPipeline(pipelines);
            setStartTime(startTime);
            setEndTime(endTime);
            setState(state);
            setExpectedStart(finalExpectedStart != null ? finalExpectedStart : null);
            setType(type.toString());
        }});
    }

    public void save(ExecutionArchive currentExecutionArchive) {
        executionArchiveRepository.save(currentExecutionArchive);
    }

    /**
     * Find all archive between date and the interval
     * @param intervalMinutes
     * @param batchId
     * @return
     */
    public List<ExecutionArchive> findInInterval(Calendar currentDate, int intervalMinutes, String batchId) {
        Calendar currentDateClone = (Calendar)currentDate.clone();
        currentDateClone.add(Calendar.MINUTE, -intervalMinutes*2);
        try {
            return executionArchiveRepository.findInterval(currentDateClone.getTimeInMillis(), Calendar.getInstance().getTimeInMillis(), batchId);
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
                executionArchiveRepository.findIntervalPage(cal.getTimeInMillis(), Calendar.getInstance().getTimeInMillis(), pageRequest)
                .spliterator(), false).collect(Collectors.toList());

    }

    public void clear() {
        executionArchiveRepository.deleteAll();
    }

    public void initArchive() {
        clear();
        save(new ExecutionArchive(){{
            setState("INIT");
            setStartTime(Calendar.getInstance());
        }});
    }

    /**
     *
     * Fetch Log data from a archive
     * @param executionArchiveId
     * @return
     */
    public List<String> getLog(String executionArchiveId) {
        Optional<ExecutionArchive> archive = executionArchiveRepository.findById(executionArchiveId);
        if (!archive.isPresent()) {
            return Arrays.asList("NOT_FOUND");
        }
        String path = archive.get().getLogPath();
        if (path == null) {
            return Arrays.asList("NOT_FOUND");
        }
        File logFile = new File(path);
        InputStream inputStream = null;
        try {
            inputStream = (new FileInputStream(logFile));

            List<String> resultStringBuilder = new LinkedList();
            try (BufferedReader br
                         = new BufferedReader(new InputStreamReader(inputStream))) {
                String line;
                while ((line = br.readLine()) != null) {
                    resultStringBuilder.add(line);
                }

                return resultStringBuilder;

            } catch (IOException e) {
                e.printStackTrace();
                return Arrays.asList("READ_ERROR");
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return Arrays.asList("READ_ERROR");
        }

    }
}
