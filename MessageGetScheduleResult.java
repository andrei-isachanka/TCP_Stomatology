package dentist;

import java.io.Serializable;
import java.util.ArrayList;

public class MessageGetScheduleResult extends MessageResult implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private ArrayList<TimeSlot> availableSlots;
    
    public MessageGetScheduleResult(int code, ArrayList<TimeSlot> availableSlots) {
        super(Protocol.CMD_GET_SCHEDULE, code);
        this.availableSlots = availableSlots;
    }
    
    public ArrayList<TimeSlot> getAvailableSlots() {
        return availableSlots;
    }
}
