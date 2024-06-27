package com.example.chatserver;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ClientGUI {
    private JFrame loginFrame;
    private JFrame chatFrame;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JTextArea chatArea;
    private JTextField messageField;
    private String username;
    private String password;
    private final Object lock = new Object();
    private final Object messageLock = new Object();
    private String message;

    public ClientGUI() {
        initializeLoginGUI();
    }

    private void initializeLoginGUI() {
        loginFrame = createFrame("Login", 400, 300);

        JPanel panel = createGradientPanel();
        panel.setLayout(new GridLayout(4, 1, 10, 10));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel titleLabel = createLabel("Welcome to Chat Room", 24, Color.WHITE);
        panel.add(titleLabel);

        usernameField = createTextField();
        panel.add(createLabeledPanel("Username:", usernameField));

        passwordField = createPasswordField();
        panel.add(createLabeledPanel("Password:", passwordField));

        JButton loginButton = createStyledButton("Login");
        loginButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                handleLogin();
            }
        });
        panel.add(loginButton);

        loginFrame.add(panel, BorderLayout.CENTER);
        loginFrame.setVisible(true);
    }

    private void handleLogin() {
        username = usernameField.getText();
        password = new String(passwordField.getPassword());
        if (!username.isEmpty() && !password.isEmpty()) {
            synchronized (lock) {
                lock.notify();
            }
            loginFrame.setVisible(false);
        } else {
            showErrorMessage("Username and Password cannot be empty");
        }
    }

    private void initializeChatGUI() {
        chatFrame = createFrame("Chat - " + username, 600, 700);

        JPanel mainPanel = new JPanel(new BorderLayout());

        chatArea = createTextArea();
        mainPanel.add(new JScrollPane(chatArea), BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout());
        messageField = createTextField();
        inputPanel.add(messageField, BorderLayout.CENTER);

        JButton sendButton = createStyledButton("Send");
        sendButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                handleSendMessage();
            }
        });
        inputPanel.add(sendButton, BorderLayout.EAST);

        mainPanel.add(inputPanel, BorderLayout.SOUTH);

        chatFrame.add(mainPanel, BorderLayout.CENTER);
        chatFrame.setVisible(true);
    }

    private void handleSendMessage() {
        synchronized (messageLock) {
            message = messageField.getText();
            messageField.setText("");
            messageLock.notify();
        }
    }

    private JFrame createFrame(String title, int width, int height) {
        JFrame frame = new JFrame(title);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(width, height);
        frame.setLayout(new BorderLayout());
        return frame;
    }

    private JPanel createGradientPanel() {
        return new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                GradientPaint gp = new GradientPaint(0, 0, new Color(66, 135, 245), getWidth(), getHeight(),
                        new Color(25, 25, 112));
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
    }

    private JLabel createLabel(String text, int fontSize, Color color) {
        JLabel label = new JLabel(text, JLabel.CENTER);
        label.setFont(new Font("Arial", Font.BOLD, fontSize));
        label.setForeground(color);
        return label;
    }

    private JTextField createTextField() {
        JTextField textField = new JTextField();
        textField.setFont(new Font("Arial", Font.PLAIN, 16));
        textField.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
        return textField;
    }

    private JPasswordField createPasswordField() {
        JPasswordField passwordField = new JPasswordField();
        passwordField.setFont(new Font("Arial", Font.PLAIN, 16));
        passwordField.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
        return passwordField;
    }

    private JTextArea createTextArea() {
        JTextArea textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setFont(new Font("Arial", Font.PLAIN, 16));
        textArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        textArea.setBackground(new Color(240, 248, 255));
        return textArea;
    }

    private JPanel createLabeledPanel(String labelText, JComponent component) {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel label = new JLabel(labelText);
        label.setFont(new Font("Arial", Font.PLAIN, 16));
        label.setForeground(Color.WHITE);
        panel.add(label, BorderLayout.WEST);
        panel.add(component, BorderLayout.CENTER);
        return panel;
    }

    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 16));
        button.setBackground(new Color(30, 144, 255));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(100, 149, 237));
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(30, 144, 255));
            }
        });
        return button;
    }

    private void showErrorMessage(String message) {
        JOptionPane.showMessageDialog(loginFrame, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public String[] getLoginCredentials() throws InterruptedException {
        synchronized (lock) {
            lock.wait();
        }
        return new String[] { username, password };
    }

    public void showChatGUI() {
        if (username != null && password != null) {
            initializeChatGUI();
        }
    }

    public void appendMessage(String message) {
        chatArea.append(message + "\n");
    }

    public String getMessage() throws InterruptedException {
        synchronized (messageLock) {
            messageLock.wait();
        }
        return message;
    }

    public int getDisplayedMessageCount() {
        return chatArea.getLineCount();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new ClientGUI();
            }
        });
    }
}
