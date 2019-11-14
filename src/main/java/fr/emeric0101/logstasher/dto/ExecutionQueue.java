package fr.emeric0101.logstasher.dto;

import fr.emeric0101.logstasher.entity.Batch;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ExecutionQueue {
    public List<ExecutionBatch> queue;

    public ExecutionQueue(List<Batch> batches) {
        queue = batches.stream().map(e -> new ExecutionBatch(e)).collect(Collectors.toList());
    }


    public List<ExecutionBatch> getQueue() {
        return queue;
    }

    public void setQueue(List<ExecutionBatch> queue) {
        this.queue = queue;
    }


    public class ExecutionBatch {
        private Batch batch;
        private String state;
        private List<String> output = new ArrayList<>();

        public ExecutionBatch(Batch batch) {
            this.batch = batch;
            state = "IDLE";
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

    }
}
