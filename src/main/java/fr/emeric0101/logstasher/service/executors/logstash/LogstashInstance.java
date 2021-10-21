package fr.emeric0101.logstasher.service.executors.logstash;

import fr.emeric0101.logstasher.dto.ExecutionBatch;
import fr.emeric0101.logstasher.entity.Pipeline;
import fr.emeric0101.logstasher.exception.LogstashNotFoundException;
import fr.emeric0101.logstasher.service.GeneratorService;
import fr.emeric0101.logstasher.service.executors.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
public class LogstashInstance extends ExecutorAbstract {
    final static String log_cmd = "java.exe -Xms1g -Xmx1g -XX:+UseParNewGC -XX:+UseConcMarkSweepGC -XX:CMSInitiatingOccupancyFraction=75 -XX:+UseCMSInitiatingOccupancyOnly -Djava.awt.headless=true -Dfile.encoding=UTF-8 -Djruby.compile.invokedynamic=true -Djruby.jit.threshold=0 -XX:+HeapDumpOnOutOfMemoryError -Djava.security.egd=file:/dev/urandom  -cp \"\"%LOGSTASH_PATH%/logstash-core/lib/jars/animal-sniffer-annotations-1.14.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/commons-codec-1.11.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/commons-compiler-3.0.8.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/error_prone_annotations-2.0.18.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/google-java-format-1.1.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/gradle-license-report-0.7.1.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/guava-22.0.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/j2objc-annotations-1.1.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/jackson-annotations-2.9.5.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/jackson-core-2.9.5.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/jackson-databind-2.9.5.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/jackson-dataformat-cbor-2.9.5.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/janino-3.0.8.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/javassist-3.22.0-GA.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/jruby-complete-9.1.13.0.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/jsr305-1.3.9.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/log4j-api-2.9.1.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/log4j-core-2.9.1.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/log4j-slf4j-impl-2.9.1.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/logstash-core.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/org.eclipse.core.commands-3.6.0.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/org.eclipse.core.contenttype-3.4.100.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/org.eclipse.core.expressions-3.4.300.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/org.eclipse.core.filesystem-1.3.100.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/org.eclipse.core.jobs-3.5.100.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/org.eclipse.core.resources-3.7.100.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/org.eclipse.core.runtime-3.7.0.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/org.eclipse.equinox.app-1.3.100.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/org.eclipse.equinox.common-3.6.0.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/org.eclipse.equinox.preferences-3.4.1.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/org.eclipse.equinox.registry-3.5.101.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/org.eclipse.jdt.core-3.10.0.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/org.eclipse.osgi-3.7.1.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/org.eclipse.text-3.5.101.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/slf4j-api-1.7.25.jar\"\" org.logstash.Logstash";

    private String path;


    Process logstashProcess = null;
    BufferThreadManager bufferThread;

    private String dataPath;

    private String instanceName;

    private GeneratorService generatorService;



    List<Pipeline> currentPipelines;

    // only when Successfully started Logstash API endpoint {:port=>9600}
    boolean successfullyStarted = false;


    public LogstashInstance(String path, String instanceName, GeneratorService generatorService) {
        super();
        this.path = path;
        this.generatorService = generatorService;
        dataPath = "data_logstasher-" + instanceName;
        this.instanceName = instanceName;
    }

    /**
     * Remove all data stored by logstash
     */
    public void clearData() {
        File path = new File(this.path + (isWindows ? "\\" : "/") + dataPath);
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
    public void start(ExecutionBatch batch, List<Pipeline> pipelines, BiConsumer<Integer, Boolean> endCallback, Consumer<String> logAddLines, Runnable startedCallback) throws LogstashNotFoundException {
        if (bufferThread != null && bufferThread.isRunning()) {
            bufferThread.stop();
            log.warn("The bufferThread was not stopped automatically, strange...");
        }

        successfullyStarted = false;

        initialize(batch, logAddLines);

        currentPipelines = pipelines;
        log.info("Starting logstash " + instanceName);
        if (logstashProcess != null && logstashProcess.isAlive()) {
            log.info("logstash already running, unable to start");
            return;
        }

        try {
            // control logstash path
            File logstashBin = new File(this.path + File.separator + "bin" + File.separator + "logstash" + (isWindows ? ".bat" : ""));
            if (!logstashBin.exists()) {
                throw new LogstashNotFoundException("Logstash not found at path : " + this.path);
            }


            List<String> execArgs = new LinkedList<>();
            // if batch, run it
            if (getCurrentBatch() != null) {
                // generate batch file
                String batchPath = generatorService.generateBatch(batch.getBatch());
                // archive execution batch state
                execArgs.addAll(List.of("-f", batchPath));

            }
            execArgs.add("--path.data");
            execArgs.add(this.path + (isWindows ? "\\" : "/") + dataPath);

            ScriptParserAbstract batParser = new LogstashBatParser(this.path, execArgs);

            List<String> cmds = batParser.parse(logstashBin);

            ProcessBuilder pb = new ProcessBuilder(cmds);
            pb.redirectErrorStream(true);
            pb.environment().put("LS_HOME", this.path);

            log.info(cmds.stream().collect(Collectors.joining(" ")));

            logstashProcess = pb.start();
            bufferThread = new BufferThreadManager(logstashProcess, (returnCode) -> {
                if (getBuffer().stream().anyMatch(e -> e != null && (e.contains("No configuration found in the configured sources.") || e.contains("LogStash::ConfigurationError")))) {
                    // error in LOG
                    // On exit process
                    endCallback.accept(-1, successfullyStarted);
                    bufferThread.stop();
                } else {
                    // On exit process
                    endCallback.accept(returnCode, successfullyStarted);
                    bufferThread.stop();
                }


            }, (line) -> {
                bufferWrite(line);
                // when return data from process
                if (!successfullyStarted && line.contains("Successfully started Logstash API endpoint {:port=>9600}")) {
                    successfullyStarted = true;
                }
                if (line.contains("Logstash shut down")) {
                    // something logstash is stuck at the end, must auto kill the instance in this case
                    stop();
                }
            });
            bufferThread.start();
            startedCallback.run();

            // Start the process buffer
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        log.info("Shutdown logstash " + instanceName);

        ProcessHelper.stop(logstashProcess);

        logstashProcess = null;
    }




    public ExecutorStateEnum getState() {
        if (logstashProcess == null || !logstashProcess.isAlive()){
            return ExecutorStateEnum.STOPPED;
        } else if (successfullyStarted)  {
            return ExecutorStateEnum.RUNNING;
        }
        return ExecutorStateEnum.STARTING;
    }


    public List<Pipeline> getCurrentPipelines() {
        return currentPipelines;
    }

    public boolean isAlive() {
        return logstashProcess != null && logstashProcess.isAlive();
    }

}
