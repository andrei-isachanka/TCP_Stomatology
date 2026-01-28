package dentist.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import dentist.*;

public class ServerThread extends Thread {
    
    private Socket socket;
    private ObjectInputStream is;
    private ObjectOutputStream os;
    private String clientName;
    private boolean connected = false;
    
    public ServerThread(Socket socket) {
        this.socket = socket;
    }
    
    @Override
    public void run() {
        try {
            os = new ObjectOutputStream(socket.getOutputStream());
            is = new ObjectInputStream(socket.getInputStream());
            
            if (processConnect()) {
                connected = true;
                
                while (connected) {
                    try {
                        Message msg = (Message) is.readObject();
                        
                        if (msg == null) {
                            break;
                        }
                        
                        processMessage(msg);
                        
                    } catch (ClassNotFoundException e) {
                        System.err.println("Неизвестный тип сообщения: " + e.getMessage());
                        break;
                    } catch (IOException e) {
                        break;
                    }
                }
            }
            
        } catch (IOException e) {
            System.err.println("Ошибка I/O в ServerThread: " + e.getMessage());
        } finally {
            disconnect();
        }
    }
    
    private boolean processConnect() {
        try {
            Message msg = (Message) is.readObject();
            
            if (msg.getID() != Protocol.CMD_CONNECT) {
                os.writeObject(new MessageConnectResult(
                    Protocol.RESULT_CODE_ERROR, 
                    "Ожидалось сообщение подключения"));
                return false;
            }
            
            MessageConnect connectMsg = (MessageConnect) msg;
            clientName = connectMsg.getClientName();

            // Если имя не занято
            if (!ServerMain.addClient(clientName, this)) {
                os.writeObject(new MessageConnectResult(
                    Protocol.RESULT_CODE_ERROR,
                    "Клиент с таким именем уже подключен"));
                return false;
            }
            
            os.writeObject(new MessageConnectResult(Protocol.RESULT_CODE_OK));
            
            System.out.println("[CONNECT] Клиент '" + clientName + "' подключился");
            return true;
            
        } catch (Exception e) {
            System.err.println("Ошибка при подключении: " + e.getMessage());
            return false;
        }
    }
    
    private void processMessage(Message msg) {
        try {
            switch (msg.getID()) {
                case Protocol.CMD_DISCONNECT:
                    processDisconnect();
                    break;
                    
                case Protocol.CMD_GET_SCHEDULE:
                    processGetSchedule();
                    break;
                    
                case Protocol.CMD_BOOK_APPOINTMENT:
                    processBookAppointment((MessageBookAppointment) msg);
                    break;
                    
                default:
                    System.err.println("Неизвестная команда: " + msg.getID());
            }
        } catch (Exception e) {
            System.err.println("Ошибка обработки сообщения: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void processDisconnect() {
        System.out.println("[DISCONNECT] Клиент '" + clientName + "' отключился");
        connected = false;
    }
    
    private void processGetSchedule() throws IOException {
        System.out.println("[REQUEST] " + clientName + " запросил расписание");
        
        ArrayList<TimeSlot> available = ServerMain.getAvailableSlots();
        
        MessageGetScheduleResult result = new MessageGetScheduleResult(
            Protocol.RESULT_CODE_OK, 
            available
        );
        
        os.writeObject(result);
        
        System.out.println("[RESPONSE] Отправлено " + available.size() + " свободных слотов");
    }
    
    private void processBookAppointment(MessageBookAppointment msg) throws IOException {
        System.out.println("[BOOKING REQUEST] " + clientName + " пытается забронировать " +
            msg.getDateTime().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")));
        
        boolean success = ServerMain.bookSlot(
            msg.getDateTime(),
            msg.getPatientName(),
            msg.getPatientPhone(),
            msg.getComplaints()
        );
        
        if (success) {
            String confirmation = String.format(
                "Вы записаны на %s.\nФИО: %s\nТелефон: %s",
                msg.getDateTime().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")),
                msg.getPatientName(),
                msg.getPatientPhone()
            );
            
            os.writeObject(new MessageBookAppointmentResult(
                Protocol.RESULT_CODE_OK, 
                confirmation
            ));
            
            System.out.println("[BOOKING SUCCESS] Запись оформлена для " + msg.getPatientName());
            
        } else {
            os.writeObject(new MessageBookAppointmentResult(
                Protocol.RESULT_CODE_SLOT_TAKEN,
                "Это время уже занято другим пациентом"
            ));
            
            System.out.println("[BOOKING FAILED] Слот уже занят");
        }
    }
    
    public void disconnect() {
        connected = false;
        
        if (clientName != null) {
            ServerMain.removeClient(clientName);
            System.out.println("[REMOVE] Клиент '" + clientName + "' удален из списка");
        }
        
        try {
            if (os != null) os.close();
        } catch (IOException e) {
        }
        
        try {
            if (is != null) is.close();
        } catch (IOException e) {
        }
        
        try {
            if (socket != null) socket.close();
        } catch (IOException e) {
        }
    }
}
