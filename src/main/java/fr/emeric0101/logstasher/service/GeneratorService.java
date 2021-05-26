package fr.emeric0101.logstasher.service;

import fr.emeric0101.logstasher.configuration.LogstashProperties;
import fr.emeric0101.logstasher.entity.Batch;
import fr.emeric0101.logstasher.entity.Pipeline;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;

@Service
public class GeneratorService {
    static final String LOGSTASH_PIPELINES = "logstash-pipelines";
    static final String LOGSTASH_BATCHES = "logstash-batches";
    @Autowired
    PipelineService pipelineService;

    @Autowired
    LogstashProperties logstashProperties;


    public void generatePipelines() {
        Iterable<Pipeline> pipelines = pipelineService.findAll();
        String config = "";

        // First clean the directory
        File file = new File(LOGSTASH_PIPELINES);
        try {
            FileUtils.deleteDirectory(file);
        } catch (FileNotFoundException e) {

        } catch (IOException e) {
            e.printStackTrace();
        }
        // Create the directory and pipelines config
        try {
            FileUtils.forceMkdir(file);
            for (Pipeline pipeline : pipelines) {
                if (!pipeline.isActivated()) {
                    continue;
                }
                File pipelineFile = new File(LOGSTASH_PIPELINES + "/" + pipeline.getId() + ".conf");
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


    public String generateBatch(Batch batch) throws IOException {
        String config = batch.getContent();
        String path = LOGSTASH_BATCHES + File.separator + batch.getId() + ".conf";
        // Create the directory and pipelines config
        FileUtils.forceMkdir(new File(LOGSTASH_BATCHES));
        // Create the logstash pipeline configuration
        File configFile = new File(path);
        FileUtils.writeStringToFile(configFile, config, Charset.defaultCharset());
        return path;
    }
}
