package fr.emeric0101.logstasher;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LogstasherApplication {

    public static void main(String[] args) {
        SpringApplication.run(LogstasherApplication.class, args);
    }
}
