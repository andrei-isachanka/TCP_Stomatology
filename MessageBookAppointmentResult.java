package dentist;

import java.io.Serializable;

public class MessageBookAppointmentResult extends MessageResult implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String confirmationMessage;
    
    public MessageBookAppointmentResult(int code, String message) {
        super(Protocol.CMD_BOOK_APPOINTMENT, code, message);
        this.confirmationMessage = message;
    }
    
    public String getConfirmationMessage() {
        return confirmationMessage;
    }
}
