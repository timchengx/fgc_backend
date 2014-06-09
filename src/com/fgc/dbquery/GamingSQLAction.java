package com.fgc.dbquery;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;

import com.fgc.tools.ConsoleLog;

/* 
 * this part handles the game step data save to database for online viewer 
 */
public class GamingSQLAction {
  private static String SQL_REMOVE_QUEUE = "DELETE FROM queue WHERE id = ? AND game = ?";
  private static String SQL_CREATE_RECORD =
      "INSERT INTO game_record(game, id1, id2) VALUES (?, ?, ?)";
  private static String SQL_ADD_RECORD =
      "UPDATE game_record SET record = ? where rid = ?";
  private static String SQL_QUERY_RECORD = "SELECT record from game_record WHERE rid = ?";
  private static String SQL_END_RECORD = "UPDATE game_record SET endTime = ? WHERE rid = ?";
  private static String SQL_REMOVE_GAME_QUEUE =
      "DELETE FROM $tableName WHERE host = ? AND client = ?";
  private static String SQL_UPDATE_GAME_COLUMN =
      "UPDATE stats SET $columnName = ? WHERE id = ? AND game = ?";
  private static String SQL_QUERY_GAME_COLUMN =
      "SELECT $columnName FROM stats WHERE id = ? AND game = ?";
  private static String SQL_FIND_RID =
      "select rid from game_record where id1 = ? AND id2 = ? order by startTime DESC limit 1";
  private static String QUEUE_TABLE = "queue_";
  private static String COLUMN_RID = "rid";
  private static String COLUMN_TIME = "time";
  private static String COLUMN_WIN = "winTime";
  private static String COLUMN_LOSE = "loseTime";
  private static String COLUMN_RECORD = "record";
  private static String REPLACE_TABLE = "$tableName";
  private static String REPLACE_COLUMN = "$columnName";

  /* invoke when game is started, no longer accept other's invite */
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

  /* 
   * create game step data save entry
   * firstPlay stand for player who take's first lead
   * secondPlay stand for player who make the move after first player
   */
  public static int createGameRecord(String gameID, String firstPlay, String secondPlay) {
    Connection dbConnection = Database.getConnection();
    int rid = -1;
    try {
      
      /* create game record */
      PreparedStatement query =
          dbConnection.prepareStatement(SQL_CREATE_RECORD);
      query.setString(1, gameID);
      query.setString(2, firstPlay);
      query.setString(3, secondPlay);
      query.executeUpdate();
      
      /* lookup room id that correspond to the game record we just create before */
      query = dbConnection.prepareStatement(SQL_FIND_RID);
      query.setString(1, firstPlay);
      query.setString(2, secondPlay);
      ResultSet queryResult = query.executeQuery();
      if (queryResult.first()) {
        rid = queryResult.getInt(COLUMN_RID);
      }
    } catch (SQLException e) {
      ConsoleLog.sqlErrorPrint(SQL_CREATE_RECORD, gameID + ", " + firstPlay + ", " + secondPlay);
      e.printStackTrace();
    } finally {
      Database.returnConnection(dbConnection);
    }
    return rid;
  }
  /* invoke when game room make a new step data, write to SQL */
  public static void appendGameRecord(int rid, String data) {
    Connection dbConnection = Database.getConnection();
    String oldData = "";
    try {
      /* take out previous data first */
      PreparedStatement query = dbConnection.prepareStatement(SQL_QUERY_RECORD);
      query.setInt(1, rid);
      ResultSet queryResult = query.executeQuery();
      if(queryResult.first())
        oldData = queryResult.getString(COLUMN_RECORD);
      /* write to same entry, using append way to add it */
      query = dbConnection.prepareStatement(SQL_ADD_RECORD);
      if(oldData == null || oldData.isEmpty())  // if this is a brand new write
        query.setString(1, data);
      else
        query.setString(1, oldData + "\n" + data);
      query.setInt(2, rid);
      query.executeUpdate();
    } catch (SQLException e) {
      ConsoleLog.sqlErrorPrint(SQL_ADD_RECORD, data + ", " + rid);
      e.printStackTrace();
    } finally {
      Database.returnConnection(dbConnection);
    }
  }

