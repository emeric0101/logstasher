package fr.emeric0101.logstasher.repository;


import fr.emeric0101.logstasher.entity.ExecutionArchive;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

public interface ExecutionArchiveRepository extends ElasticsearchRepository<ExecutionArchive, String> {

    @Query("{        \"range\" : { " +
            "            \"startTime\" : { " +
            "                \"gte\" : ?0, " +
            "                \"lt\" :  ?1 " +
            "            } " +
            "        }}")

    List<ExecutionArchive> findInterval(long start, long end, PageRequest page);
}
