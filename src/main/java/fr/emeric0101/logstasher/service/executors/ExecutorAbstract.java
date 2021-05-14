package fr.emeric0101.logstasher.service.executors;

import fr.emeric0101.logstasher.dto.ExecutionBatch;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.function.Consumer;

public abstract class ExecutorAbstract implements ExecutorInterface {

    ConcurrentLinkedQueue buffer = new ConcurrentLinkedQueue<String>();

    Consumer<String> logAddLines;
    ExecutionBatch currentBatch;
    String startDate;

    protected final boolean isWindows;


    public ExecutorAbstract() {
        isWindows = System.getProperty("os.name").startsWith("Windows");
    }

    private String generateDate() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/M/yyyy HH:mm:ss");
        return simpleDateFormat.format(new Date());
    }

    public ConcurrentLinkedQueue<String> getBuffer() {
            return buffer;
    }


    public void clearBuffer() {
        buffer.clear();
    }

    /**
     * Thread safe operation
     * @param line
     * @throws InterruptedException
     */
    public void bufferWrite(String line) {
        if (currentBatch != null) {
            currentBatch.getOutput().add(line);
        }
        if (buffer.size() > 300) {
            // remove first line
            buffer.remove(0);
        }
        buffer.add(line);
        logAddLines.accept(line);
    }

    protected void initialize(ExecutionBatch executionBatch, Consumer<String> logAddLines) {
        this.currentBatch = executionBatch;
        this.startDate = generateDate();
        this.logAddLines = logAddLines;
    }
    public String getStartDate() {
        return startDate;
    }

    public ExecutionBatch getCurrentBatch() {
        return currentBatch;
    }

}
