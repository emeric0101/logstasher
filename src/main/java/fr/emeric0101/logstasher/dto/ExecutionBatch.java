package fr.emeric0101.logstasher.dto;

import fr.emeric0101.logstasher.entity.Batch;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;



public class ExecutionBatch {
    private Batch batch;
    private String state = "IDLE";
    private boolean automaticallyStarted = false;
    private List<String> output = new ArrayList<>();


    public ExecutionBatch(Batch batch, boolean automaticallyStarted) {
        this.automaticallyStarted = automaticallyStarted;
        this.batch = batch;
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
}
