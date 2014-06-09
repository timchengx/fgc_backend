package com.fgc.tools;

import java.io.IOException;

import com.fgc.backend.MatchingSession;
import com.fgc.backend.SocketListener;
import com.fgc.dbquery.Database;

/* The FGC platform main class */
public class main {
  public static void main(String[] args) {
    System.setProperty("file.encoding", "UTF-8"); // json communicate by utf-8 encoding

    /* the following setting is default setting */
    int port = 5566;                // server listen port number
    String location = "localhost";  // database address
    String dbname = "fgc";          // database schema name
    String username = "fgcbackend"; // database connection username
    String pass = "backend";        // database connectnio password
    int size = 10;                  // database connection pool size
    long cleanInterval = 60000;     // connection pool clean time interval (ms)
    long matchingWaiting = 3000;    // when game is no other user, sleep time (ms)

    if (args.length == 0) {
                                 // is no argument is inputed, use the default setting
    } else if (args.length != 8) // if arguments are not enough or lack, print message
      ConsoleLog
          .println("<server port> <DB address> <schema name> <DB user> <DB password> <connection pool size> <pool clean interval(ms)> <matching list interval(ms)>");
    else {
      // if custom setting is inputed, use these setting to initialize fgc backend
      try {
        port = Integer.parseInt(args[0]);
        location = args[1];
        dbname = args[2];
        username = args[3];
        pass = args[4];
        size = Integer.parseInt(args[5]);
        cleanInterval = Long.parseLong(args[6]);
        matchingWaiting = Long.parseLong(args[7]);
      } catch (NumberFormatException e) {
        ConsoleLog.println("please enter valid number!");
        e.printStackTrace();
      }
    }
    /* initialize fgc backend */
    try {
      MatchingSession.setSleepTime(matchingWaiting);    //set wait sleep time
      
      /* setup database and connect it */
      Database.setDatabase(location, dbname, username, pass, size, cleanInterval);
      Database.startDatabase();
      
      /* start listen socket port */
      new Thread(new SocketListener(port)).start();
      
      /* initialize complete */
      ConsoleLog.println("FGC backend Ready.");
      
    } catch (IOException e) {
      ConsoleLog.errorPrint("Fail to create ServerSocket!!!");
      e.printStackTrace();
    }

  }
}
