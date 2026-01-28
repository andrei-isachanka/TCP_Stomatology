package dentist.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Scanner;
import dentist.*;

public class ServerMain {

    private static int MAX_CLIENTS = 100;

    private static ArrayList<TimeSlot> schedule = new ArrayList<>();
    private static Object syncSchedule = new Object();

    public static void main(String[] args) {

        initializeSchedule();

        try (ServerSocket serv = new ServerSocket(Protocol.PORT)) {
            System.out.println("    Сервер системы записи к стоматологу ");
            System.out.println("Сервер запущен на порту " + Protocol.PORT);
            System.out.println("Расписание инициализировано: " + schedule.size() + " слотов");
            System.out.println("Для остановки введите 'stop'");
            System.out.println("____________________________________\n");

            ServerStopThread tester = new ServerStopThread();
            tester.start();

            while (true) {
                Socket sock = accept(serv);

                if (sock != null) {
                    if (ServerMain.getNumClients() < ServerMain.MAX_CLIENTS) {
                        System.out.println("Клиент подключен: " +
                            sock.getInetAddress().getHostName());
                        ServerThread server = new ServerThread(sock);
                        server.start();
                    } else {
                        System.out.println("Отклонено подключение (превышен лимит): " +
                            sock.getInetAddress().getHostName());
                        sock.close();
                    }
                }

                if (ServerMain.getStopFlag()) {
                    break;
                }
            }

        } catch (IOException e) {
            System.err.println("Ошибка сервера: " + e.getMessage());
        } finally {
            stopAllClients();
            System.out.println("\nСервер остановлен.");
        }

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }
    }

    private static void initializeSchedule() {
        synchronized (syncSchedule) {

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime start = now.plusHours(1).withMinute(0).withSecond(0).withNano(0);

            for (int day = 0; day < 7; day++) {
                LocalDateTime current = start.plusDays(day);

                DayOfWeek dayOfWeek = current.getDayOfWeek();
                if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
                    continue;
                }


                LocalDateTime dayStart = current.withHour(9).withMinute(0);
                LocalDateTime dayEnd = current.withHour(18).withMinute(0);

                LocalDateTime slot = dayStart;
                while (slot.isBefore(dayEnd)) {
                    schedule.add(new TimeSlot(slot));
                    slot = slot.plusMinutes(30);
                }
            }

            System.out.println("Инициализировано " + schedule.size() +
                " временных слотов на неделю вперед.");
        }
    }

    public static ArrayList<TimeSlot> getAvailableSlots() {
        synchronized (syncSchedule) {
            ArrayList<TimeSlot> available = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();

            for (TimeSlot slot : schedule) {
                if (slot.isAvailable() && slot.getDateTime().isAfter(now)) {
                    available.add(slot);
                }
            }

            return available;
        }
    }

    public static boolean bookSlot(LocalDateTime dateTime, String patientName,
                                   String patientPhone, String complaints) {
        synchronized (syncSchedule) {
            for (TimeSlot slot : schedule) {
                if (slot.getDateTime().equals(dateTime)) {
                    if (slot.isAvailable()) {
                        slot.book(patientName, patientPhone, complaints);
                        System.out.println("[BOOKING] " + patientName + " записан на " +
                            dateTime.format(java.time.format.DateTimeFormatter.ofPattern(
                                "dd.MM.yyyy HH:mm")));
                        return true;
                    } else {
                        return false;
                    }
                }
            }
            return false;
        }
    }

    public static Socket accept(ServerSocket serv) {
        assert(serv != null);
        try {
            serv.setSoTimeout(1000);
            Socket sock = serv.accept();
            return sock;
        } catch (SocketException e) {
        } catch (IOException e) {
        }
        return null;
    }

    private static void stopAllClients() {
        String[] clients = getClients();
        for (String client : clients) {
            ServerThread ct = getClient(client);
            if (ct != null) {
                ct.disconnect();
            }
        }
    }

    private static Object syncFlags = new Object();
    private static boolean stopFlag = false;

    public static boolean getStopFlag() {
        synchronized (ServerMain.syncFlags) {
            return stopFlag;
        }
    }

    public static void setStopFlag(boolean value) {
        synchronized (ServerMain.syncFlags) {
            stopFlag = value;
        }
    }

    private static Object syncClients = new Object();
    private static java.util.TreeMap<String, ServerThread> clients =
        new java.util.TreeMap<>();

    public static int getNumClients() {
        synchronized (ServerMain.syncClients) {
            return clients.size();
        }
    }

    public static String[] getClients() {
        synchronized (ServerMain.syncClients) {
            return clients.keySet().toArray(new String[0]);
        }
    }

    public static boolean addClient(String name, ServerThread client) {
        synchronized (ServerMain.syncClients) {
            if (clients.containsKey(name)) {
                return false;
            }
            clients.put(name, client);
            return true;
        }
    }

    public static void removeClient(String name) {
        synchronized (ServerMain.syncClients) {
            clients.remove(name);
        }
    }

    public static ServerThread getClient(String name) {
        synchronized (ServerMain.syncClients) {
            return clients.get(name);
        }
    }
}

class ServerStopThread extends Thread {

    @Override
    public void run() {
        try (Scanner in = new Scanner(System.in)) {
            while (true) {
                String cmd = in.nextLine();
                if (cmd.equalsIgnoreCase("stop")) {
                    System.out.println("Получена команда остановки...");
                    ServerMain.setStopFlag(true);
                    break;
                }
            }
        } catch (Exception e) {
            System.err.println("Ошибка в потоке остановки: " + e.getMessage());
        }
    }
}
