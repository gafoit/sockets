package client;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class ChatClient {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;

    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            Scanner scanner = new Scanner(System.in);

            // Ввод никнейма
            System.out.println("Введите ваш никнейм:");
            String nickname = scanner.nextLine();
            out.println(nickname);

            // Поток для чтения сообщений от сервера
            Thread listenerThread = new Thread(() -> {
                try {
                    String serverMessage;
                    while ((serverMessage = in.readLine()) != null) {
                        System.out.println(serverMessage);
                    }
                } catch (IOException e) {
                    System.err.println("Соединение с сервером прервано.");
                }
            });
            listenerThread.start();

            System.out.println("Вы подключены к чату.");
            System.out.println("Для отправки личного сообщения используйте формат: @имя_пользователя сообщение");
            System.out.println("Для выхода введите: /exit");

            while (true) {
                String message = scanner.nextLine();

                if (message.equalsIgnoreCase("/exit")) {
                    System.out.println("Вы вышли из чата.");
                    break;
                }

                out.println(message);
            }
        } catch (Exception e) {
            System.err.println("Ошибка клиента: " + e.getMessage());
        }
    }
}