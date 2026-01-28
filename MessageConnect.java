package dentist;

import java.io.Serializable;

public class MessageConnect extends Message implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String clientName;
    
    public MessageConnect(String clientName) {
        super(Protocol.CMD_CONNECT);
        this.clientName = clientName;
    }
    
    public String getClientName() {
        return clientName;
    }
}
