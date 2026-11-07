package dentist;

import java.util.ArrayList;

public class MessageGetScheduleResult extends MessageResult {

    private static final long serialVersionUID = 1L;

    private final ArrayList<TimeSlot> availableSlots;

    public MessageGetScheduleResult(int code) {
        super(Protocol.CMD_GET_SCHEDULE, code);
        this.availableSlots = new ArrayList<>();
    }

    public MessageGetScheduleResult(
            int code,
            ArrayList<TimeSlot> availableSlots
    ) {
        super(Protocol.CMD_GET_SCHEDULE, code);

        this.availableSlots = availableSlots == null
                ? new ArrayList<>()
                : new ArrayList<>(availableSlots);
    }

    public MessageGetScheduleResult(int code, String errorMessage) {
        super(Protocol.CMD_GET_SCHEDULE, code, errorMessage);
        this.availableSlots = new ArrayList<>();
    }

    public ArrayList<TimeSlot> getAvailableSlots() {
        return new ArrayList<>(availableSlots);
    }
}