  /* invoke when game is finish, write end time to SQL entry */
  public static void finishGame(int rid) {
    Connection dbConnection = Database.getConnection();
    Timestamp time = new Timestamp(new Date().getTime());   // get current time
    try {
      /* write end time to entiry */
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

  /* 
   * remove matching entries from gamequeue, invoke when game is finish
   * (the reason of remove in finsh is because 2 player can't be invited before
   * they finish game)
   */
  public static void removeFromGameQueue(String gameID, String host, String client) {
    Connection dbConnection = Database.getConnection();
    try {
      String sqlCommand = SQL_REMOVE_GAME_QUEUE.replace(REPLACE_TABLE, QUEUE_TABLE + gameID);
      PreparedStatement query = dbConnection.prepareStatement(sqlCommand);
      query.setString(1, host);
      query.setString(2, client);
      query.executeUpdate();
    } catch (SQLException e) {
      ConsoleLog
          .sqlErrorPrint(SQL_REMOVE_QUEUE, QUEUE_TABLE + gameID + ", " + host + ", " + client);
      e.printStackTrace();
    } finally {
      Database.returnConnection(dbConnection);
    }
  }

  /* add player's play count of correspond game */
  public static void addGameCount(String gameID, String userID) {
    Connection dbConnection = Database.getConnection();
    PreparedStatement query;
    ResultSet queryResult;
    String sqlCommand;
    int time;

    try {
      /* lookup current play count first */
      sqlCommand = SQL_QUERY_GAME_COLUMN.replace(REPLACE_COLUMN, COLUMN_TIME);
      query = dbConnection.prepareStatement(sqlCommand);
      query.setString(1, userID);
      query.setString(2, gameID);
      queryResult = query.executeQuery();
      if (queryResult.first()) {
        time = queryResult.getInt(COLUMN_TIME);
        time++; // add play count
        /* write new play count to database */
        sqlCommand = SQL_UPDATE_GAME_COLUMN.replace(REPLACE_COLUMN, COLUMN_TIME);
        query = dbConnection.prepareStatement(sqlCommand);
        query.setInt(1, time);
        query.setString(2, userID);
        query.setString(3, gameID);
        query.executeUpdate();
      }
    } catch (SQLException e) {
      ConsoleLog.errorPrint("in addGameCount with gameID = " + gameID + ", userID = " + userID);
      e.printStackTrace();
    } finally {
      Database.returnConnection(dbConnection);
    }

  }

  /* set player's match result by correspond game, win or lose */
  public static void setUserGameStats(String gameID, String userID, boolean win) {
    Connection dbConnection = Database.getConnection();
    PreparedStatement query;
    ResultSet queryResult;
    int time;
    String changeColumn;
    String sqlCommand;

    /* determine which column should lookup and update(win or lost) */
    if (win)
      changeColumn = COLUMN_WIN;
    else
      changeColumn = COLUMN_LOSE;

    try {
      /* lookup correspond column's value first */
      sqlCommand = SQL_QUERY_GAME_COLUMN.replace(REPLACE_COLUMN, changeColumn);
      query = dbConnection.prepareStatement(sqlCommand);
      query.setString(1, userID);
      query.setString(2, gameID);
      queryResult = query.executeQuery();
      if (queryResult.first()) {
        time = queryResult.getInt(changeColumn);
        time++; // add win or lost count
        /* write new value to database */
        sqlCommand = SQL_UPDATE_GAME_COLUMN.replace(REPLACE_COLUMN, changeColumn);
        query = dbConnection.prepareStatement(sqlCommand);
        query.setInt(1, time);
        query.setString(2, userID);
        query.setString(3, gameID);
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
