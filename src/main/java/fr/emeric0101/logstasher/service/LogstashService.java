package fr.emeric0101.logstasher.service;

import fr.emeric0101.logstasher.configuration.LogstashProperties;
import fr.emeric0101.logstasher.dto.LogstashRunning;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

@Service
public class LogstashService {
    final static String log_cmd = "java.exe -Xms1g -Xmx1g -XX:+UseParNewGC -XX:+UseConcMarkSweepGC -XX:CMSInitiatingOccupancyFraction=75 -XX:+UseCMSInitiatingOccupancyOnly -Djava.awt.headless=true -Dfile.encoding=UTF-8 -Djruby.compile.invokedynamic=true -Djruby.jit.threshold=0 -XX:+HeapDumpOnOutOfMemoryError -Djava.security.egd=file:/dev/urandom  -cp \"\"%LOGSTASH_PATH%/logstash-core/lib/jars/animal-sniffer-annotations-1.14.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/commons-codec-1.11.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/commons-compiler-3.0.8.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/error_prone_annotations-2.0.18.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/google-java-format-1.1.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/gradle-license-report-0.7.1.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/guava-22.0.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/j2objc-annotations-1.1.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/jackson-annotations-2.9.5.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/jackson-core-2.9.5.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/jackson-databind-2.9.5.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/jackson-dataformat-cbor-2.9.5.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/janino-3.0.8.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/javassist-3.22.0-GA.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/jruby-complete-9.1.13.0.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/jsr305-1.3.9.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/log4j-api-2.9.1.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/log4j-core-2.9.1.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/log4j-slf4j-impl-2.9.1.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/logstash-core.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/org.eclipse.core.commands-3.6.0.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/org.eclipse.core.contenttype-3.4.100.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/org.eclipse.core.expressions-3.4.300.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/org.eclipse.core.filesystem-1.3.100.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/org.eclipse.core.jobs-3.5.100.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/org.eclipse.core.resources-3.7.100.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/org.eclipse.core.runtime-3.7.0.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/org.eclipse.equinox.app-1.3.100.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/org.eclipse.equinox.common-3.6.0.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/org.eclipse.equinox.preferences-3.4.1.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/org.eclipse.equinox.registry-3.5.101.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/org.eclipse.jdt.core-3.10.0.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/org.eclipse.osgi-3.7.1.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/org.eclipse.text-3.5.101.jar\";\"%LOGSTASH_PATH%/logstash-core/lib/jars/slf4j-api-1.7.25.jar\"\" org.logstash.Logstash";
    @Autowired
    LogstashProperties logstashProperties;

    Process logstashInstance = null;
    Thread bufferThread;
    Semaphore semaphore = new Semaphore(1);

    List<String> buffer = new ArrayList<String>();
    String state = "STOPPED";

    boolean isWindows;



    public LogstashService() {
        isWindows = System.getProperty("os.name").startsWith("Windows");
    }

    public void start() {
        if (logstashInstance != null && logstashInstance.isAlive()) {
            return;
        }
        state = "STARTING";


        try {
            String[] cmds = log_cmd.replaceAll("%LOGSTASH_PATH%", logstashProperties.getPath()).split(" ");
            ProcessBuilder pb = new ProcessBuilder(cmds);
            pb.environment().put("LS_HOME", logstashProperties.getPath());
        /*        pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            pb.redirectError(ProcessBuilder.Redirect.INHERIT);
            pb.redirectInput(ProcessBuilder.Redirect.INHERIT);*/

            logstashInstance = pb.start();
            bufferThread =  new Thread(() -> {
                while (logstashInstance.isAlive()) {
                    try {
                        BufferedReader input = new BufferedReader(new InputStreamReader(logstashInstance.getInputStream()));
                        String line = input.readLine();
                        if (line != null) {
                            if (state == "STARTING" && line.contains("Successfully started Logstash")) {
                                state = "RUNNING";
                            }
                            semaphore.acquire();
                            buffer.add(line);
                            semaphore.release();
                        }
                    }
                    catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }

                }

            });
            bufferThread.start();
            // Start the process buffer
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<String> getBuffer() {
        try {
            semaphore.acquire();
            return new ArrayList<>(buffer);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        finally {
            semaphore.release();
        }
        return null;
    }

    public void restart() {
        stopLogstash();
        buffer.clear();
        start();
    }

    public void stopLogstash() {
        if (logstashInstance != null && logstashInstance.isAlive()) {
            state = "KILLING";
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
        state = "STOPPED";
        logstashInstance = null;
    }

    public LogstashRunning getRunning() {
        LogstashRunning running = new LogstashRunning();
        running.setState(state);
        return running;
    }
}
