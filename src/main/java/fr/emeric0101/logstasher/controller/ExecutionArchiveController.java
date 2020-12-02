package fr.emeric0101.logstasher.controller;

import fr.emeric0101.logstasher.entity.ExecutionArchive;
import fr.emeric0101.logstasher.service.ExecutionArchiveService;
import org.elasticsearch.action.search.SearchPhaseExecutionException;
import org.elasticsearch.client.transport.NoNodeAvailableException;
import org.elasticsearch.index.query.QueryShardException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController()
@RequestMapping("/executionArchive")
public class ExecutionArchiveController {


    @Autowired
    ExecutionArchiveService batchExecutionArchiveService;

    @RequestMapping("/archive")
    @CrossOrigin(origins="*")
    public List<ExecutionArchive> archive() {
        try {
            return batchExecutionArchiveService.findLastWeek();
        } catch (SearchPhaseExecutionException e) {
            // init ES
            batchExecutionArchiveService.initArchive();
            return batchExecutionArchiveService.findLastWeek();
        }
    }

}
