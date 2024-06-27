# Chat Server and Client

## 概述

这是一个简单的基于 Java 的聊天室系统，包括服务器端和客户端实现。客户端通过图形用户界面（GUI）与服务器通信，支持广播消息、私聊消息以及系统命令。

## 功能

- **登录**：用户可以使用用户名和密码登录。
- **发送消息**：用户可以发送广播消息和私聊消息。
- **系统命令**：支持列出在线用户、切换匿名模式、退出系统等命令。
- **消息轮询**：客户端每 0.5 秒从服务器获取一次新消息。

## 目录结构

```
E:.
├───com
│   └───example
│       └───chatserver
│           ├───ChatServer.java
│           ├───Client.java
│           ├───ClientGUI.java
│           └───test.java
├───libs
│   └───gson-2.11.0.jar
├───logs.txt
├───README.md
└───users.txt
```

## 快速开始

### 克隆项目

首先，克隆项目到本地：

```sh
git clone <repository_url>
cd <repository_directory>
```

### 编译

在项目根目录下运行以下命令编译所有 Java 文件：

```sh
javac -cp ".;libs\gson-2.11.0.jar" com\example\chatserver\*.java
```

### 启动服务器

编译完成后，运行以下命令启动服务器：

```sh
java -cp ".;libs\gson-2.11.0.jar" com.example.chatserver.ChatServer
```

### 启动客户端

在另一个终端窗口中，运行以下命令启动客户端：

```sh
java -cp ".;libs\gson-2.11.0.jar" com.example.chatserver.Client
```

## 代码逻辑简述

### ChatServer.java

- **端口**：服务器监听 8000 端口。
- **用户认证**：从 `users.txt` 文件加载用户凭证。
- **消息处理**：处理登录、发送消息、获取消息、列出在线用户、用户登出等请求。
- **服务器系统命令**：
  - `listall`：列出全部用户。
  - `quit`：退出系统。

### Client.java

- **登录**：向服务器发送登录请求并处理响应。
- **发送消息**：通过 POST 请求发送广播或私聊消息。
- **获取消息**：每 0.5 秒从服务器获取一次新消息，并根据消息类型进行过滤和显示。
- **客户端系统命令**：
  - `@@list`：列出当前在线用户。
  - `@@quit`：退出系统。
  - `@@showanonymous`：显示当前聊天方式是否为匿名。
  - `@@anonymous`：切换聊天方式，即使用匿名聊天还是实名聊天。

### ClientGUI.java

- **登录界面**：用户输入用户名和密码进行登录。
- **聊天界面**：用户可以输入消息并发送，查看聊天记录。

## 使用方法

1. **启动服务器**：按照上述步骤启动服务器。
2. **启动客户端**：按照上述步骤启动客户端。
3. **登录**：在客户端的登录界面输入用户名和密码，点击登录。
4. **发送消息**：
   - **广播消息**：直接输入消息并发送，所有用户可见。
   - **私聊消息**：以 `@username` 开头输入私聊消息，仅目标用户可见。
5. **系统命令**：
   - **客户端系统命令**：
     - `@@list`：列出当前在线用户。
     - `@@quit`：退出系统。
     - `@@showanonymous`：显示当前聊天方式是否为匿名。
     - `@@anonymous`：切换聊天方式，即使用匿名聊天还是实名聊天。
   - **服务器系统命令**：
     - `listall`：列出全部用户。
     - `quit`：退出系统。

## 注意事项

请确保在运行服务器前正确配置 `users.txt` 文件，以便用户能够正常登录。如果在编译过程中遇到过时的 API 警告，可以使用 `-Xlint:deprecation` 和 `-Xlint:unchecked` 选项重新编译以查看详细信息：

```sh
javac -cp ".;libs\gson-2.11.0.jar" -Xlint:deprecation -Xlint:unchecked com\example\chatserver\*.java
```

---
