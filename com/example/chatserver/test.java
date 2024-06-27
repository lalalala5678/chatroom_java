package com.example.chatserver;

public class test {
    public static void main(String[] args) {
        ClientGUI clientGUI = new ClientGUI();
        try {
            String[] credentials = clientGUI.getLoginCredentials();
            String username = credentials[0];
            String password = credentials[1];

            // 假设我们有一个简单的认证方法：validateCredentials
            if (validateCredentials(username, password)) {
                clientGUI.showChatGUI();
                while (true) {
                    String message = clientGUI.getMessage();
                    if (message.equalsIgnoreCase("exit")) {
                        break;
                    }
                    clientGUI.appendMessage("User: " + message);
                }
            } else {
                System.out.println("Invalid credentials, please try again.");
                main(args); // 重新调用main方法以重新显示登录界面
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static boolean validateCredentials(String username, String password) {
        // 简单的验证示例，这里可以替换成你的实际验证逻辑
        return "user".equals(username) && "password".equals(password);
    }
}
