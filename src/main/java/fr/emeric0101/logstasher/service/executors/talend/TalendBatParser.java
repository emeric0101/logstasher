package fr.emeric0101.logstasher.service.executors.talend;

import org.apache.commons.io.FileUtils;

import java.io.*;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class TalendBatParser {
    File file;
    public TalendBatParser(File file) {
        this.file = file;
    }

    public List<String> getCommandsFromBat() throws IOException {
        // get all content
        List<String> lines = readFromInputStream(new FileInputStream(file));
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

    private List<String> readFromInputStream(InputStream inputStream)
            throws IOException {
        List<String> resultStringBuilder = new LinkedList();
        try (BufferedReader br
                     = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = br.readLine()) != null) {
                resultStringBuilder.add(line);
            }
        }
        return resultStringBuilder;
    }
}
