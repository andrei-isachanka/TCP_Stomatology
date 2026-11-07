package dentist.Server;

import dentist.Protocol;
import dentist.TimeSlot;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;

public class ServerMain {

    private static final int MAX_CLIENTS = 100;
    private static final int ACCEPT_TIMEOUT_MS = 1_000;

    private static final Object SCHEDULE_LOCK = new Object();
    private static final Object CLIENTS_LOCK = new Object();

    private static final ArrayList<TimeSlot> SCHEDULE = new ArrayList<>();

    private static final TreeMap<String, ServerThread> CLIENTS =
            new TreeMap<>();

    private static final Set<ServerThread> CONNECTIONS = new HashSet<>();

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private static volatile boolean stopFlag;

    public static void main(String[] args) {
        initializeSchedule();

        try (ServerSocket serverSocket = new ServerSocket(Protocol.PORT)) {
            serverSocket.setSoTimeout(ACCEPT_TIMEOUT_MS);

            System.out.println("--- Сервер системы записи к стоматологу ---");
            System.out.println("Сервер запущен на порту " + Protocol.PORT);
            System.out.println("Для остановки введите 'stop'");
            System.out.println("----------------------------------------");

            ServerStopThread stopThread = new ServerStopThread();
            stopThread.setDaemon(true);
            stopThread.start();

            while (!getStopFlag()) {
                Socket socket;

                try {
                    socket = serverSocket.accept();
                } catch (SocketTimeoutException e) {
                    continue;
                }

                ServerThread client = new ServerThread(socket);

                if (!registerConnection(client)) {
                    client.disconnect();
                    System.out.println(
                            "[-] Подключение отклонено: лимит соединений "
                                    + "или остановка сервера."
                    );
                    continue;
                }

                System.out.println(
                        "[+] Подключение: "
                                + socket.getInetAddress().getHostAddress()
                );

                client.start();
            }
        } catch (IOException e) {
            System.err.println("Ошибка сервера: " + e.getMessage());
        } finally {
            setStopFlag(true);
            stopAllClients();
            System.out.println("\nСервер остановлен.");
        }
    }

    private static void initializeSchedule() {
        synchronized (SCHEDULE_LOCK) {
            SCHEDULE.clear();

            LocalDateTime now = LocalDateTime.now();
            LocalDate firstDay = now.toLocalDate();

            for (int day = 0; day < 7; day++) {
                LocalDate date = firstDay.plusDays(day);
                DayOfWeek dayOfWeek = date.getDayOfWeek();

                if (dayOfWeek == DayOfWeek.SATURDAY
                        || dayOfWeek == DayOfWeek.SUNDAY) {
                    continue;
                }

                LocalDateTime slot =
                        LocalDateTime.of(date, LocalTime.of(9, 0));

                LocalDateTime dayEnd =
                        LocalDateTime.of(date, LocalTime.of(18, 0));

                while (slot.isBefore(dayEnd)) {
                    if (slot.isAfter(now)) {
                        SCHEDULE.add(new TimeSlot(slot));
                    }

                    slot = slot.plusMinutes(30);
                }
            }

            System.out.println(
                    "Инициализировано " + SCHEDULE.size()
                            + " временных слотов на неделю вперед."
            );
        }
    }

    public static ArrayList<TimeSlot> getAvailableSlots() {
        synchronized (SCHEDULE_LOCK) {
            ArrayList<TimeSlot> available = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();

            for (TimeSlot slot : SCHEDULE) {
                if (slot.isAvailable() && slot.getDateTime().isAfter(now)) {
                    available.add(new TimeSlot(slot.getDateTime()));
                }
            }

            return available;
        }
    }

    public static boolean bookSlot(
            LocalDateTime dateTime,
            String patientName,
            String patientPhone,
            String complaints
    ) {
        if (dateTime == null
                || isBlank(patientName)
                || isBlank(patientPhone)
                || isBlank(complaints)) {
            return false;
        }

        synchronized (SCHEDULE_LOCK) {
            if (!dateTime.isAfter(LocalDateTime.now())) {
                return false;
            }

            for (TimeSlot slot : SCHEDULE) {
                if (slot.getDateTime().equals(dateTime)) {
                    if (!slot.isAvailable()) {
                        return false;
                    }

                    slot.book(
                            patientName.trim(),
                            patientPhone.trim(),
                            complaints.trim()
                    );

                    System.out.println(
                            "[BOOKING] " + patientName.trim()
                                    + " записан на "
                                    + dateTime.format(FORMATTER)
                    );

                    return true;
                }
            }

            return false;
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static boolean registerConnection(ServerThread client) {
        synchronized (CLIENTS_LOCK) {
            if (stopFlag || CONNECTIONS.size() >= MAX_CLIENTS) {
                return false;
            }

            return CONNECTIONS.add(client);
        }
    }

    public static boolean addClient(String name, ServerThread client) {
        synchronized (CLIENTS_LOCK) {
            if (stopFlag
                    || isBlank(name)
                    || client.isDisconnected()
                    || !CONNECTIONS.contains(client)
                    || CLIENTS.containsKey(name)) {
                return false;
            }

            CLIENTS.put(name, client);
            return true;
        }
    }

    public static void removeClient(String name, ServerThread client) {
        synchronized (CLIENTS_LOCK) {
            if (name != null) {
                CLIENTS.remove(name, client);
            }

            CONNECTIONS.remove(client);
        }
    }

    public static boolean getStopFlag() {
        return stopFlag;
    }

    public static void setStopFlag(boolean value) {
        synchronized (CLIENTS_LOCK) {
            stopFlag = value;
        }
    }

    private static void stopAllClients() {
        ServerThread[] connections;

        synchronized (CLIENTS_LOCK) {
            connections = CONNECTIONS.toArray(new ServerThread[0]);
        }

        for (ServerThread client : connections) {
            client.disconnect();
        }

        for (ServerThread client : connections) {
            try {
                client.join(2_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}

class ServerStopThread extends Thread {

    @Override
    public void run() {
        Scanner console = new Scanner(System.in);

        while (!ServerMain.getStopFlag() && console.hasNextLine()) {
            String command = console.nextLine().trim();

            if ("stop".equalsIgnoreCase(command)) {
                System.out.println("Получена команда остановки...");
                ServerMain.setStopFlag(true);
                return;
            }
        }
    }
}