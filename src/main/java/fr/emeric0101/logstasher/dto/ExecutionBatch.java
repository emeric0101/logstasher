package fr.emeric0101.logstasher.dto;

import fr.emeric0101.logstasher.entity.Batch;
import fr.emeric0101.logstasher.entity.ExecutionArchive;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;



public class ExecutionBatch {
    private Batch batch;
    private String state = "IDLE";
    private boolean automaticallyStarted = false;
    private ExecutionArchive archive;
    private List<String> output = new ArrayList<>();


    public ExecutionBatch(Batch batch, boolean automaticallyStarted, ExecutionArchive archive) {
        this.automaticallyStarted = automaticallyStarted;
        this.batch = batch;
        this.archive = archive;
    }

    public Batch getBatch() {
        return batch;
    }

    public void setBatch(Batch batch) {
        this.batch = batch;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public List<String> getOutput() {
        return output;
    }

    public boolean isAutomaticallyStarted() {
        return automaticallyStarted;
    }

    public void setAutomaticallyStarted(boolean automaticallyStarted) {
        this.automaticallyStarted = automaticallyStarted;
    }

    public ExecutionArchive getArchive() {
        return archive;
    }

    public void setArchive(ExecutionArchive archive) {
        this.archive = archive;
    }
}
