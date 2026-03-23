package dentist;

import java.io.Serializable;

public class MessageDisconnectResult extends MessageResult implements Serializable {
    private static final long serialVersionUID = 1L;

    public MessageDisconnectResult(int code) {
        super(Protocol.CMD_DISCONNECT, code);
    }

    public MessageDisconnectResult(int code, String errorMessage) {
        super(Protocol.CMD_DISCONNECT, code, errorMessage);
    }
}
