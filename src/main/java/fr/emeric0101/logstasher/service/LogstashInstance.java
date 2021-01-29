package fr.emeric0101.logstasher.service;

import fr.emeric0101.logstasher.configuration.LogstashProperties;
import fr.emeric0101.logstasher.dto.ExecutionBatch;
import fr.emeric0101.logstasher.entity.Pipeline;
import fr.emeric0101.logstasher.exception.LogstashNotFoundException;
import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.function.BiConsumer;
import java.util.function.Consumer;


public class LogstashInstance {
    final static String log_cmd = "java.exe -Xms1g -Xmx1g -XX:+UseParNewGC -XX:+UseConcMarkSweepGC -XX:CMSInitiatingOccupancyFraction=75 -XX:+UseCMSInitiatingOccupancyOnly -Djava.awt.headless=true -Dfile.encoding=UTF-8 -Djruby.compile.invokedynamic=true -Djruby.jit.threshold=0 -XX:+HeapDumpOnOutOfMemoryError -Djava.security.egd=file:/dev/urandom  -cp \"\"%LOGSTASH_PATH%/logstash-core/lib/jars/animal-sniffer-annotations-1.14.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/commons-codec-1.11.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/commons-compiler-3.0.8.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/error_prone_annotations-2.0.18.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/google-java-format-1.1.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/gradle-license-report-0.7.1.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/guava-22.0.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/j2objc-annotations-1.1.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/jackson-annotations-2.9.5.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/jackson-core-2.9.5.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/jackson-databind-2.9.5.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/jackson-dataformat-cbor-2.9.5.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/janino-3.0.8.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/javassist-3.22.0-GA.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/jruby-complete-9.1.13.0.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/jsr305-1.3.9.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/log4j-api-2.9.1.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/log4j-core-2.9.1.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/log4j-slf4j-impl-2.9.1.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/logstash-core.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/org.eclipse.core.commands-3.6.0.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/org.eclipse.core.contenttype-3.4.100.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/org.eclipse.core.expressions-3.4.300.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/org.eclipse.core.filesystem-1.3.100.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/org.eclipse.core.jobs-3.5.100.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/org.eclipse.core.resources-3.7.100.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/org.eclipse.core.runtime-3.7.0.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/org.eclipse.equinox.app-1.3.100.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/org.eclipse.equinox.common-3.6.0.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/org.eclipse.equinox.preferences-3.4.1.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/org.eclipse.equinox.registry-3.5.101.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/org.eclipse.jdt.core-3.10.0.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/org.eclipse.osgi-3.7.1.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/org.eclipse.text-3.5.101.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/slf4j-api-1.7.25.jar\"\" org.logstash.Logstash";

    LogstashProperties logstashProperties;

    boolean isWindows;

    Process logstashInstance = null;
    Thread bufferThread;
    Semaphore semaphore = new Semaphore(1);

    private String dataPath;

    private String instanceName;


    List<String> buffer = new ArrayList<String>();
    String startDate;

    ExecutionBatch currentBatch;
    List<Pipeline> currentPipelines;

    // only when Successfully started Logstash API endpoint {:port=>9600}
    boolean successfullyStarted = false;

