package dentist;

import java.io.Serializable;

public class MessageGetSchedule extends Message implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public MessageGetSchedule() {
        super(Protocol.CMD_GET_SCHEDULE);
    }
}
