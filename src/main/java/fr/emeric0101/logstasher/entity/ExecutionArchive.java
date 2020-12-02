package fr.emeric0101.logstasher.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

import java.nio.channels.Pipe;
import java.util.Date;
import java.util.List;

@Document(indexName = "logstasher_archive", type = "doc")
public class ExecutionArchive {
    @Id
    private String id;
    private Batch batch;
    private List<Pipeline> pipeline;
    private Date startTime;
    private Date endTime;
    private String state;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Batch getBatch() {
        return batch;
    }

    public void setBatch(Batch batch) {
        this.batch = batch;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public List<Pipeline> getPipeline() {
        return pipeline;
    }

    public void setPipeline(List<Pipeline> pipeline) {
        this.pipeline = pipeline;
    }
}
