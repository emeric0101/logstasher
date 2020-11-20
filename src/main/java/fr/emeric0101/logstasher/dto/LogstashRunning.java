package fr.emeric0101.logstasher.dto;

import fr.emeric0101.logstasher.entity.Batch;
import fr.emeric0101.logstasher.entity.Pipeline;

import java.sql.Date;
import java.util.List;

public class LogstashRunning {
    private String state;
    private List<String> buffer;
    private String instance;
    private Batch batch;
    private List<Pipeline> pipelines;
    private String started;

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public List<String> getBuffer() {
        return buffer;
    }

    public void setBuffer(List<String> buffer) {
        this.buffer = buffer;
    }

    public String getInstance() {
        return instance;
    }

    public void setInstance(String instance) {
        this.instance = instance;
    }

    public Batch getBatch() {
        return batch;
    }

    public void setBatch(Batch batch) {
        this.batch = batch;
    }

    public List<Pipeline> getPipelines() {
        return pipelines;
    }

    public void setPipelines(List<Pipeline> pipelines) {
        this.pipelines = pipelines;
    }

    public String getStarted() {
        return started;
    }

    public void setStarted(String started) {
        this.started = started;
    }
}
