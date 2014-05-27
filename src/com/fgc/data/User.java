package com.fgc.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import com.fgc.tools.ConsoleLog;

public class User {
  private Socket socket;
  private BufferedReader input;
  private PrintWriter output;
  private String userGameName;
  private String gameID;

  public User(Socket socket) throws IOException {
    this.socket = socket;
    input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    output = new PrintWriter(socket.getOutputStream(), true);
  }

  public void send(String message) {
    output.println(message);
  }

  public String receive() throws IOException {
    return input.readLine();
  }

  public void close() {
    try {
      input.close();
      output.close();
      socket.close();
    } catch (IOException e) {
      ConsoleLog.errorPrint(userGameName + " fail to close socket");
      e.printStackTrace();
    }

  }

  public String getUserGameName() {
    return userGameName;
  }

  public String getGameID() {
    return gameID;
  }

  public void setInformation(String user, String game) {
    userGameName = user;
    gameID = game;
  }

}
