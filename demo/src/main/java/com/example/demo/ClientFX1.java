package com.example.demo;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.io.*;
import java.net.Socket;

public class ClientFX1 extends Application {

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private VBox chatBubbleLayout;
    private TextField messageField;
    private String username;
    private String currentRoom;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        showLoginPage(primaryStage);
    }

    private void showLoginPage(Stage stage) {
        VBox loginLayout = new VBox(10);
        loginLayout.setPadding(new Insets(20));
        loginLayout.setAlignment(Pos.CENTER);

        TextField usernameField = new TextField();
        usernameField.setPromptText("Enter Username");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Enter Password");

        Button loginButton = new Button("Login");
        loginButton.setOnAction(e -> {
            String enteredUsername = usernameField.getText();
            String enteredPassword = passwordField.getText();
            if (authenticate(enteredUsername, enteredPassword)) {
                this.username = enteredUsername;
                showChatRoomPage(stage);
            } else {
                showErrorAlert("Login Failed", "Invalid username or password.");
            }
        });

        loginLayout.getChildren().addAll(usernameField, passwordField, loginButton);

        Scene loginScene = new Scene(loginLayout, 300, 200);
        stage.setScene(loginScene);
        stage.setTitle("Chat Login");
        stage.show();
    }

    private boolean authenticate(String username, String password) {
        try {
            socket = new Socket("localhost", 12345);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            out.println(username);
            out.println(password);

            String response = in.readLine();
            return response != null && response.equals("AUTH_SUCCESS");
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void showChatRoomPage(Stage stage) {
        VBox roomLayout = new VBox(10);
        roomLayout.setPadding(new Insets(20));
        roomLayout.setAlignment(Pos.CENTER);

        TextField newRoomField = new TextField();
        newRoomField.setPromptText("Enter room name");

        Button createButton = new Button("Create Room");
        createButton.setOnAction(e -> {
            String roomName = newRoomField.getText();
            if (!roomName.isEmpty()) {
                out.println("CREATE_ROOM:" + roomName);
                String response = getServerResponse();
                if (response.equals("CREATE_ROOM_SUCCESS")) {
                    currentRoom = roomName;
                    showChatPage(stage);
                } else {
                    showErrorAlert("Error", "Room creation failed or room already exists.");
                }
            }
        });

        Button joinButton = new Button("Join Room");
        joinButton.setOnAction(e -> {
            String roomName = newRoomField.getText();
            if (!roomName.isEmpty()) {
                out.println("JOIN_ROOM:" + roomName);
                String response = getServerResponse();
                if (response.equals("JOIN_ROOM_SUCCESS")) {
                    currentRoom = roomName;
                    showChatPage(stage);
                } else {
                    showErrorAlert("Error", "Room not found.");
                }
            }
        });

        roomLayout.getChildren().addAll(newRoomField, createButton, joinButton);

        Scene roomScene = new Scene(roomLayout, 300, 200);
        stage.setScene(roomScene);
        stage.setTitle("Select Chatroom");
        stage.show();
    }

    private void showChatPage(Stage stage) {
        BorderPane chatLayout = new BorderPane();

        chatBubbleLayout = new VBox(10);
        chatBubbleLayout.setPadding(new Insets(10));

        ScrollPane scrollPane = new ScrollPane(chatBubbleLayout);
        scrollPane.setFitToWidth(true);
        chatLayout.setCenter(scrollPane);

        HBox inputLayout = new HBox(10);
        inputLayout.setPadding(new Insets(10));

        messageField = new TextField();
        messageField.setPromptText("Type your message here...");

        Button sendButton = new Button("Send");
        sendButton.setOnAction(e -> sendMessage());

        inputLayout.getChildren().addAll(messageField, sendButton);
        chatLayout.setBottom(inputLayout);

        Scene chatScene = new Scene(chatLayout, 500, 400);
        stage.setScene(chatScene);
        stage.setTitle("Chat - " + currentRoom);
        stage.show();

        new Thread(this::listenForMessages).start();
    }

    private void sendMessage() {
        String message = messageField.getText();
        if (message.isEmpty()) return;

        out.println(message);
        Platform.runLater(() -> {
            Label sentBubble = new Label("You: " + message);
            sentBubble.setStyle("-fx-background-color: #b3e5fc; -fx-padding: 10px; -fx-border-radius: 10px; -fx-background-radius: 10px;");
            sentBubble.setWrapText(true);
            sentBubble.setMaxWidth(400);
            chatBubbleLayout.getChildren().add(sentBubble);
        });

        messageField.clear();
    }

    private void listenForMessages() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                String finalMessage = message;
                Platform.runLater(() -> {
                    Label receivedBubble = new Label(finalMessage);
                    receivedBubble.setStyle("-fx-background-color: #c8e6c9; -fx-padding: 10px; -fx-border-radius: 10px; -fx-background-radius: 10px;");
                    receivedBubble.setWrapText(true);
                    receivedBubble.setMaxWidth(400);
                    chatBubbleLayout.getChildren().add(receivedBubble);
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showErrorAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private String getServerResponse() {
        try {
            return in.readLine();
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }
}

