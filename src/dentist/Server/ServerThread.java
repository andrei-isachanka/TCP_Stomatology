package dentist.Server;

import dentist.Message;
import dentist.MessageBookAppointment;
import dentist.MessageBookAppointmentResult;
import dentist.MessageConnect;
import dentist.MessageConnectResult;
import dentist.MessageDisconnect;
import dentist.MessageGetSchedule;
import dentist.MessageGetScheduleResult;
import dentist.Protocol;
import dentist.TimeSlot;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class ServerThread extends Thread {

    private static final int HANDSHAKE_TIMEOUT_MS = 10_000;

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final Socket socket;
    private final AtomicBoolean disconnected = new AtomicBoolean();

    private ObjectInputStream input;
    private ObjectOutputStream output;
    private volatile String clientName;

    public ServerThread(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            socket.setSoTimeout(HANDSHAKE_TIMEOUT_MS);

            output = new ObjectOutputStream(socket.getOutputStream());
            output.flush();

            input = new ObjectInputStream(socket.getInputStream());

            if (!processConnect()) {
                return;
            }

            socket.setSoTimeout(0);

            while (!isDisconnected() && !ServerMain.getStopFlag()) {
                Message message = readMessage();

                if (!processMessage(message)) {
                    break;
                }
            }
        } catch (EOFException e) {
            System.out.println(
                    "[DISCONNECT] Соединение закрыто клиентом: " + clientLabel()
            );
        } catch (SocketException e) {
            if (!isDisconnected() && !ServerMain.getStopFlag()) {
                System.err.println(
                        "Ошибка соединения с " + clientLabel()
                                + ": " + e.getMessage()
                );
            }
        } catch (IOException | ClassNotFoundException e) {
            if (!isDisconnected() && !ServerMain.getStopFlag()) {
                System.err.println(
                        "Ошибка сеанса " + clientLabel()
                                + ": " + e.getMessage()
                );
            }
        } finally {
            disconnect();
        }
    }

    private boolean processConnect()
            throws IOException, ClassNotFoundException {

        Message message = readMessage();

        if (!(message instanceof MessageConnect)
                || message.getID() != Protocol.CMD_CONNECT) {
            send(new MessageConnectResult(
                    Protocol.RESULT_CODE_ERROR,
                    "Ожидалось сообщение подключения"
            ));
            return false;
        }

        MessageConnect connectMessage = (MessageConnect) message;
        String requestedName = connectMessage.getClientName();

        if (isBlank(requestedName)) {
            send(new MessageConnectResult(
                    Protocol.RESULT_CODE_ERROR,
                    "Имя клиента не должно быть пустым"
            ));
            return false;
        }

        clientName = requestedName.trim();

        if (!ServerMain.addClient(clientName, this)) {
            send(new MessageConnectResult(
                    Protocol.RESULT_CODE_ERROR,
                    "Имя уже занято или сервер останавливается"
            ));
            return false;
        }

        send(new MessageConnectResult(Protocol.RESULT_CODE_OK));

        System.out.println(
                "[CONNECT] Клиент '" + clientName + "' подключился"
        );

        return true;
    }

    private Message readMessage()
            throws IOException, ClassNotFoundException {

        Object object = input.readObject();

        if (!(object instanceof Message)) {
            throw new IOException("Получен объект неизвестного типа");
        }

        return (Message) object;
    }

    private boolean processMessage(Message message) throws IOException {
        switch (message.getID()) {
            case Protocol.CMD_DISCONNECT:
                if (!(message instanceof MessageDisconnect)) {
                    throw new IOException("Неверный тип команды отключения");
                }

                return false;

            case Protocol.CMD_GET_SCHEDULE:
                if (!(message instanceof MessageGetSchedule)) {
                    throw new IOException("Неверный тип запроса расписания");
                }

                processGetSchedule();
                return true;

            case Protocol.CMD_BOOK_APPOINTMENT:
                if (!(message instanceof MessageBookAppointment)) {
                    throw new IOException("Неверный тип запроса бронирования");
                }

                processBookAppointment((MessageBookAppointment) message);
                return true;

            default:
                throw new IOException(
                        "Неизвестная команда: " + message.getID()
                );
        }
    }

    private void processGetSchedule() throws IOException {
        ArrayList<TimeSlot> available = ServerMain.getAvailableSlots();

        send(new MessageGetScheduleResult(
                Protocol.RESULT_CODE_OK,
                available
        ));

        System.out.println(
                "[SCHEDULE] Клиенту '" + clientName
                        + "' отправлено слотов: " + available.size()
        );
    }

    private void processBookAppointment(MessageBookAppointment message)
            throws IOException {

        LocalDateTime dateTime = message.getDateTime();

        if (dateTime == null
                || isBlank(message.getPatientName())
                || isBlank(message.getPatientPhone())
                || isBlank(message.getComplaints())) {

            send(new MessageBookAppointmentResult(
                    Protocol.RESULT_CODE_ERROR,
                    "Дата, ФИО, телефон и причина обращения обязательны"
            ));
            return;
        }

        if (!dateTime.isAfter(LocalDateTime.now())) {
            send(new MessageBookAppointmentResult(
                    Protocol.RESULT_CODE_ERROR,
                    "Нельзя записаться на прошедшее время"
            ));
            return;
        }

        String patientName = message.getPatientName().trim();
        String patientPhone = message.getPatientPhone().trim();
        String complaints = message.getComplaints().trim();

        boolean booked = ServerMain.bookSlot(
                dateTime,
                patientName,
                patientPhone,
                complaints
        );

        if (!booked) {
            send(new MessageBookAppointmentResult(
                    Protocol.RESULT_CODE_SLOT_TAKEN,
                    "Выбранное время больше недоступно для записи"
            ));
            return;
        }

        String confirmation = String.format(
                "Вы записаны на %s.%nФИО: %s%nТелефон: %s",
                dateTime.format(FORMATTER),
                patientName,
                patientPhone
        );

        send(new MessageBookAppointmentResult(
                Protocol.RESULT_CODE_OK,
                confirmation
        ));
    }

    private void send(Message message) throws IOException {
        output.reset();
        output.writeObject(message);
        output.flush();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String clientLabel() {
        return clientName == null
                ? socket.getRemoteSocketAddress().toString()
                : clientName;
    }

    public boolean isDisconnected() {
        return disconnected.get();
    }

    public void disconnect() {
        if (!disconnected.compareAndSet(false, true)) {
            return;
        }

        ServerMain.removeClient(clientName, this);

        try {
            socket.close();
        } catch (IOException e) {
            System.err.println(
                    "Ошибка закрытия соединения: " + e.getMessage()
            );
        }
    }
}