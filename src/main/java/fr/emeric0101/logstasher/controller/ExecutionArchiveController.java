package fr.emeric0101.logstasher.controller;

import fr.emeric0101.logstasher.dto.LogDTO;
import fr.emeric0101.logstasher.entity.ExecutionArchive;
import fr.emeric0101.logstasher.service.ExecutionArchiveService;
import org.elasticsearch.action.search.SearchPhaseExecutionException;
import org.elasticsearch.client.transport.NoNodeAvailableException;
import org.elasticsearch.index.query.QueryShardException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController()
@RequestMapping("/executionArchive")
public class ExecutionArchiveController {


    @Autowired
    ExecutionArchiveService batchExecutionArchiveService;

    @RequestMapping("/archive")
    @CrossOrigin(origins="*")
    public List<ExecutionArchive> archive() {
            return batchExecutionArchiveService.findLastWeek();
    }

    @GetMapping("/{id}/log")
    @CrossOrigin(origins = "*")
    public LogDTO getLog(@PathVariable("id") final String executionArchiveId) {
        return new LogDTO(batchExecutionArchiveService.getLog(executionArchiveId));
    }


}
