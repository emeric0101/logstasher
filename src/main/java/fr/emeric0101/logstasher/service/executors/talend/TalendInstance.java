package fr.emeric0101.logstasher.service.executors.talend;

import fr.emeric0101.logstasher.dto.ExecutionBatch;
import fr.emeric0101.logstasher.entity.Pipeline;
import fr.emeric0101.logstasher.exception.LogstashNotFoundException;
import fr.emeric0101.logstasher.service.executors.BufferThreadManager;
import fr.emeric0101.logstasher.service.executors.ExecutorAbstract;
import fr.emeric0101.logstasher.service.executors.ExecutorStateEnum;
import fr.emeric0101.logstasher.service.executors.ProcessHelper;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
public class TalendInstance extends ExecutorAbstract {
    Process talendProcess = null;
    boolean successfullyStarted = false;
    BufferThreadManager bufferThread;

    public TalendInstance() {
        super();
    }

    @Override
    public void clearData() {

    }

    @Override
    public void start(ExecutionBatch batch, List<Pipeline> pipelines, BiConsumer<Integer, Boolean> endCallback, Consumer<String> logAddLines, Runnable startedCallback) {
        if (pipelines != null && !pipelines.isEmpty()) {
            throw new RuntimeException("Pipelines are not supported by Talend yet");
        }
        if (batch.getBatch().getTalendPowershellName() == null || batch.getBatch().getTalendPowershellName().isEmpty()) {
            throw new RuntimeException("Talend jar name not provided");
        }
        initialize(batch, logAddLines);


        log.info("Starting Talend ");
        if (talendProcess != null && talendProcess.isAlive()) {
            log.info("Talend already running, unable to start");
            return;
        }
        try {
            // controls talend jar path
            // control logstash path
            String path = batch.getBatch().getContent();
            File talendBat = new File(path + File.separator + batch.getBatch().getTalendPowershellName());
            if (!talendBat.exists()) {
                throw new LogstashNotFoundException("Talend not found at path : " + path + File.separator + batch.getBatch().getTalendPowershellName());
            }
            // parse the Bat and extract the command lines to start talend
            TalendBatParser talendBatParser = new TalendBatParser(talendBat);
                List<String> cmds = talendBatParser.getCommandsFromBat();

            ProcessBuilder pb = new ProcessBuilder(cmds);
            log.debug(cmds.stream().collect(Collectors.joining(" ")));
            pb.redirectErrorStream(true);

            // set current working dir
            pb.directory(new File(path));
            talendProcess = pb.start();
            bufferThread = new BufferThreadManager(talendProcess, (returnCode) -> {
                // On exit process
                endCallback.accept(returnCode, successfullyStarted);
            }, (line) -> {
                bufferWrite(line);
                // when return data from process
                if (!successfullyStarted) {
                    successfullyStarted = true;
                }
            });
            bufferThread.start();
            startedCallback.run();

        }
        catch (IOException e) {
            e.printStackTrace();
            log.error("Unable to parse BAT file, IOException : " + e.getMessage());
        }

    }

    @Override
    public void stop() {
        log.info("Shutdown Talend");

        ProcessHelper.stop(talendProcess);
        if (bufferThread.isRunning()) {
            bufferThread.stop();
        }
        talendProcess = null;
    }

    @Override
    public ExecutorStateEnum getState() {
        if (talendProcess == null || !talendProcess.isAlive()){
            return ExecutorStateEnum.STOPPED;
        } else if (successfullyStarted)  {
            return ExecutorStateEnum.RUNNING;
        } else  {
            return ExecutorStateEnum.STARTING;
        }
    }

    @Override
    public boolean isAlive() {
        if (talendProcess == null) {
            return false;
        }
        return talendProcess.isAlive();
    }
}
