package fr.emeric0101.logstasher.dto;

import java.util.List;

public class LogstashRunning {
    private String state;
    private ExecutionQueue queue;
    private List<String> buffer;

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public ExecutionQueue getQueue() {
        return queue;
    }

    public void setQueue(ExecutionQueue queue) {
        this.queue = queue;
    }

    public List<String> getBuffer() {
        return buffer;
    }

    public void setBuffer(List<String> buffer) {
        this.buffer = buffer;
    }
}
