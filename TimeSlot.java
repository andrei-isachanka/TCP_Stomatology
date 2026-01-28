package dentist;

import java.io.Serializable;
import java.time.LocalDateTime;

public class TimeSlot implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private LocalDateTime dateTime;
    private boolean available;
    private String patientName;
    private String patientPhone;
    private String complaints;
    
    public TimeSlot(LocalDateTime dateTime) {
        this.dateTime = dateTime;
        this.available = true;
        this.patientName = null;
        this.patientPhone = null;
        this.complaints = null;
    }
    
    public LocalDateTime getDateTime() {
        return dateTime;
    }
    
    public boolean isAvailable() {
        return available;
    }
    
    public void book(String patientName, String patientPhone, String complaints) {
        this.available = false;
        this.patientName = patientName;
        this.patientPhone = patientPhone;
        this.complaints = complaints;
    }
    
    public void cancel() {
        this.available = true;
        this.patientName = null;
        this.patientPhone = null;
        this.complaints = null;
    }
    
    public String getPatientName() {
        return patientName;
    }
    
    public String getPatientPhone() {
        return patientPhone;
    }
    
    public String getComplaints() {
        return complaints;
    }
    
    @Override
    public String toString() {
        if (available) {
            return String.format("%02d.%02d.%04d %02d:%02d - свободно",
                dateTime.getDayOfMonth(),
                dateTime.getMonthValue(),
                dateTime.getYear(),
                dateTime.getHour(),
                dateTime.getMinute());
        } else {
            return String.format("%02d.%02d.%04d %02d:%02d - занято (%s)",
                dateTime.getDayOfMonth(),
                dateTime.getMonthValue(),
                dateTime.getYear(),
                dateTime.getHour(),
                dateTime.getMinute(),
                patientName);
        }
    }
}
