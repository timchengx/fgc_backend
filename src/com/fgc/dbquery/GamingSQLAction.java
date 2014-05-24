package com.fgc.dbquery;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;

import com.fgc.tools.ConsoleLog;


public class GamingSQLAction {
  private static String SQL_REMOVE_QUEUE = "DELETE FROM queue WHERE id = ? AND game = ?";
  private static String SQL_CREATE_RECORD =
      "INSERT INTO game_record(game, id1, id2) VALUES (?, ?, ?)";
  private static String SQL_ADD_RECORD =
      "UPDATE game_record SET record = CONCAT(record, ?) where rid = ?";
  private static String SQL_END_RECORD = "UPDATE game_record SET endTime = ? WHERE rid = ?";
  private static String SQL_REMOVE_GAME_QUEUE = "DELETE FROM ? WHERE host = ? AND client = ?";
  private static String SQL_UPDATE_GAME_COLUMN = "UPDATE stats SET ? = ? WHERE id = ? AND game = ?";
  private static String SQL_QUERY_GAME_COLUMN = "SELECT ? FROM stats WHERE id = ? AND game = ?";
  private static String QUEUE_TABLE = "queue_";
  private static String COLUMN_RID = "rid";
  private static String COLUMN_TIME = "time";
  private static String COLUMN_WIN = "winTime";
  private static String COLUMN_LOSE = "lostTime";

  public static void removeFromQueue(String gameID, String userID) {
    Connection dbConnection = Database.getConnection();
    try {
      PreparedStatement query = dbConnection.prepareStatement(SQL_REMOVE_QUEUE);
      query.setString(1, userID);
      query.setString(2, gameID);
      query.executeUpdate();
    } catch (SQLException e) {
      ConsoleLog.sqlErrorPrint(SQL_REMOVE_QUEUE, userID + ", " + gameID);
      e.printStackTrace();
    } finally {
      Database.returnConnection(dbConnection);
    }
  }

  public static int createGameRecord(String gameID, String firstPlay, String secondPlay) {
    Connection dbConnection = Database.getConnection();
    int rid = -1;
    try {
      PreparedStatement query =
          dbConnection.prepareStatement(SQL_CREATE_RECORD, PreparedStatement.RETURN_GENERATED_KEYS);
      query.setString(1, gameID);
      query.setString(2, firstPlay);
      query.setString(3, secondPlay);
      query.executeUpdate();
      ResultSet queryResult = query.getGeneratedKeys();
      if (queryResult.first()) {
        rid = queryResult.getInt(COLUMN_RID);
      }
    } catch (SQLException e) {
      ConsoleLog.sqlErrorPrint(SQL_ADD_RECORD, gameID + ", " + firstPlay + ", " + secondPlay);
      e.printStackTrace();
    } finally {
      Database.returnConnection(dbConnection);
    }
    return rid;
  }

  public static void appendGameRecord(int rid, String data) {
    Connection dbConnection = Database.getConnection();
    try {
      PreparedStatement query = dbConnection.prepareStatement(SQL_ADD_RECORD);
      query.setString(1, data);
      query.setInt(2, rid);
      query.executeUpdate();
    } catch (SQLException e) {
      ConsoleLog.sqlErrorPrint(SQL_ADD_RECORD, data + ", " + rid);
      e.printStackTrace();
    } finally {
      Database.returnConnection(dbConnection);
    }
  }

  public static void finishGame(int rid) {
    Connection dbConnection = Database.getConnection();
    Timestamp time = new Timestamp(new Date().getTime());
    try {
      PreparedStatement query = dbConnection.prepareStatement(SQL_END_RECORD);
      query.setTimestamp(1, time);
      query.setInt(2, rid);
      query.executeUpdate();
    } catch (SQLException e) {
      ConsoleLog.sqlErrorPrint(SQL_REMOVE_QUEUE, time.toString() + ", " + rid);
      e.printStackTrace();
    } finally {
      Database.returnConnection(dbConnection);
    }
  }

  public static void removeFromGameQueue(String gameID, String host, String client) {
    Connection dbConnection = Database.getConnection();
    try {
      PreparedStatement query = dbConnection.prepareStatement(SQL_REMOVE_GAME_QUEUE);
      query.setString(1, QUEUE_TABLE + gameID);
      query.setString(2, host);
      query.setString(3, client);
      query.executeUpdate();
    } catch (SQLException e) {
      ConsoleLog
          .sqlErrorPrint(SQL_REMOVE_QUEUE, QUEUE_TABLE + gameID + ", " + host + ", " + client);
      e.printStackTrace();
    } finally {
      Database.returnConnection(dbConnection);
    }
  }

  public static void addGameCount(String gameID, String userID) {
    Connection dbConnection = Database.getConnection();
    PreparedStatement query;
    ResultSet queryResult;
    int time;

    try {
      query = dbConnection.prepareStatement(SQL_QUERY_GAME_COLUMN);
      query.setString(1, COLUMN_TIME);
      query.setString(2, userID);
      query.setString(3, gameID);
      queryResult = query.executeQuery();
      if (queryResult.first()) {
        time = queryResult.getInt(COLUMN_TIME);
        time++;
        query = dbConnection.prepareStatement(SQL_UPDATE_GAME_COLUMN);
        query.setString(1, COLUMN_TIME);
        query.setInt(2, time);
        query.setString(3, userID);
        query.setString(4, gameID);
        query.executeUpdate();
      }
    } catch (SQLException e) {
      ConsoleLog.errorPrint("in addGameCount with gameID = " + gameID + ", userID = " + userID);
      e.printStackTrace();
    } finally {
      Database.returnConnection(dbConnection);
    }

  }

  public static void setGameResult(String gameID, String userID, boolean win) {
    Connection dbConnection = Database.getConnection();
    PreparedStatement query;
    ResultSet queryResult;
    int time;
    String changeColumn;
    
    if (win)
      changeColumn = COLUMN_WIN;
    else
      changeColumn = COLUMN_LOSE;

    try {
      query = dbConnection.prepareStatement(SQL_QUERY_GAME_COLUMN);
      query.setString(1, changeColumn);
      query.setString(2, userID);
      query.setString(3, gameID);
      queryResult = query.executeQuery();
      if (queryResult.first()) {
        time = queryResult.getInt(changeColumn);
        time++;
        query = dbConnection.prepareStatement(SQL_UPDATE_GAME_COLUMN);
        query.setString(1, changeColumn);
        query.setInt(2, time);
        query.setString(3, userID);
        query.setString(4, gameID);
        query.executeUpdate();
      }
    } catch (SQLException e) {
      ConsoleLog.errorPrint("in setGameResult with gameID = " + gameID + ", userID = " + userID
          + "is this user win? " + win);
      e.printStackTrace();
    } finally {
      Database.returnConnection(dbConnection);
    }
  }
}
