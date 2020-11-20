package fr.emeric0101.logstasher.controller;

import fr.emeric0101.logstasher.dto.LogstashRunning;
import fr.emeric0101.logstasher.service.LogstashService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController()
@RequestMapping("/logstash")
public class LogstashController {
    @Autowired
    LogstashService logstashService;


    @GetMapping("/running/{instanceName}")
    @CrossOrigin(origins = "*")
    public LogstashRunning getRunning(@PathVariable("instanceName") final String instanceName) {
        return logstashService.getRunning(instanceName);
    }
    @GetMapping("/stop/{instanceName}")
    @CrossOrigin(origins = "*")
    public void stop(@PathVariable("instanceName") final String instanceName) {
        logstashService.stop(instanceName);
    }

    @GetMapping("/all")
    @CrossOrigin(origins = "*")
    public List<LogstashRunning> all() {
        return logstashService.getRunningAll();
    }




}
