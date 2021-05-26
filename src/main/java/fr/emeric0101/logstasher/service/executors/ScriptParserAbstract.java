package fr.emeric0101.logstasher.service.executors;

import java.io.*;
import java.util.LinkedList;
import java.util.List;

public abstract class ScriptParserAbstract {

    public List<String> parse(File file)
            throws IOException {
        InputStream inputStream = (new FileInputStream(file));

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
