package com.fgc.backend;

import java.io.IOException;
import java.net.ServerSocket;

import com.fgc.data.User;
import com.fgc.tools.ConsoleLog;

/* this session manage to receive all socket connection who first connected to fgc */
public class SocketListener implements Runnable {
  private ServerSocket serverSocket;

  /* create server socket by correspond port */
  public SocketListener(int port) throws IOException {
    serverSocket = new ServerSocket(port);
  }

  @Override
  public void run() {
    while(true) {
      try {
        /* 
         * when a new client is connected,
         * create a new user object to save the socket connection
         * and redirect them to authentication session 
         */
        new Thread(new AuthenticationSession(new User(serverSocket.accept()))).start();
      } catch (IOException e) {
        ConsoleLog.errorPrint("A User fail to connect");
        e.printStackTrace();
      }
    }
  }

}
