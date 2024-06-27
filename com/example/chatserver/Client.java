package com.example.chatserver;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.*;

public class Client {
    private static final String SERVER_URL = "http://localhost:8000";

    private ClientGUI clientGUI;
    private String username;
    private boolean isAnonymous = false; // 是否匿名聊天
    private ScheduledExecutorService scheduler;

    public Client(ClientGUI clientGUI) {
        this.clientGUI = clientGUI;
    }

    public boolean login(String username, String password) {
        try {
            URL url = new URL(SERVER_URL + "/login");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);

            String urlParameters = "username=" + URLEncoder.encode(username, "UTF-8") +
                    "&password=" + URLEncoder.encode(password, "UTF-8");

            System.out.println("Sending login request with username: " + username + " and password: " + password);

            try (DataOutputStream out = new DataOutputStream(connection.getOutputStream())) {
                out.writeBytes(urlParameters);
                out.flush();
            }

            StringBuilder responseBuilder = new StringBuilder();
            try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String line;
                while ((line = in.readLine()) != null) {
                    responseBuilder.append(line);
                }
            }

            String response = responseBuilder.toString();
            System.out.println("Login response: " + response);

            JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
            return jsonResponse.get("success").getAsBoolean();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean sendMessage(String message, String toUser) {
        try {
            URL url = new URL(SERVER_URL + "/send");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);

            String usernameToUse = isAnonymous ? "Anonymous" : username;
            String urlParameters = "username=" + URLEncoder.encode(usernameToUse, "UTF-8") +
                    "&message=" + URLEncoder.encode(message, "UTF-8");

            System.out.println("Sending message: " + message);

            try (DataOutputStream out = new DataOutputStream(connection.getOutputStream())) {
                out.writeBytes(urlParameters);
                out.flush();
            }

            StringBuilder responseBuilder = new StringBuilder();
            try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String line;
                while ((line = in.readLine()) != null) {
                    responseBuilder.append(line);
                }
            }

            String response = responseBuilder.toString();
            System.out.println("Send message response: " + response);

            JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
            return jsonResponse.get("success").getAsBoolean();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Map<String, String>> getMessages(int messageCount) {
        try {
            URL url = new URL(SERVER_URL + "/messages?count=" + messageCount);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            System.out.println("Requesting messages from count: " + messageCount);

            StringBuilder responseBuilder = new StringBuilder();
            try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String line;
                while ((line = in.readLine()) != null) {
                    responseBuilder.append(line);
                }
            }

            String response = responseBuilder.toString();
            System.out.println("Get messages response: " + response);

            JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
            List<Map<String, String>> messages = new ArrayList<>();
            for (var jsonElement : jsonResponse.get("newMessages").getAsJsonArray()) {
                messages.add(new Gson().fromJson(jsonElement, Map.class));
            }

            return messages;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public List<String> getActiveUsers() {
        try {
            URL url = new URL(SERVER_URL + "/list");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            System.out.println("Requesting list of active users");

            StringBuilder responseBuilder = new StringBuilder();
            try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String line;
                while ((line = in.readLine()) != null) {
                    responseBuilder.append(line);
                }
            }

            String response = responseBuilder.toString();
            System.out.println("List active users response: " + response);

            List<String> activeUsers = new Gson().fromJson(response, List.class);
            return activeUsers;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public boolean logout() {
        try {
            URL url = new URL(SERVER_URL + "/logout");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);

            String urlParameters = "username=" + URLEncoder.encode(username, "UTF-8");

            System.out.println("Logging out username: " + username);

            try (DataOutputStream out = new DataOutputStream(connection.getOutputStream())) {
                out.writeBytes(urlParameters);
                out.flush();
            }

            StringBuilder responseBuilder = new StringBuilder();
            try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String line;
                while ((line = in.readLine()) != null) {
                    responseBuilder.append(line);
                }
            }

            String response = responseBuilder.toString();
            System.out.println("Logout response: " + response);

            JsonObject jsonResponse = JsonParser.parseString(response).getAsJsonObject();
            return jsonResponse.get("success").getAsBoolean();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void startMessagePolling() {
        scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            try {
                List<Map<String, String>> newMessages = getMessages(clientGUI.getDisplayedMessageCount());
                for (Map<String, String> msg : newMessages) {
                    String messageUsername = msg.get("username");
                    String messageContent = msg.get("message");

                    // 过滤私聊信息
                    if (messageContent.startsWith("@") && !messageContent.startsWith("@" + username + " ")) {
                        continue;
                    }

                    clientGUI.appendMessage(messageUsername + ": " + messageContent);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 0, 2, TimeUnit.SECONDS); // 每2秒轮询一次
    }

    private void stopMessagePolling() {
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }

    private void handleSystemCommand(String command) {
        switch (command.toLowerCase()) {
            case "list":
                List<String> activeUsers = getActiveUsers();
                clientGUI.appendMessage("Active users: " + String.join(", ", activeUsers));
                break;
            case "quit":
                logout();
                stopMessagePolling();
                System.exit(0);
                break;
            case "showanonymous":
                clientGUI.appendMessage("Anonymous mode: " + (isAnonymous ? "ON" : "OFF"));
                break;
            case "anonymous":
                isAnonymous = !isAnonymous;
                clientGUI.appendMessage("Anonymous mode toggled to: " + (isAnonymous ? "ON" : "OFF"));
                break;
            default:
                clientGUI.appendMessage("Unknown command: " + command);
                break;
        }
    }

    public static void main(String[] args) {
        ClientGUI clientGUI = new ClientGUI();
        Client client = new Client(clientGUI);

        while (true) {
            try {
                String[] credentials = clientGUI.getLoginCredentials();
                client.username = credentials[0]; // 设置用户名
                String password = credentials[1];

                System.out.println("Attempting to login with username: " + client.username);

                boolean loginSuccess = client.login(client.username, password);
                if (loginSuccess) {
                    System.out.println("Login successful. Initializing chat GUI...");
                    clientGUI.showChatGUI();
                    client.startMessagePolling(); // 开始轮询消息

                    while (true) {
                        String message = clientGUI.getMessage();
                        System.out.println("Received message from GUI: " + message);

                        if (message.startsWith("@@")) {
                            String command = message.substring(2).trim();
                            client.handleSystemCommand(command);
                        } else if (message.startsWith("@")) {
                            client.sendMessage(message, null);
                            clientGUI.appendMessage("Private message to " + message.substring(1, message.indexOf(' '))
                                    + ": " + message.substring(message.indexOf(' ') + 1));
                        } else {
                            client.sendMessage(message, null);
                            clientGUI.appendMessage(client.username + ": " + message);
                        }

                        if (message.equalsIgnoreCase("exit")) {
                            client.logout();
                            client.stopMessagePolling(); // 停止轮询消息
                            break;
                        }
                    }
                    break; // 退出外部循环
                } else {
                    System.out.println("Invalid credentials, please try again.");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
