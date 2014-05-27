package com.fgc.tools;

import java.io.IOException;

import com.fgc.backend.MatchingSession;
import com.fgc.backend.SocketListener;
import com.fgc.dbquery.Database;


public class main {
  public static void main(String[] args) {
    System.setProperty("file.encoding", "UTF-8");
    if (args.length != 8)
      ConsoleLog
          .println("<server port> <DB address> <schema name> <DB user> <DB password> <connection pool size> <pool clean interval(ms)> <matching list interval(ms)>");
    else {
      try {
        int port = Integer.parseInt(args[0]);
        String location = args[1];
        String dbname = args[2];
        String username = args[3];
        String pass = args[4];
        int size = Integer.parseInt(args[5]);
        long sleep = Long.parseLong(args[6]);
        long matchingWaiting = Long.parseLong(args[7]);
        MatchingSession.setSleepTime(matchingWaiting);
        Database.setDatabase(location, dbname, username, pass, size, sleep);
        Database.startDatabase();
        new Thread(new SocketListener(port)).start();
        ConsoleLog.println("FGC backend Ready.");
      } catch (NumberFormatException e) {
        ConsoleLog.println("please enter valid number!");
        e.printStackTrace();
      } catch (IOException e) {
        ConsoleLog.errorPrint("Fail to create ServerSocket!!!");
        e.printStackTrace();
      }
    }
  }
}
