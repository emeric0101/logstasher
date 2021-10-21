package fr.emeric0101.logstasher.service;

import fr.emeric0101.logstasher.entity.ExecutionArchive;
import fr.emeric0101.logstasher.entity.Batch;
import fr.emeric0101.logstasher.entity.ExecutionArchiveTypeEnum;
import fr.emeric0101.logstasher.entity.Pipeline;
import fr.emeric0101.logstasher.repository.ExecutionArchiveRepository;
import org.apache.commons.io.FileUtils;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.search.SearchPhaseExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.*;
import java.text.SimpleDateFormat;
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
        var executionArchive = new ExecutionArchive();
        executionArchive.setBatch(batch);
        executionArchive.setPipeline(pipelines);
        executionArchive.setStartTime(startTime);
        executionArchive.setEndTime(endTime);
        executionArchive.setState(state);
        executionArchive.setExpectedStart(finalExpectedStart != null ? finalExpectedStart : null);
        executionArchive.setType(type.toString());

        return executionArchiveRepository.save(executionArchive);
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
            return executionArchiveRepository.findInterval(
                    simpleDateFormat.format(currentDateClone.getTime()),
                    simpleDateFormat.format(Calendar.getInstance().getTime()),
                    batchId);
        } catch (SearchPhaseExecutionException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX");

    public List<ExecutionArchive> findLastWeek() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.add(Calendar.DATE, -7);
        //fixme : move to localdatetime
        PageRequest pageRequest = PageRequest.of(0, 1000, Sort.Direction.DESC, "startTime");
        try {
            return StreamSupport.stream(
                    executionArchiveRepository.findIntervalPage(
                            simpleDateFormat.format(cal.getTime()),
                            simpleDateFormat.format(Calendar.getInstance().getTime()),
                            pageRequest)
                            .spliterator(), false).collect(Collectors.toList());
        } catch (ElasticsearchStatusException e) {
            e.printStackTrace();
            // init ES
            initArchive();
            return findLastWeek();
        }


    }

    public void clear() {
        executionArchiveRepository.deleteAll();
    }

    public void initArchive() {
        clear();
        var archive = new ExecutionArchive();
        archive.setState("INIT");
        archive.setStartTime(Calendar.getInstance());
        save(archive);
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
