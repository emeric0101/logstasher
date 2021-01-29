package fr.emeric0101.logstasher.repository;

import fr.emeric0101.logstasher.entity.Pipeline;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

public interface PipelineRepository extends ElasticsearchRepository<Pipeline, String>{
}
