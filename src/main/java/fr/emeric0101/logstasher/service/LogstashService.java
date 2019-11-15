package fr.emeric0101.logstasher.service;

import fr.emeric0101.logstasher.configuration.LogstashProperties;
import fr.emeric0101.logstasher.dto.ExecutionQueue;
import fr.emeric0101.logstasher.dto.LogstashRunning;
import fr.emeric0101.logstasher.entity.Batch;
import fr.emeric0101.logstasher.repository.BatchRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Semaphore;

@Service
public class LogstashService {
    final static String log_cmd = "java.exe -Xms1g -Xmx1g -XX:+UseParNewGC -XX:+UseConcMarkSweepGC -XX:CMSInitiatingOccupancyFraction=75 -XX:+UseCMSInitiatingOccupancyOnly -Djava.awt.headless=true -Dfile.encoding=UTF-8 -Djruby.compile.invokedynamic=true -Djruby.jit.threshold=0 -XX:+HeapDumpOnOutOfMemoryError -Djava.security.egd=file:/dev/urandom  -cp \"\"%LOGSTASH_PATH%/logstash-core/lib/jars/animal-sniffer-annotations-1.14.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/commons-codec-1.11.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/commons-compiler-3.0.8.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/error_prone_annotations-2.0.18.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/google-java-format-1.1.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/gradle-license-report-0.7.1.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/guava-22.0.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/j2objc-annotations-1.1.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/jackson-annotations-2.9.5.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/jackson-core-2.9.5.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/jackson-databind-2.9.5.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/jackson-dataformat-cbor-2.9.5.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/janino-3.0.8.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/javassist-3.22.0-GA.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/jruby-complete-9.1.13.0.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/jsr305-1.3.9.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/log4j-api-2.9.1.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/log4j-core-2.9.1.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/log4j-slf4j-impl-2.9.1.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/logstash-core.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/org.eclipse.core.commands-3.6.0.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/org.eclipse.core.contenttype-3.4.100.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/org.eclipse.core.expressions-3.4.300.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/org.eclipse.core.filesystem-1.3.100.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/org.eclipse.core.jobs-3.5.100.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/org.eclipse.core.resources-3.7.100.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/org.eclipse.core.runtime-3.7.0.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/org.eclipse.equinox.app-1.3.100.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/org.eclipse.equinox.common-3.6.0.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/org.eclipse.equinox.preferences-3.4.1.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/org.eclipse.equinox.registry-3.5.101.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/org.eclipse.jdt.core-3.10.0.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/org.eclipse.osgi-3.7.1.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/org.eclipse.text-3.5.101.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/slf4j-api-1.7.25.jar\"\" org.logstash.Logstash";
    @Autowired
    LogstashProperties logstashProperties;

    @Autowired
    SimpMessagingTemplate template;

    @Autowired
    ExecutionQueueSerializer executionQueueSerializer;



    Process logstashInstance = null;
    Thread bufferThread;
    Semaphore semaphore = new Semaphore(1);

    List<String> buffer = new ArrayList<String>();
    String state = "STOPPED";

    String startDate = null;

    boolean isWindows;
    private Iterator<ExecutionQueue.ExecutionBatch> queueIterator;
    private ExecutionQueue.ExecutionBatch currentBatch;
    private ExecutionQueue currentQueue;
    private String dataPath = "data_logstasher";


    public LogstashService() {
        isWindows = System.getProperty("os.name").startsWith("Windows");
    }



