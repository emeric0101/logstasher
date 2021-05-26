package fr.emeric0101.logstasher.service.executors.logstash;

import fr.emeric0101.logstasher.service.executors.ScriptParserAbstract;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class LogstashBatParser extends ScriptParserAbstract {

    private String logstashPath;
    private List<String> execArgs;
    public LogstashBatParser(String logstashPath, List<String> execArgs) {
        this.logstashPath = logstashPath;
        this.execArgs = execArgs;
    }



    @Override
    public List<String> parse(File file) throws IOException {

        // get all content
        List<String> lines = super.parse(file);
        // find the command line
        List<String> result = new LinkedList<>();
        result.add("cmd.exe");
        result.add("/c");
        result.add(file.getAbsolutePath());
        result.addAll(execArgs);
        return result;
    }
}
