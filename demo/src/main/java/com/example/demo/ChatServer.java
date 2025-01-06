package com.example.demo;

import java.io.*;
import java.net.*;
import java.util.*;

public class ChatServer {

    private static final int PORT = 12345;
    private static final Map<String, String> userCredentials = new HashMap<>();
    private static final Map<String, ChatRoom> chatRooms = new HashMap<>();

    public static void main(String[] args) {
        userCredentials.put("Yasmine", "123");
        userCredentials.put("Fox", "123");
        userCredentials.put("Atta", "123");

        System.out.println("Server is running...");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ClientHandler implements Runnable {
        private final Socket clientSocket;
        private BufferedReader in;
        private PrintWriter out;
        private String username;
        private ChatRoom currentChatRoom;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                out = new PrintWriter(clientSocket.getOutputStream(), true);

                // Read username and password from the client
                String username = in.readLine();
                String password = in.readLine();

                // Validate credentials
                if (isValidUser(username, password)) {
                    out.println("AUTH_SUCCESS");
                    System.out.println("User authenticated: " + username);
                    this.username = username;
                    handleClientChat();
                } else {
                    out.println("AUTH_FAIL");
                    System.out.println("Invalid login attempt: " + username);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private boolean isValidUser(String username, String password) {
            return userCredentials.containsKey(username) && userCredentials.get(username).equals(password);
        }

        private void handleClientChat() throws IOException {
            String message;
            // Start listening for room creation/join
            while ((message = in.readLine()) != null) {
                if (message.startsWith("CREATE_ROOM:")) {
                    createChatRoom(message.substring(12));
                } else if (message.startsWith("JOIN_ROOM:")) {
                    joinChatRoom(message.substring(10));
                } else if (message.equals("LEAVE_ROOM")) {
                    leaveChatRoom();
                } else {
                    currentChatRoom.broadcastMessage(username + ": " + message);
                }
            }
        }

        private void createChatRoom(String roomName) {
            synchronized (chatRooms) {
                if (!chatRooms.containsKey(roomName)) {
                    currentChatRoom = new ChatRoom(roomName);
                    chatRooms.put(roomName, currentChatRoom);
                    currentChatRoom.addClient(this);
                    out.println("CREATE_ROOM_SUCCESS");
                    System.out.println("Room created: " + roomName);
                } else {
                    out.println("CREATE_ROOM_FAIL");
                    System.out.println("Room already exists: " + roomName);
                }
            }
        }

        private void joinChatRoom(String roomName) {
            synchronized (chatRooms) {
                currentChatRoom = chatRooms.get(roomName);
                if (currentChatRoom != null) {
                    currentChatRoom.addClient(this);
                    out.println("JOIN_ROOM_SUCCESS");
                    System.out.println(username + " joined room: " + roomName);
                } else {
                    out.println("ROOM_NOT_FOUND");
                    System.out.println("Room not found: " + roomName);
                }
            }
        }

        private void leaveChatRoom() {
            if (currentChatRoom != null) {
                currentChatRoom.removeClient(this);
                out.println("LEAVE_ROOM_SUCCESS");
                currentChatRoom = null;
                System.out.println(username + " left the room.");
            }
        }
    }

    private static class ChatRoom {
        private final String roomName;
        private final Set<ClientHandler> clients = new HashSet<>();

        public ChatRoom(String roomName) {
            this.roomName = roomName;
        }

        public void addClient(ClientHandler clientHandler) {
            synchronized (clients) {
                clients.add(clientHandler);
            }
        }

        public void removeClient(ClientHandler clientHandler) {
            synchronized (clients) {
                clients.remove(clientHandler);
            }
        }

        public void broadcastMessage(String message) {
            synchronized (clients) {
                for (ClientHandler client : clients) {
                    client.out.println(message);  // Broadcast to all clients in the chatroom
                }
            }
        }
    }
}
