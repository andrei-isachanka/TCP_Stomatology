package dentist.Client;

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
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

public class ClientMain {

    private static final int CONNECT_TIMEOUT_MS = 5_000;
    private static final int READ_TIMEOUT_MS = 15_000;

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy (EEE) HH:mm",
                    Locale.forLanguageTag("ru"));

    public static void main(String[] args) {
        if (args.length < 1 || args.length > 2) {
            System.err.println(
                    "Использование: java dentist.Client.ClientMain <ваше_имя> [host]"
            );
            return;
        }

        String clientName = args[0].trim();

        if (clientName.isEmpty()) {
            System.err.println("Имя клиента не должно быть пустым.");
            return;
        }

        String host = args.length == 2 ? args[1] : "localhost";

        System.out.println("Система записи к стоматологу");
        System.out.println("Подключение к серверу...");

        try (Socket socket = new Socket()) {
            socket.connect(
                    new InetSocketAddress(host, Protocol.PORT),
                    CONNECT_TIMEOUT_MS
            );
            socket.setSoTimeout(READ_TIMEOUT_MS);
            session(socket, clientName);
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Ошибка соединения: " + e.getMessage());
        } finally {
            System.out.println("\nДо свидания!");
        }
    }

    private static void session(Socket socket, String clientName)
            throws IOException, ClassNotFoundException {

        try (ObjectOutputStream output =
                     new ObjectOutputStream(socket.getOutputStream())) {

            output.flush();

            try (ObjectInputStream input =
                         new ObjectInputStream(socket.getInputStream())) {

                send(output, new MessageConnect(clientName));

                MessageConnectResult connectionResult =
                        readResponse(input, MessageConnectResult.class);

                if (connectionResult.Error()) {
                    System.err.println(
                            "Не удалось подключиться: "
                                    + connectionResult.getErrorMessage()
                    );
                    return;
                }

                System.out.println("Подключение успешно!");
                System.out.println("Добро пожаловать, " + clientName + "!");

                Scanner console = new Scanner(System.in);
                showMenu();

                while (true) {
                    System.out.print("\nВыберите действие (1-3): ");

                    if (!console.hasNextLine()) {
                        send(output, new MessageDisconnect());
                        return;
                    }

                    String choice = console.nextLine().trim();

                    switch (choice) {
                        case "1":
                            viewSchedule(input, output);
                            break;
                        case "2":
                            bookAppointment(input, output, console);
                            break;
                        case "3":
                            send(output, new MessageDisconnect());
                            System.out.println("Выход из системы...");
                            return;
                        default:
                            System.out.println(
                                    "Неверный выбор. Попробуйте снова."
                            );
                    }
                }
            }
        }
    }

    private static void showMenu() {
        System.out.println("\nМЕНЮ");
        System.out.println("1. Просмотреть свободное время");
        System.out.println("2. Записаться на прием");
        System.out.println("3. Выход");
        System.out.println("--------------------------------");
    }

    private static void viewSchedule(
            ObjectInputStream input,
            ObjectOutputStream output
    ) throws IOException, ClassNotFoundException {

        MessageGetScheduleResult result = requestSchedule(input, output);

        if (result.Error()) {
            System.err.println(
                    "Ошибка получения расписания: " + result.getErrorMessage()
            );
            return;
        }

        List<TimeSlot> slots = result.getAvailableSlots();

        if (slots.isEmpty()) {
            System.out.println("Нет свободных слотов на ближайшую неделю.");
            return;
        }

        System.out.println("\nСвободное время на неделю:");
        printSlots(slots);
    }

    private static void bookAppointment(
            ObjectInputStream input,
            ObjectOutputStream output,
            Scanner console
    ) throws IOException, ClassNotFoundException {

        MessageGetScheduleResult scheduleResult =
                requestSchedule(input, output);

        if (scheduleResult.Error()) {
            System.err.println(
                    "Ошибка получения расписания: "
                            + scheduleResult.getErrorMessage()
            );
            return;
        }

        List<TimeSlot> slots = scheduleResult.getAvailableSlots();

        if (slots.isEmpty()) {
            System.out.println("Нет свободных слотов для записи.");
            return;
        }

        System.out.println("\nДоступное время:");
        printSlots(slots);

        System.out.print("\nВыберите номер слота (или 0 для отмены): ");
        String choice = readLine(console);

        int slotNumber;

        try {
            slotNumber = Integer.parseInt(choice);
        } catch (NumberFormatException e) {
            System.out.println("Введите целое число.");
            return;
        }

        if (slotNumber == 0) {
            System.out.println("Отмена записи.");
            return;
        }

        if (slotNumber < 1 || slotNumber > slots.size()) {
            System.out.println("Неверный номер слота.");
            return;
        }

        System.out.print("Введите ФИО: ");
        String fullName = readLine(console);

        System.out.print("Введите телефон: ");
        String phone = readLine(console);

        System.out.print("Введите жалобы/причину обращения: ");
        String complaints = readLine(console);

        if (fullName.isEmpty() || phone.isEmpty() || complaints.isEmpty()) {
            System.out.println("ФИО, телефон и причина обращения обязательны.");
            return;
        }

        TimeSlot selectedSlot = slots.get(slotNumber - 1);

        send(output, new MessageBookAppointment(
                selectedSlot.getDateTime(),
                fullName,
                phone,
                complaints
        ));

        MessageBookAppointmentResult result =
                readResponse(input, MessageBookAppointmentResult.class);

        if (result.Error()) {
            System.err.println(
                    "Ошибка бронирования: " + result.getErrorMessage()
            );
            return;
        }

        System.out.println("\n" + result.getConfirmationMessage());
    }

    private static MessageGetScheduleResult requestSchedule(
            ObjectInputStream input,
            ObjectOutputStream output
    ) throws IOException, ClassNotFoundException {

        send(output, new MessageGetSchedule());

        return readResponse(input, MessageGetScheduleResult.class);
    }

    private static void printSlots(List<TimeSlot> slots) {
        System.out.println("----------------------------");

        for (int i = 0; i < slots.size(); i++) {
            System.out.printf(
                    "%2d. %s%n",
                    i + 1,
                    slots.get(i).getDateTime().format(FORMATTER)
            );
        }

        System.out.println("----------------------------");
    }

    private static String readLine(Scanner console) throws EOFException {
        if (!console.hasNextLine()) {
            throw new EOFException("Ввод завершён.");
        }

        return console.nextLine().trim();
    }

    private static void send(
            ObjectOutputStream output,
            Message message
    ) throws IOException {

        output.reset();
        output.writeObject(message);
        output.flush();
    }

    private static <T> T readResponse(
            ObjectInputStream input,
            Class<T> expectedType
    ) throws IOException, ClassNotFoundException {

        Object response = input.readObject();

        if (!expectedType.isInstance(response)) {
            throw new IOException(
                    "Неожиданный ответ сервера. Ожидался "
                            + expectedType.getSimpleName()
            );
        }

        return expectedType.cast(response);
    }
}