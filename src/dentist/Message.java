package dentist;

import java.io.Serializable;

public class Message implements Serializable {

    private static final long serialVersionUID = 1L;

    private final byte id;

    protected Message(byte id) {
        if (!Protocol.validID(id)) {
            throw new IllegalArgumentException(
                    "Неизвестный идентификатор сообщения: " + id
            );
        }

        this.id = id;
    }

    public byte getID() {
        return id;
    }
}