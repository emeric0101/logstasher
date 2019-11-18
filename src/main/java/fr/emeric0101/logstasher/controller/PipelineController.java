package fr.emeric0101.logstasher.controller;

import fr.emeric0101.logstasher.entity.Pipeline;
import fr.emeric0101.logstasher.repository.PipelineRepository;
import fr.emeric0101.logstasher.service.LogstashService;
import fr.emeric0101.logstasher.service.PipelineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController()
@RequestMapping("/pipeline")
public class PipelineController {

    @Autowired
    PipelineService pipelineService;

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

    @DeleteMapping("/{id}")
    @CrossOrigin(origins = "*")
    public void delete(@PathVariable("id") final String id) {
        this.pipelineService.delete(id);

    }

}
