package com.example.chatserver;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import com.google.gson.Gson;

import java.io.*;
import java.net.*;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class ChatServer {
    private static final int SERVER_PORT = 8000;
    private static Map<String, String> userCredentialsMap = new HashMap<>();
    private static List<Map<String, String>> chatMessagesList = new ArrayList<>();
    private static Set<String> activeUsersSet = new HashSet<>();

    public static void main(String[] args) throws Exception {
        // 打印当前工作路径
        System.out.println("当前工作路径: " + Paths.get("").toAbsolutePath().toString());

        // 读取用户文件
        loadUserCredentials("users.txt");

        // 启动HTTP服务器
        HttpServer httpServer = HttpServer.create(new InetSocketAddress(SERVER_PORT), 0);
        httpServer.createContext("/login", new UserLoginHandler());
        httpServer.createContext("/send", new UserSendMessageHandler());
        httpServer.createContext("/messages", new GetChatMessagesHandler());
        httpServer.createContext("/list", new ListActiveUsersHandler());
        httpServer.createContext("/logout", new UserLogoutHandler());
        httpServer.setExecutor(null); // 创建默认的执行器
        httpServer.start();

        System.out.println("服务器启动，端口：" + SERVER_PORT);

        // 启动终端命令监听线程
        new Thread(ChatServer::terminalCommandListener).start();
    }

    private static void loadUserCredentials(String filePath) throws IOException {
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                String[] parts = line.split(" ");
                userCredentialsMap.put(parts[0], parts[1]);
                System.out.println("Loaded user: " + parts[0]); // 添加调试信息
            }
        }
    }

    private static void terminalCommandListener() {
        Scanner terminalScanner = new Scanner(System.in);
        while (true) {
            String command = terminalScanner.nextLine();
            switch (command) {
                case "listall":
                    printAllActiveUsers();
                    break;
                case "quit":
                    System.out.println("服务器即将关闭...");
                    System.exit(0);
                    break;
                default:
                    System.out.println("未知命令: " + command);
                    break;
            }
        }
    }

    private static void printAllActiveUsers() {
        System.out.println("在线用户列表:");
        for (String user : activeUsersSet) {
            System.out.println(user);
        }
    }

    static class UserLoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            addCorsHeaders(httpExchange);
            if ("POST".equals(httpExchange.getRequestMethod())) {
                Map<String, String> requestData = parseRequestBody(httpExchange.getRequestBody());

                String username = requestData.get("username");
                String password = requestData.get("password");

                String response;
                if (userCredentialsMap.containsKey(username) && userCredentialsMap.get(username).equals(password)) {
                    activeUsersSet.add(username); // 添加到在线用户列表
                    logUserActivity(username, "登录");
                    response = "{\"success\": true}";
                } else {
                    response = "{\"success\": false}";
                }

                sendJsonResponse(httpExchange, response);
            }
        }
    }

    static class UserSendMessageHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            addCorsHeaders(httpExchange);
            if ("POST".equals(httpExchange.getRequestMethod())) {
                Map<String, String> requestData = parseRequestBody(httpExchange.getRequestBody());

                String messageContent = URLDecoder.decode(requestData.get("message"), "UTF-8");
                String username = URLDecoder.decode(requestData.get("username"), "UTF-8");
                String toUser = requestData.get("toUser") != null
                        ? URLDecoder.decode(requestData.get("toUser"), "UTF-8")
                        : null;

                Map<String, String> message = new HashMap<>();
                message.put("username", username);
                message.put("message", messageContent);
                if (toUser != null) {
                    message.put("toUser", toUser);
                }

                chatMessagesList.add(message);

                String response = "{\"success\": true, \"totalMessages\": " + chatMessagesList.size() + "}";

                sendJsonResponse(httpExchange, response);
            }
        }
    }

    static class GetChatMessagesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            addCorsHeaders(httpExchange);
            if ("GET".equals(httpExchange.getRequestMethod())) {
                String query = httpExchange.getRequestURI().getQuery();
                int clientMessageCount = Integer.parseInt(query.split("=")[1]);

                List<Map<String, String>> newMessages = chatMessagesList.subList(clientMessageCount,
                        chatMessagesList.size());

                Map<String, Object> response = new HashMap<>();
                response.put("newMessages", newMessages);
                response.put("totalMessages", chatMessagesList.size());

                String jsonResponse = new Gson().toJson(response);
                sendJsonResponse(httpExchange, jsonResponse);
            }
        }
    }

    static class ListActiveUsersHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            addCorsHeaders(httpExchange);
            if ("GET".equals(httpExchange.getRequestMethod())) {
                String jsonResponse = new Gson().toJson(activeUsersSet);
                sendJsonResponse(httpExchange, jsonResponse);
            }
        }
    }

    static class UserLogoutHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            addCorsHeaders(httpExchange);
            if ("POST".equals(httpExchange.getRequestMethod())) {
                Map<String, String> requestData = parseRequestBody(httpExchange.getRequestBody());

                String username = requestData.get("username");

                String response;
                if (activeUsersSet.contains(username)) {
                    activeUsersSet.remove(username); // 从在线用户列表中移除
                    logUserActivity(username, "退出");
                    response = "{\"success\": true}";
                } else {
                    response = "{\"success\": false}";
                }

                sendJsonResponse(httpExchange, response);
            }
        }
    }

    private static Map<String, String> parseRequestBody(InputStream body) throws IOException {
        Map<String, String> requestDataMap = new HashMap<>();
        String requestBody = new BufferedReader(new InputStreamReader(body)).lines()
                .collect(Collectors.joining("\n"));
        String[] pairs = requestBody.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=");
            requestDataMap.put(keyValue[0], URLDecoder.decode(keyValue[1], "UTF-8"));
        }
        return requestDataMap;
    }

    private static void addCorsHeaders(HttpExchange httpExchange) {
        httpExchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        httpExchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        httpExchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    private static void sendJsonResponse(HttpExchange httpExchange, String response) throws IOException {
        httpExchange.getResponseHeaders().set("Content-Type", "application/json");
        httpExchange.sendResponseHeaders(200, response.getBytes().length);
        try (OutputStream os = httpExchange.getResponseBody()) {
            os.write(response.getBytes());
        }
    }

    private static void logUserActivity(String username, String activity) {
        try (FileWriter fw = new FileWriter("logs.txt", true);
                BufferedWriter bw = new BufferedWriter(fw);
                PrintWriter out = new PrintWriter(bw)) {
            out.println(username + " " + activity + " " + new Date());
        } catch (IOException e) {
            System.err.println("日志记录失败: " + e.getMessage());
        }
    }
}
