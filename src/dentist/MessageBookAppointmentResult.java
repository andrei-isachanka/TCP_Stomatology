package dentist;

public class MessageBookAppointmentResult extends MessageResult {

    private static final long serialVersionUID = 1L;

    private final String confirmationMessage;

    public MessageBookAppointmentResult(int code) {
        this(code, "");
    }

    public MessageBookAppointmentResult(int code, String message) {
        super(
                Protocol.CMD_BOOK_APPOINTMENT,
                code,
                code == Protocol.RESULT_CODE_OK ? "" : message
        );

        this.confirmationMessage =
                code == Protocol.RESULT_CODE_OK && message != null
                        ? message
                        : "";
    }

    public String getConfirmationMessage() {
        return confirmationMessage;
    }
}