package dentist;

public final class Protocol {

    public static final byte CMD_CONNECT = 1;
    public static final byte CMD_DISCONNECT = 2;
    public static final byte CMD_GET_SCHEDULE = 3;
    public static final byte CMD_BOOK_APPOINTMENT = 4;

    public static final int RESULT_CODE_OK = 0;
    public static final int RESULT_CODE_ERROR = -1;
    public static final int RESULT_CODE_SLOT_TAKEN = -2;

    public static final int PORT = 8071;

    private Protocol() {
    }

    public static boolean validID(byte id) {
        return id >= CMD_CONNECT && id <= CMD_BOOK_APPOINTMENT;
    }
}