package fr.emeric0101.logstasher.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;

import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Document(indexName = "logstasher_archive", type = "doc")
public class ExecutionArchive {
    @Id
    private String id;
    private Batch batch;
    private List<Pipeline> pipeline;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private Date startTime;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private Date endTime;
    private String state;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private Date expectedStart;
    private String type;
    private String logPath;


    /**
     *
     * @param date
     * @return
     */
    private static Calendar dateToCalendar(Date date) {
        Calendar obj = Calendar.getInstance();
        if (date == null) {
            return null;
        }
        obj.setTime(date);
        return obj;
    }

    /**
     *
     * @param calendar
     * @return
     */
    private static Date calendarToDate(Calendar calendar) {
        if (calendar == null) {
            return null;
        }
        return calendar.getTime();
    }



    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Batch getBatch() {
        return batch;
    }

    public void setBatch(Batch batch) {
        this.batch = batch;
    }


    public Calendar getStartTime() {
        return dateToCalendar(startTime);
    }

    public void setStartTime(Calendar startTime) {
        this.startTime = calendarToDate(startTime);
    }

    public Calendar getEndTime() {
        return dateToCalendar(endTime);
    }

    public void setEndTime(Calendar endTime) {
        this.endTime = calendarToDate(endTime);
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public List<Pipeline> getPipeline() {
        return pipeline;
    }

    public void setPipeline(List<Pipeline> pipeline) {
        this.pipeline = pipeline;
    }

    public Calendar getExpectedStart() {
        return dateToCalendar(expectedStart);
    }

    public void setExpectedStart(Calendar expectedStart) {
        this.expectedStart = calendarToDate(expectedStart);
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getLogPath() {
        return logPath;
    }

    public void setLogPath(String logPath) {
        this.logPath = logPath;
    }
}
