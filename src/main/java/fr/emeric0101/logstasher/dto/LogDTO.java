package fr.emeric0101.logstasher.dto;

import lombok.Data;

import java.util.List;

@Data
public class LogDTO {
    private List<String> lines;

    public LogDTO(List<String> lines) {
        this.lines = lines;
    }
}
