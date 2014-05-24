package com.fgc.dbquery;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.fgc.tools.ConsoleLog;

public class Database implements Runnable {
  private static String connectionURL = null;
  private static final String driverName = "com.mysql.jdbc.Driver";
  private static int connectPoolSize;
  private static long threadSleepTime;
  private static boolean isRunning = false;
  private static ConcurrentLinkedQueue<Connection> usingPool;
  private static ConcurrentLinkedQueue<Connection> availablePool;

  public static void setDatabase(String location, String dbname, String username, String pass,
      int size, long sleep) {
    connectionURL =
        "jdbc:mysql://" + location + "/" + dbname + "?user=" + username + "&password=" + pass;
    connectPoolSize = size;
    threadSleepTime = sleep;
    usingPool = new ConcurrentLinkedQueue<Connection>();
    availablePool = new ConcurrentLinkedQueue<Connection>();
    try {
      Class.forName(driverName).newInstance();
      for (int i = 0; i < connectPoolSize; i++)
        availablePool.add(createConnection());
    } catch (Exception e) {
      ConsoleLog.errorPrint("Can't load" + driverName);
      e.printStackTrace();
      System.exit(-1);
    }
  }

  public static void startCleanPool() {
    if (!isRunning)
      new Thread(new Database()).start();
    isRunning = true;
  }

  private static Connection createConnection() {
    Connection connection = null;
    try {
      connection = DriverManager.getConnection(connectionURL);
    } catch (SQLException e) {
      ConsoleLog.errorPrint("Fail to open Database Connection");
      e.printStackTrace();
    }
    return connection;
  }

  public static Connection getConnection() {
    Connection connection = availablePool.poll();
    if (connection == null) {
      connection = createConnection();
      usingPool.add(connection);
      ConsoleLog.println("created a SQL connection, current connection: "
          + (availablePool.size() + usingPool.size()));
    }
    return connection;
  }

  public static void returnConnection(Connection connection) {
    if (connection != null) {
      usingPool.remove(connection);
      availablePool.add(connection);
    }
  }

  @Override
  public void run() {
    while (true) {
      while (availablePool.size() > connectPoolSize)
        try {
          availablePool.remove().close();
          ConsoleLog.println("Closed a SQL connection from pool");
        } catch (SQLException e) {
          ConsoleLog.errorPrint("Fail to close a SQL connection from pool");
          e.printStackTrace();
        }
      try {
        Thread.sleep(threadSleepTime);
      } catch (InterruptedException e) {
        ConsoleLog.errorPrint("Connection clean thread sleep fail");
        e.printStackTrace();
      }
    }
  }
}
