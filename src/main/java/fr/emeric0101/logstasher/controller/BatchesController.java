package fr.emeric0101.logstasher.controller;

import fr.emeric0101.logstasher.dto.LogstashRunning;
import fr.emeric0101.logstasher.entity.Batch;
import fr.emeric0101.logstasher.entity.BatchArchive;
import fr.emeric0101.logstasher.service.ArchiveService;
import fr.emeric0101.logstasher.service.BatchExecutionService;
import fr.emeric0101.logstasher.service.BatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController()
@RequestMapping("/batches")
public class BatchesController {

    @Autowired
    BatchService service;

    @Autowired
    ArchiveService batchArchiveService;

    @Autowired
    BatchExecutionService batchExecutionService;

    @GetMapping("/init")
    @CrossOrigin(origins = "*")
    public void init() {
        batchExecutionService.init();
    }


    @RequestMapping()
    @CrossOrigin(origins="*")
    public List<Batch> list() {
        return service.findAll();
    }

    @RequestMapping("/archive")
    @CrossOrigin(origins="*")
    public List<BatchArchive> archive() {
        return batchArchiveService.findLastWeek();
    }

    @PostMapping()
    @CrossOrigin(origins="*")
    public void save(@RequestBody Batch pipeline) {
        service.save(pipeline);
    }

    @DeleteMapping("/{id}")
    @CrossOrigin(origins = "*")
    public void delete(@PathVariable("id") final String id) {
        this.service.delete(id);

    }

    @RequestMapping("/start/{id}")
    @CrossOrigin(origins="*")
    public void start(@PathVariable("id") final String id) {
        service.start(id);
    }

    @RequestMapping("/restartBatches")
    @CrossOrigin(origins="*")
    public void restartBatches() {
        service.restartBatches();
    }



    @GetMapping("/stop/{instanceName}")
    @CrossOrigin(origins = "*")
    public void stop(@PathVariable("instanceName") final String instanceName) {
        batchExecutionService.stopLogstash(false);
    }

    @GetMapping("/clear")
    @CrossOrigin(origins = "*")
    public void clear() {
        batchExecutionService.clear();
    }

}
