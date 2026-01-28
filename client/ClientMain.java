package dentist.client;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Scanner;
import dentist.*;

public class ClientMain {
    
    public static void main(String[] args) {
        if (args.length < 1 || args.length > 2) {
            System.err.println(
                "Неверное количество аргументов\n" +
                "Использование: java ClientMain <ваше_имя> [host]"
            );
            waitKeyToStop();
            return;
        }
        
        try (Socket sock = (args.length == 1 ?
                new Socket(InetAddress.getLocalHost(), Protocol.PORT) :
                new Socket(args[1], Protocol.PORT))) {
            
            System.out.println(" Система записи к стоматологу ");
            System.out.println("Подключение к серверу...");
            session(sock, args[0]);
            
        } catch (Exception e) {
            System.err.println("Ошибка подключения: " + e.getMessage());
        } finally {
            System.out.println("\nДо свидания!");
        }
    }
    
    static void waitKeyToStop() {
        System.out.println("Нажмите Enter для выхода");
        try {
            System.in.read();
        } catch (IOException e) {
        }
    }
    
    static class Session {
        boolean connected = false;
        String clientName = null;
        
        Session(String name) {
            clientName = name;
        }
    }
    
    static void session(Socket s, String name) {
        try (Scanner in = new Scanner(System.in);
             ObjectInputStream is = new ObjectInputStream(s.getInputStream());
             ObjectOutputStream os = new ObjectOutputStream(s.getOutputStream())) {
            
            Session ses = new Session(name);
            
            if (openSession(ses, is, os)) {
                try {
                    showMenu();
                    
                    while (true) {
                        System.out.print("\nВыберите действие (1-3): ");
                        String choice = in.nextLine().trim();
                        
                        if (choice.equals("1")) {
                            // Просмотр свободного расписания
                            if (!viewSchedule(ses, is, os)) {
                                break;
                            }
                        } else if (choice.equals("2")) {
                            // Записаться на прием
                            if (!bookAppointment(ses, is, os, in)) {
                                break;
                            }
                        } else if (choice.equals("3")) {
                            // Выход
                            System.out.println("Выход из системы...");
                            break;
                        } else {
                            System.out.println("Неверный выбор. Попробуйте снова.");
                        }
                    }
                } finally {
                    closeSession(ses, os);
                }
            }
            
        } catch (Exception e) {
            System.err.println("Ошибка сеанса: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    static void showMenu() {
        System.out.println("\n          МЕНЮ ");
        System.out.println("1. Просмотреть свободное время");
        System.out.println("2. Записаться на прием");
        System.out.println("3. Выход");
        System.out.println("__________________________________");
    }
    
    static boolean openSession(Session ses, ObjectInputStream is, ObjectOutputStream os)
            throws IOException, ClassNotFoundException {
        
        os.writeObject(new MessageConnect(ses.clientName));
        MessageConnectResult msg = (MessageConnectResult) is.readObject();
        
        if (!msg.Error()) {
            System.out.println("Подключение успешно!");
            System.out.println("Добро пожаловать, " + ses.clientName + "!");
            ses.connected = true;
            return true;
        }
        
        System.err.println("Не удалось подключиться: " + msg.getErrorMessage());
        waitKeyToStop();
        return false;
    }
    
    static void closeSession(Session ses, ObjectOutputStream os) {
        if (ses.connected) {
            try {
                os.writeObject(new MessageDisconnect());
                System.out.println("Отключено от сервера.");
            } catch (IOException e) {
                System.err.println("Ошибка при отключении: " + e.getMessage());
            }
        }
    }
    
    static boolean viewSchedule(Session ses, ObjectInputStream is, ObjectOutputStream os)
            throws IOException, ClassNotFoundException {
        
        System.out.println("\n  Запрос свободного расписания");
        os.writeObject(new MessageGetSchedule());
        
        MessageGetScheduleResult msg = (MessageGetScheduleResult) is.readObject();
        
        if (msg.Error()) {
            System.err.println("Ошибка получения расписания: " + msg.getErrorMessage());
            return true;
        }
        
        ArrayList<TimeSlot> slots = msg.getAvailableSlots();
        
        if (slots.isEmpty()) {
            System.out.println("К сожалению, нет свободных слотов на ближайшую неделю.");
        } else {
            System.out.println("\nСвободное время на неделю:");
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy (EEE) HH:mm");
            for (int i = 0; i < slots.size(); i++) {
                TimeSlot slot = slots.get(i);
                System.out.printf("%2d. %s\n", i + 1, 
                    slot.getDateTime().format(formatter));
            }
        }
        
        return true;
    }
    
    static boolean bookAppointment(Session ses, ObjectInputStream is, 
                                   ObjectOutputStream os, Scanner in)
            throws IOException, ClassNotFoundException {
        
        System.out.println("\n  Запись на прием");

        os.writeObject(new MessageGetSchedule());
        MessageGetScheduleResult scheduleMsg = (MessageGetScheduleResult) is.readObject();
        
        if (scheduleMsg.Error() || scheduleMsg.getAvailableSlots().isEmpty()) {
            System.out.println("Нет свободных слотов для записи.");
            return true;
        }
        
        ArrayList<TimeSlot> slots = scheduleMsg.getAvailableSlots();
        
        System.out.println("\nДоступное время:");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy (EEE) HH:mm");
        for (int i = 0; i < slots.size(); i++) {
            TimeSlot slot = slots.get(i);
            System.out.printf("%2d. %s\n", i + 1, 
                slot.getDateTime().format(formatter));
        }
        
        System.out.print("\nВыберите номер слота (или 0 для отмены): ");
        String slotChoice = in.nextLine().trim();
        
        int slotIndex;
        try {
            slotIndex = Integer.parseInt(slotChoice);
        } catch (NumberFormatException e) {
            System.out.println("Неверный ввод.");
            return true;
        }
        
        if (slotIndex == 0) {
            System.out.println("Отмена записи.");
            return true;
        }
        
        if (slotIndex < 1 || slotIndex > slots.size()) {
            System.out.println("Неверный номер слота.");
            return true;
        }
        
        TimeSlot selectedSlot = slots.get(slotIndex - 1);

        System.out.print("Введите ФИО: ");
        String fullName = in.nextLine().trim();
        
        System.out.print("Введите телефон: ");
        String phone = in.nextLine().trim();
        
        System.out.print("Введите жалобы/причину обращения: ");
        String complaints = in.nextLine().trim();

        MessageBookAppointment bookMsg = new MessageBookAppointment(
            selectedSlot.getDateTime(),
            fullName,
            phone,
            complaints
        );
        
        os.writeObject(bookMsg);
        MessageBookAppointmentResult result = (MessageBookAppointmentResult) is.readObject();
        
        if (result.Error()) {
            System.err.println("Ошибка бронирования: " + result.getErrorMessage());
            if (result.getCode() == Protocol.RESULT_CODE_SLOT_TAKEN) {
                System.out.println("Это время уже было занято другим клиентом.");
            }
        } else {
            System.out.println("\n " + result.getConfirmationMessage());
            System.out.println("Ваша запись успешно оформлена!");
        }
        
        return true;
    }
}
