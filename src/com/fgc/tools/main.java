package com.fgc.tools;

import java.io.IOException;

import com.fgc.backend.SocketListener;


public class main {
  public static void main(String[] args) {
    System.setProperty("file.encoding", "UTF-8");
    if (args.length < 1)
      ConsoleLog.println("need parameter.");
    else {
      try {
        int port = Integer.parseInt(args[0]);
        new Thread(new SocketListener(port)).start();
      } catch (NumberFormatException e) {
        ConsoleLog.println("format error.");
      } catch (IOException e) {
        ConsoleLog.errorPrint("Fail to create ServerSocket!!!");
        e.printStackTrace();
      }
    }
  }
}
