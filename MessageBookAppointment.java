package dentist;

import java.io.Serializable;
import java.time.LocalDateTime;

public class MessageBookAppointment extends Message implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private LocalDateTime dateTime;
    private String patientName;
    private String patientPhone;
    private String complaints;
    
    public MessageBookAppointment(LocalDateTime dateTime, String patientName, String patientPhone, String complaints) {
        super(Protocol.CMD_BOOK_APPOINTMENT);
        this.dateTime = dateTime;
        this.patientName = patientName;
        this.patientPhone = patientPhone;
        this.complaints = complaints;
    }
    
    public LocalDateTime getDateTime() {
        return dateTime;
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
}
