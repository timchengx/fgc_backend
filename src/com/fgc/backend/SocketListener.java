package com.fgc.backend;

import java.io.IOException;
import java.net.ServerSocket;

import com.fgc.data.User;
import com.fgc.tools.ConsoleLog;

public class SocketListener implements Runnable {
  private ServerSocket serverSocket;

  public SocketListener(int port) throws IOException {
    serverSocket = new ServerSocket(port);
  }

  @Override
  public void run() {
    try {
      new Thread(new AuthenticationSession(new User(serverSocket.accept()))).start();
    } catch (IOException e) {
      ConsoleLog.println("First User fail to connect");
      e.printStackTrace();
    }
    try {
      new Thread(new AuthenticationSession(new User(serverSocket.accept()))).start();
    } catch (IOException e) {
      ConsoleLog.println("Second User fail to connect");
      e.printStackTrace();
    }
  }

}
