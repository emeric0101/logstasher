package fr.emeric0101.logstasher.exception;

public class LogstashNotFoundException extends RuntimeException {
    public LogstashNotFoundException(String message) {
        super(message);
    }
}
