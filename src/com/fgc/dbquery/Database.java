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
    } catch (Exception e) {
      ConsoleLog.errorPrint("Can't load" + driverName);
      e.printStackTrace();
      System.exit(-1);
    }
  }

  public static void startDatabase() {
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
      System.exit(-1);
    }
    return connection;
  }

  public static Connection getConnection() {
    Connection connection = availablePool.poll();
    if (connection == null) {
      connection = createConnection();
      usingPool.add(connection);
      ConsoleLog.println("A new SQL connection has been created, current connection: "
          + (availablePool.size() + usingPool.size()));
    }
    return connection;
  }

  public synchronized static void returnConnection(Connection connection) {
    if (connection != null) {
      if (usingPool.contains(connection))
        usingPool.remove(connection);
      if (!availablePool.contains(connection))
        availablePool.add(connection);
    }
  }

  @Override
  public void run() {
    while (true) {
      while (!availablePool.isEmpty()) {
        try {
          availablePool.remove().close();
        } catch (SQLException e) {
          ConsoleLog.errorPrint("Fail to close a SQL connection from pool");
          e.printStackTrace();
        }
      }
      for (int i = 0; i < connectPoolSize; i++)
        availablePool.add(createConnection());
      try {
        Thread.sleep(threadSleepTime);
      } catch (InterruptedException e) {
        ConsoleLog.errorPrint("Connection clean thread sleep fail");
        e.printStackTrace();
      }
    }
  }
}
