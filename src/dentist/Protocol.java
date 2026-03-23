package dentist;

interface CMD {
    byte CMD_CONNECT = 1;
    byte CMD_DISCONNECT = 2;
    byte CMD_GET_SCHEDULE = 3;
    byte CMD_BOOK_APPOINTMENT = 4;
    byte CMD_CANCEL_APPOINTMENT = 5;
}

interface RESULT {
    int RESULT_CODE_OK = 0;
    int RESULT_CODE_ERROR = -1;
    int RESULT_CODE_SLOT_TAKEN = -2;
}

interface PORT {
    int PORT = 8071;
}

public class Protocol implements CMD, RESULT, PORT {
    private static final byte CMD_MIN = CMD_CONNECT;
    private static final byte CMD_MAX = CMD_CANCEL_APPOINTMENT;

    public static boolean validID(byte id) {
        return id >= CMD_MIN && id <= CMD_MAX;
    }
}
