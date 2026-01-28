package dentist;

import java.io.Serializable;

public class MessageResult extends Message implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private int code;
    private String errorMessage;
    
    protected MessageResult(byte id, int code) {
        super(id);
        this.code = code;
        this.errorMessage = "";
    }
    
    protected MessageResult(byte id, int code, String errorMessage) {
        super(id);
        this.code = code;
        this.errorMessage = (errorMessage == null ? "" : errorMessage);
    }
    
    public boolean Error() {
        return code != Protocol.RESULT_CODE_OK;
    }
    
    public int getCode() {
        return code;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
}
