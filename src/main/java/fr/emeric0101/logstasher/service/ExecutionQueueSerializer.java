package fr.emeric0101.logstasher.service;

import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

@Service
public class ExecutionQueueSerializer {
    public String getDate() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-M-yyyy HH-mm-ss");
        return simpleDateFormat.format(new Date());
    }

    /**
     * Return path of the log file
     * @param id
     * @param data
     * @return
     */
    public String saveLog(String id, String data) {
        String date = getDate();
        String path = "log/" + date.substring(0, 12) + "/" + id + ".log";
        // truncate date to avoir minutes and seconds
        File pipelineFile = new File(path);
        if (!pipelineFile.exists()) {
            String buffer = "Logstasher - (Emeric BAVEUX)";
            buffer += "\n";
            buffer += "\n";
            buffer += "Batch : " + id;
            buffer += "\n";
            buffer += data;
            buffer += "\n";


            try {
                FileUtils.writeStringToFile(pipelineFile, buffer, Charset.defaultCharset());
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try (FileWriter fr = new FileWriter(pipelineFile, true)){
                fr.write(data + "\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return path;

    }
}
