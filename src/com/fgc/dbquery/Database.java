package com.fgc.dbquery;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.fgc.tools.ConsoleLog;

/*
 * this class is handle the connection to MySQL (or MariaDB) also is a connection pool
 */
public class Database implements Runnable {
  private static String connectionURL = null;
  private static final String driverName = "com.mysql.jdbc.Driver"; // use mysql jdbc driver
  private static int connectPoolSize; // connection pool size
  private static long threadSleepTime; // interval of clean pool (ms)
  private static boolean isRunning = false; // is connection pool running?

  /*
   * connection pool implementation usingPool for saving the connection that using by process
   * currently availablePool is saving the available connection ready for process to use
   */
  private static ConcurrentLinkedQueue<Connection> usingPool;
  private static ConcurrentLinkedQueue<Connection> availablePool;

  /*
   * setup address, schema name, username and password for connect to database also setup the
   * connection pool size and interval of clean the pool
   */
  public static void setDatabase(String location, String dbname, String username, String pass,
      int size, long sleep) {
    /* address of connect to database */
    connectionURL =
        "jdbc:mysql://" + location + "/" + dbname + "?user=" + username + "&password=" + pass;

    connectPoolSize = size;
    threadSleepTime = sleep;
    /* initialize using & available pool */
    usingPool = new ConcurrentLinkedQueue<Connection>();
    availablePool = new ConcurrentLinkedQueue<Connection>();
    try {
      Class.forName(driverName).newInstance();
    } catch (Exception e) {
      ConsoleLog.errorPrint("Can't load" + driverName);
    }
  }

  /* start database connection and connection pool clean process */
  public static void startDatabase() {
    /* only allow one process to go */
    if (!isRunning)
      new Thread(new Database()).start();
    isRunning = true;
  }

  /* create a new SQL connection */
  private static Connection createConnection() {
    Connection connection = null;
    try {
      connection = DriverManager.getConnection(connectionURL);
    } catch (SQLException e) {
      ConsoleLog.errorPrint("Fail to open Database Connection");
    }
    return connection;
  }

  /*
   * get a connection from connection pool or if pool is no connection available create a new
   * connection and return
   */
  public static Connection getConnection() {
    Connection connection = availablePool.poll();
    if (connection == null) { // if poll is no connection, create a new one
      connection = createConnection();
      usingPool.add(connection);
      ConsoleLog.println("A new SQL connection has been created, current connection: "
          + (availablePool.size() + usingPool.size()));
    }
    return connection;
  }

  /* return the connection which session finish using it */
  public synchronized static void returnConnection(Connection connection) {
    if (connection != null) {
      if (usingPool.contains(connection))
        usingPool.remove(connection);
      if (!availablePool.contains(connection))
        availablePool.add(connection);
    }
  }

  /* connection pool clean procedure */
  @Override
  public void run() {
    while (true) {
      /*
       * start clean when available pool is not empty clean all available connection and recreate of
       * it
       */
      while (!availablePool.isEmpty()) {
        try {
          availablePool.remove().close();
        } catch (SQLException e) {
          ConsoleLog.errorPrint("Fail to close a SQL connection from pool");
        }
      }
      for (int i = 0; i < connectPoolSize; i++) {
        Connection conn = createConnection();
        if (conn != null)
          availablePool.add(conn);
      }

      try {
        Thread.sleep(threadSleepTime); // wait threadSleepTime before next clean
      } catch (InterruptedException e) {
        ConsoleLog.errorPrint("Connection clean thread sleep fail");
      }
    }
  }
}
