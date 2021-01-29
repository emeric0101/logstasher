package fr.emeric0101.logstasher.configuration;


import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "logstasher")
public class LogstasherProperties {
    private String index_batch;
    private String index_pipeline;
    private boolean autostart_pipeline;


    public String getIndex_batch() {
        return index_batch;
    }

    public void setIndex_batch(String index_batch) {
        this.index_batch = index_batch;
    }

    public String getIndex_pipeline() {
        return index_pipeline;
    }

    public void setIndex_pipeline(String index_pipeline) {
        this.index_pipeline = index_pipeline;
    }

    public boolean isAutostart_pipeline() {
        return autostart_pipeline;
    }

    public void setAutostart_pipeline(boolean autostart_pipeline) {
        this.autostart_pipeline = autostart_pipeline;
    }
}
