package fr.emeric0101.logstasher.repository;

import fr.emeric0101.logstasher.entity.Pipeline;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface PipelineRepository extends ElasticsearchRepository<Pipeline, String>{
}
