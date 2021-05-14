package fr.emeric0101.logstasher.controller;

import fr.emeric0101.logstasher.dto.LogstashRunning;
import fr.emeric0101.logstasher.service.ExecutionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController()
@RequestMapping("/logstash")
public class LogstashController {
    @Autowired
    ExecutionService executionService;



    @GetMapping("/all")
    @CrossOrigin(origins = "*")
    public List<LogstashRunning> all() {
        return executionService.getRunningAll();
    }




}