    /**
     * Start logstash with args
     */
    public void start() {
        if (logstashInstance != null && logstashInstance.isAlive()) {
            return;
        }
        changeState("STARTING");


        try {
            List<String> cmds = new ArrayList<String>();
            cmds.addAll(Arrays.asList(log_cmd.replaceAll("%LOGSTASH_PATH%", logstashProperties.getPath()).split(" ")));
            // if batch, run it
            if (currentBatch != null) {
                cmds.addAll(new ArrayList<String>(){{add("-e"); add("\"" + currentBatch.getBatch().getContent()
                        .replace("\"", "\\\"").replace("\t", " ")
                         + "\"");}});
                currentBatch.setState("RUNNING");
                executionQueueSerializer.saveLog(startDate, currentBatch.getBatch().getId(), "Starting batch");
                sendState();
            }
            cmds.add("--path.data");
            cmds.add(logstashProperties.getPath() + (isWindows ? "\\" : "/") + dataPath);

            ProcessBuilder pb = new ProcessBuilder(cmds);
            pb.environment().put("LS_HOME", logstashProperties.getPath());


            logstashInstance = pb.start();
            bufferThread =  new Thread(() -> {
                while (logstashInstance.isAlive()) {
                    try {
                        BufferedReader input = new BufferedReader(new InputStreamReader(logstashInstance.getInputStream()));
                        String line = input.readLine();
                        if (line != null) {
                            if (state == "STARTING" && line.contains("Successfully started Logstash")) {
                                changeState("RUNNING");

                            }
                            semaphore.acquire();
                            if (currentBatch != null) {
                                currentBatch.getOutput().add(line);
                            }
                            buffer.add(line);
                            executionQueueSerializer.saveLog(startDate, currentBatch.getBatch().getId(), line);

                            semaphore.release();
                            sendState();
                        }
                    }
                    catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }

                }
                executionQueueSerializer.saveLog(startDate, currentBatch.getBatch().getId(), "End with " + logstashInstance.exitValue());

                processEnded(logstashInstance.exitValue());
            });
            bufferThread.start();
            // Start the process buffer
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * If a batchQueue is running, go into next step
     * @param exitValue
     */
    private void processEnded(int exitValue) {
        if (currentBatch != null) {
            if (exitValue != 0) {
                currentBatch.setState("ERROR");
            } else {
                currentBatch.setState("DONE");
            }
            sendState();
            // next step ?
            if (queueIterator.hasNext()) {
                currentBatch = queueIterator.next();
                // start again :)
                start();
            } else {
                // batch has error ?
                // save the log of the current batch
                if (currentQueue.getQueue().stream().anyMatch(e -> e.getState().equals("ERROR"))) {
                    changeState("ERROR");
                } else {
                    changeState("DONE");
                }
                currentQueue = null;
            }

        } else {
            // pipeline
            if (exitValue != 0) {
                changeState("ERROR");

            } else {
                changeState("STOPPED");

            }
        }


    }


    public void restart() {
        startDate = executionQueueSerializer.getDate();

        stopLogstash();
        buffer.clear();
        start();
    }

    public void stopLogstash() {
        if (logstashInstance != null && logstashInstance.isAlive()) {
            changeState("KILLING");

            logstashInstance.destroy();
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (logstashInstance.isAlive()) {
                Process p = logstashInstance.destroyForcibly();

                try {
                    p.waitFor();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            bufferThread.interrupt();

        }
        changeState("STOPPED");
        logstashInstance = null;
        currentQueue = null;
        currentBatch = null;
    }

    public LogstashRunning getRunning() {
        LogstashRunning running = new LogstashRunning();
        try {
            semaphore.acquire();
            running.setState(state);
            running.setQueue(currentQueue);
            running.setBuffer(buffer);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        finally {
            semaphore.release();
        }

        return running;
    }


    public void startFromQueue(ExecutionQueue queue) {
        startDate = executionQueueSerializer.getDate();
        if (currentQueue != null) {
            throw new RuntimeException("Queue is already running");
        }
        if (!queue.getQueue().iterator().hasNext()) {
            return;
        }
        currentQueue = queue;
        stopLogstash();
        buffer.clear();

        queueIterator = queue.getQueue().iterator();

        currentBatch = queueIterator.next();
        start();
    }

    private void changeState(String state) {
        this.state = state;
        sendState();
    }

    /**
     * Send the current state the user
     */
    private void sendState() {
        template.convertAndSend("/state", getRunning());
    }
}
