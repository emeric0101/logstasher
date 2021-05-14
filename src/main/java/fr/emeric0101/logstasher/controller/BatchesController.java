package fr.emeric0101.logstasher.controller;

import fr.emeric0101.logstasher.entity.Batch;
import fr.emeric0101.logstasher.entity.ExecutorEnum;
import fr.emeric0101.logstasher.service.batch.BatchExecutionService;
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

    @PostMapping()
    @CrossOrigin(origins="*")
    public void save(@RequestBody Batch batch) {
        service.save(batch);
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




    @GetMapping("/stop/{executor}")
    @CrossOrigin(origins = "*")
    public void stop(@PathVariable("executor")ExecutorEnum executorEnum) {
        batchExecutionService.stop(executorEnum, false);
    }


}
