package com.fgc.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class User {
  private Socket socket;
  private BufferedReader input;
  private PrintWriter output;
  private String userName = "default";

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

  public void close() throws IOException {
    input.close();
    output.close();
    socket.close();

  }

}