    public String getDate() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/M/yyyy HH:mm:ss");
        return simpleDateFormat.format(new Date());
    }

    public LogstashInstance(LogstashProperties logstashProperties, String instanceName) {
        this.logstashProperties = logstashProperties;
        isWindows = System.getProperty("os.name").startsWith("Windows");
        dataPath = "data_logstasher-" + instanceName;
        this.instanceName = instanceName;
    }

    /**
     * Remove all data stored by logstash
     */
    public void clearData() {
        File path = new File(logstashProperties.getPath() + (isWindows ? "\\" : "/") + dataPath);
        if (path.exists()) {
            try {
                FileUtils.deleteDirectory(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * Start logstash
     * @param batch (null for pipeline)
     * @param pipelines (null for batch)
     * @param endCallback
     * @param logAddLines
     */
    public void start(ExecutionBatch batch, List<Pipeline> pipelines, BiConsumer<Integer, Boolean> endCallback, Consumer<String> logAddLines) throws LogstashNotFoundException {
        successfullyStarted = false;

        currentBatch = batch;
        currentPipelines = pipelines;
        System.out.println("Starting logstash " + instanceName);
        startDate = getDate();
        if (logstashInstance != null && logstashInstance.isAlive()) {
            return;
        }

        try {

            // control logstash path
            File logstashBin = new File(logstashProperties.getPath() + File.separator + "bin" + File.separator + "logstash");
            if (!logstashBin.exists()) {
                throw new LogstashNotFoundException("Logstash not found at path : " + logstashProperties.getPath());
            }

            List<String> cmds = new ArrayList<String>();
            cmds.addAll(Arrays.asList(log_cmd.replaceAll("%LOGSTASH_PATH%", logstashProperties.getPath()).split(" ")));
            // if batch, run it
            if (currentBatch != null) {
                // archive execution batch state
                cmds.addAll(new ArrayList<String>() {{
                    add("-e");
                    add("\"" + currentBatch.getBatch().getContent()
                            .replace("\"", "\\\"").replace("\t", " ")
                            + "\"");
                }});
            }
            cmds.add("--path.data");
            cmds.add(logstashProperties.getPath() + (isWindows ? "\\" : "/") + dataPath);

            ProcessBuilder pb = new ProcessBuilder(cmds);
            pb.redirectErrorStream(true);
            pb.environment().put("LS_HOME", logstashProperties.getPath());


            logstashInstance = pb.start();
            bufferThread = new Thread(() -> {
                while (logstashInstance.isAlive()) {
                    try {
                        BufferedReader input = new BufferedReader(new InputStreamReader(logstashInstance.getInputStream()));
                        String line = input.readLine();
                        if (line != null) {
                            // flag that logstash is OK
                            if (!successfullyStarted && line.contains("Successfully started Logstash API endpoint {:port=>9600}")) {
                                successfullyStarted = true;
                            }

                            semaphore.acquire();
                            if (currentBatch != null) {
                                currentBatch.getOutput().add(line);
                            }
                            if (buffer.size() > 300) {
                                // remove first line
                                buffer.remove(0);
                            }
                            buffer.add(line);

                            semaphore.release();
                            logAddLines.accept(line);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }
                System.out.println("Logstash has end up with code "+ logstashInstance.exitValue());
                if (endCallback != null) {
                    endCallback.accept(logstashInstance.exitValue(), successfullyStarted);
                }
            });
            bufferThread.start();
            // Start the process buffer
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stopLogstash() {
        System.out.println("Shutdown logstash " + instanceName);

        if (logstashInstance != null && logstashInstance.isAlive()) {
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
            logstashInstance = null;

        }
    }

    public List<String> getBuffer() {
        try {
            semaphore.acquire();
            return new ArrayList<String>() {{
                addAll(buffer);
            }};
        } catch (InterruptedException e) {
            throw new RuntimeException("Unable to get semaphone on logstash buffer");
        } finally {
            semaphore.release();
        }
    }

    public void clearBuffer() {
        try {
            semaphore.acquire();
            buffer.clear();
        } catch (InterruptedException e) {
            throw new RuntimeException("Unable to get semaphone on logstash buffer");
        } finally {
            semaphore.release();
        }
    }

    public String getState() {
        if (logstashInstance == null || !logstashInstance.isAlive()){
            return "STOPPED";
        } else if (successfullyStarted)  {
            return "RUNNING";
        }
        return "STARTING";
    }

    public String getStartDate() {
        return startDate;
    }

    public ExecutionBatch getCurrentBatch() {
        return currentBatch;
    }

    public List<Pipeline> getCurrentPipelines() {
        return currentPipelines;
    }

    public boolean isAlive() {
        return logstashInstance != null && logstashInstance.isAlive();
    }
}
