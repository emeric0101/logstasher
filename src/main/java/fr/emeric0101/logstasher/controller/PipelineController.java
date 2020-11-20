package fr.emeric0101.logstasher.controller;

import fr.emeric0101.logstasher.entity.Pipeline;
import fr.emeric0101.logstasher.service.PipelineExecutionService;
import fr.emeric0101.logstasher.service.PipelineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


@RestController()
@RequestMapping("/pipeline")
public class PipelineController {

    @Autowired
    PipelineService pipelineService;

    @Autowired
    PipelineExecutionService pipelineExecutionService;

    @RequestMapping()
    @CrossOrigin(origins="*")
    public Iterable<Pipeline> list() {
        return pipelineService.findAll();
    }

    @PostMapping()
    @CrossOrigin(origins="*")
    public void save(@RequestBody Pipeline pipeline) {
        pipelineService.save(pipeline);
    }

    @GetMapping("/stop")
    @CrossOrigin(origins="*")
    public void stop() {
        pipelineExecutionService.stop();
    }

    @GetMapping("/start")
    @CrossOrigin(origins="*")
    public void start() {
        pipelineExecutionService.restart();
    }

    @DeleteMapping("/{id}")
    @CrossOrigin(origins = "*")
    public void delete(@PathVariable("id") final String id) {
        this.pipelineService.delete(id);

    }

}
