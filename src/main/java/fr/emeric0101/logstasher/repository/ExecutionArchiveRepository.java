package fr.emeric0101.logstasher.repository;


import fr.emeric0101.logstasher.entity.ExecutionArchive;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.Date;
import java.util.List;

public interface ExecutionArchiveRepository extends ElasticsearchRepository<ExecutionArchive, String> {

    @Query("{        \"range\" : { " +
            "            \"startTime\" : { " +
            "                \"gte\" : \"?0\", " +
            "                \"lt\" :  \"?1\" " +
            "            } " +
            "        }}")
    List<ExecutionArchive> findIntervalPage(String start, String end, PageRequest page);

    @Query("{" +
            "    \"bool\": {" +
            "      \"must\": [" +
            "        {" +
            "          \"range\": {" +
            "            \"startTime\": {" +
            "              \"gte\": \"?0\"," +
            "              \"lt\": \"?1\"" +
            "            }" +
            "          }" +
            "        }," +
            "        {" +
            "            \"match\" : { " +
            "                \"batch.id\" : \"?2\" " +
            "            } " +
            "            } " +
            "      ]" +
            "    }" +
            "  }")
    List<ExecutionArchive> findInterval(String start, String end, String batchId);

}
