package com.fgc.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import com.fgc.tools.ConsoleLog;

/* 
 * this class is design for saving a client's 
 * all information 
 */
public class User {
  private Socket socket;        // client's socket
  private BufferedReader input; // input pipe
  private PrintWriter output;   // output pipe
  private String userGameName;  // client's game nickname
  private String gameID;        // game that client is playing

  /* constructor, create pipeline */
  public User(Socket socket) throws IOException {
    this.socket = socket;
    input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    output = new PrintWriter(socket.getOutputStream(), true);
  }

  /* send JSON message to client */
  public void send(String message) {
    output.println(message);
  }

  /* receive JSON message to client */
  public String receive() throws IOException {
    return input.readLine();
  }

  /* close client's pipeline and socket connection */
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

  /* get nickname that client using in game */
  public String getUserGameName() {
    return userGameName;
  }

  /* get gameID that client is playing */
  public String getGameID() {
    return gameID;
  }

  /*
   * when login succeed, the method will be invoked to set client's
   * nickname in game and the gameID that its playing
   */
  public void setInformation(String user, String game) {
    userGameName = user;
    gameID = game;
  }

}
