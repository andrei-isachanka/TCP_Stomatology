package dentist;

import java.io.Serializable;

public class Message implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private byte id;
    
    public byte getID() {
        return id;
    }
    
    protected Message(byte id) {
        assert(Protocol.validID(id) == true);
        this.id = id;
    }
}
