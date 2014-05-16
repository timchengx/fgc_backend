package com.fgc.tools;

import java.io.IOException;

import com.fgc.backend.SocketListener;


public class main {
  public static void main(String[] args) {
    System.setProperty("file.encoding", "UTF-8");
    if (args.length < 1)
      ConsoleLog.println("please enter port number");
    else {
      try {
        int port = Integer.parseInt(args[0]);
        new Thread(new SocketListener(port)).start();
      } catch (NumberFormatException e) {
        ConsoleLog.println("please enter port number");
      } catch (IOException e) {
        ConsoleLog.errorPrint("Fail to create ServerSocket!!!");
        e.printStackTrace();
      }
    }
  }
}
