package fr.emeric0101.logstasher.service;

import fr.emeric0101.logstasher.entity.Pipeline;
import fr.emeric0101.logstasher.repository.PipelineRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PipelineService {
    @Autowired
    PipelineRepository pipelineRepository;

    @Autowired
    GeneratorService generatorService;

    public Iterable<Pipeline> findAll() {
        return pipelineRepository.findAll();
    }

    public void save(Pipeline p) {
        this.pipelineRepository.save(p);
        this.generatorService.generatePipelines();

    }

    public void delete(String id) {
        this.pipelineRepository.deleteById(id);
        this.generatorService.generatePipelines();

    }
}
