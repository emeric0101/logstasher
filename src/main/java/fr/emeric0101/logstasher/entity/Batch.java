package fr.emeric0101.logstasher.entity;

import fr.emeric0101.logstasher.dto.RestRequest;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

import java.util.List;

@Document(indexName = "logstasher_batch", type = "doc")
@Data
public class Batch {
    @Id
    private String id;

    private String content;
    private Integer startHour;
    private Integer startMinute;

    private RecurrenceEnum recurrence = null;
    // In case of weekly (sunday = 1, ...)
    private List<Integer> weekDays = null;
    // in case of monthly
    private Integer monthDate = null;

    private boolean activated;
    private int order;
    private long timeout;
    private List<RestRequest> entyRequests;

    private ExecutorEnum executor = ExecutorEnum.LOGSTASH_BATCH;

    public ExecutorEnum getExecutor() {
        // retro compatibility
        return executor != null ? executor : ExecutorEnum.LOGSTASH_BATCH;
    }
}
