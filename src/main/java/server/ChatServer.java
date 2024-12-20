package server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

public class ChatServer {
    private static final Logger logger = LoggerFactory.getLogger(ChatServer.class);
    private static final int PORT = 12345;
    private static final Set<ClientHandler> clients = new HashSet<>();

    public static void main(String[] args) {
        logger.info("Сервер запускается на порту {}", PORT);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            logger.info("Сервер запущен и ожидает подключений...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                logger.info("Новое подключение от {}", clientSocket.getInetAddress());

                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clients.add(clientHandler);
                new Thread(clientHandler).start();
            }
        } catch (Exception e) {
            logger.error("Ошибка на сервере: ", e);
        }
    }

    public static synchronized void broadcast(String message, ClientHandler sender) {
        for (ClientHandler client : clients) {
            if (client != sender) {
                client.sendMessage(message);
            }
        }
    }

    public static synchronized void removeClient(ClientHandler clientHandler) {
        clients.remove(clientHandler);
        logger.info("Клиент отключен: {}", clientHandler.getClientName());
    }

    public static synchronized ClientHandler getClientByName(String name) {
        for (ClientHandler client : clients) {
            if (client.getClientName().equals(name)) {
                return client;
            }
        }
        return null; // Если клиент с таким именем не найден
    }

    public static String getClientList(String currentUserName) {
        StringBuilder clientList = new StringBuilder("Список пользователей в чате:\n");
        synchronized (clients) {
            for (ClientHandler client : clients) {
                if (client.getClientName().equals(currentUserName)) {
                    clientList.append("- ").append(client.getClientName()).append(" (Вы)\n");
                } else {
                    clientList.append("- ").append(client.getClientName()).append("\n");
                }
            }
        }
        return clientList.toString();
    }
}

class ClientHandler implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(ClientHandler.class);

    private final Socket socket;
    private PrintWriter out;
    private String clientName;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            this.out = out;

            // Получение никнейма клиента

            this.clientName = in.readLine();
            logger.info("Клиент с никнеймом '{}' подключился.", clientName);

            synchronized (ChatServer.class) {
                ChatServer.broadcast(clientName + " присоединился к чату!", this);
            }

            // Чтение сообщений от клиента
            String message;
            while ((message = in.readLine()) != null) {
                if (message.startsWith("/")) {
                    // Обработка команд
                    switch (message) {
                        case "/list":
                            out.println(ChatServer.getClientList(clientName));
                            break;
                        case "/exit":
                            out.println("Вы покинули чат.");
                            break;
                        default:
                            out.println("Неизвестная команда. Используйте /list для отображения пользователей или /exit для выхода.");
                    }
                } else if (message.startsWith("@")) {
                    // Личное сообщение
                    String[] parts = message.split(" ", 2);
                    if (parts.length < 2) {
                        out.println("Формат личного сообщения: @имя_пользователя сообщение");
                        continue;
                    }
                    String recipientName = parts[0].substring(1); // Убираем "@"
                    String privateMessage = parts[1];

                    ClientHandler recipient = ChatServer.getClientByName(recipientName);
                    if (recipient != null) {
                        recipient.sendMessage("[Личное сообщение от " + clientName + "]: " + privateMessage);
                        this.sendMessage("[Вы отправили " + recipientName + "]: " + privateMessage);
                    } else {
                        out.println("Пользователь с именем " + recipientName + " не найден.");
                    }
                } else {
                    // Широковещательное сообщение
                    ChatServer.broadcast(clientName + ": " + message, this);
                }
            }
        } catch (Exception e) {
            logger.error("Ошибка у клиента {}: ", clientName, e);
        } finally {
            ChatServer.removeClient(this);
            ChatServer.broadcast(clientName + " покинул чат.", this);
            try {
                socket.close();
            } catch (Exception e) {
                logger.error("Не удалось закрыть соединение с клиентом {}: ", clientName, e);
            }
        }
    }

    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    public String getClientName() {
        return clientName;
    }
}