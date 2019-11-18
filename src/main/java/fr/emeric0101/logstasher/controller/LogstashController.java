package fr.emeric0101.logstasher.controller;

import fr.emeric0101.logstasher.dto.LogstashRunning;
import fr.emeric0101.logstasher.service.ExecutionService;
import fr.emeric0101.logstasher.service.LogstashService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController()
@RequestMapping("/logstash")
public class LogstashController {
    @Autowired
    ExecutionService executionService;

    @GetMapping("/restart")
    @CrossOrigin(origins = "*")
    public void restart() {
        executionService.restart();
    }

    @GetMapping("/running")
    @CrossOrigin(origins = "*")
    public LogstashRunning getRunning() {
        return executionService.getRunning();
    }
    @GetMapping("/stop")
    @CrossOrigin(origins = "*")
    public void stop() {
        executionService.stopLogstash(false);
    }

    @GetMapping("/init")
    @CrossOrigin(origins = "*")
    public void init() {
        executionService.init();
    }
}
