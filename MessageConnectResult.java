package dentist;

import java.io.Serializable;

public class MessageConnectResult extends MessageResult implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public MessageConnectResult(int code) {
        super(Protocol.CMD_CONNECT, code);
    }
    
    public MessageConnectResult(int code, String errorMessage) {
        super(Protocol.CMD_CONNECT, code, errorMessage);
    }
}
