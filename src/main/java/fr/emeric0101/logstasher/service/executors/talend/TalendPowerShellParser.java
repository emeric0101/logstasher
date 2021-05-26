package fr.emeric0101.logstasher.service.executors.talend;

import fr.emeric0101.logstasher.service.executors.ScriptParserAbstract;

import java.io.*;
import java.util.LinkedList;
import java.util.List;

public class TalendPowerShellParser extends ScriptParserAbstract {

    @Override
    public List<String> parse(File file) throws IOException {
        super.parse(file);
        // get all content
        List<String> lines = super.parse(file);
        // find the command line
        String commandLine = lines.stream().filter(e -> e.contains("java")).findFirst().orElseThrow(() -> new RuntimeException("Unable to found java command in bat file"));
        commandLine = commandLine.replace("%*", "");
        commandLine = commandLine.replace("$args", "");
        commandLine = commandLine.replace("%cd%", "");
        commandLine = commandLine.replace("\"", "\\\"");
        commandLine = commandLine.replace("java", "");
        List<String> result = new LinkedList<>();
        result.add("powershell.exe");
        result.add("java");
        result.add("" + commandLine + "");
        return result;
    }

}
