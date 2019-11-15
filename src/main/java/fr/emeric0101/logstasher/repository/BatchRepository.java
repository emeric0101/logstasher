package fr.emeric0101.logstasher.repository;

import fr.emeric0101.logstasher.entity.Batch;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

public interface BatchRepository extends ElasticsearchRepository<Batch, String> {

    @Query("{\"match\" : { \"activated\": true} }")
    Iterable<Batch> findAllActive(PageRequest page);

}
