package fr.emeric0101.logstasher.entity;

import fr.emeric0101.logstasher.dto.RestRequest;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

import java.util.Date;
import java.util.List;

@Document(indexName = "batch")
public class Batch {
    @Id
    private String id;

    private String content;
    private Integer startHour;
    private Integer startMinute;
    private boolean activated;
    private int order;
    private long timeout;
    private List<RestRequest> entyRequests;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public boolean isActivated() {
        return activated;
    }

    public void setActivated(boolean activated) {
        this.activated = activated;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public Integer getStartHour() {
        return startHour;
    }

    public void setStartHour(Integer startHour) {
        this.startHour = startHour;
    }

    public Integer getStartMinute() {
        return startMinute;
    }

    public void setStartMinute(Integer startMinute) {
        this.startMinute = startMinute;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public List<RestRequest> getEntyRequests() {
        return entyRequests;
    }

    public void setEntyRequests(List<RestRequest> entyRequests) {
        this.entyRequests = entyRequests;
    }
}
