package fr.emeric0101.logstasher.service;

import fr.emeric0101.logstasher.configuration.LogstashProperties;
import fr.emeric0101.logstasher.entity.Pipeline;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

@Service
public class GeneratorService {
    static final String directory = "logstash-pipelines";
    @Autowired
    PipelineService pipelineService;

    @Autowired
    LogstashProperties logstashProperties;


    public void generatePipelines() {
        Iterable<Pipeline> pipelines = pipelineService.findAll();
        String config = "";

        // First clean the directory
        File file = new File(directory);
        try {
            FileUtils.deleteDirectory(file);
        }
        catch (FileNotFoundException e) {

        } catch (IOException e) {
            e.printStackTrace();
        }
        // Create the directory and pipelines config
        try {
            FileUtils.forceMkdir(file);
            for (Pipeline pipeline : pipelines) {
                if (!pipeline.isActivated()) {continue;}
                File pipelineFile = new File(directory + "/" + pipeline.getId() + ".conf");
                FileUtils.writeStringToFile(pipelineFile, pipeline.getContent(), Charset.defaultCharset());
                config += "- pipeline.id: " + pipeline.getId() + "\n";
                config += "  path.config: \"" + pipelineFile.getAbsolutePath().replace("\\", "\\\\") + "\"\n\n";
            }
            // Create the logstash pipeline configuration
            File configFile = new File(logstashProperties.getPath() + "\\config\\pipelines.yml");
            FileUtils.writeStringToFile(configFile, config, Charset.defaultCharset());

        } catch (IOException e) {
            e.printStackTrace();
        }


    }


    public void generateBatches() {
    }
}
