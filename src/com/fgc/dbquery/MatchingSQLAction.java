package com.fgc.dbquery;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.json.JSONArray;

import com.fgc.tools.ConsoleLog;
import com.fgc.tools.FGCJSON;

public class MatchingSQLAction {

  private static final String SQL_GETLIST = "SELECT id FROM queue WHERE game = ?";
  private static final String SQL_JOIN_QUEUE = "INSERT INTO queue(id , game) VALUES (?, ?)";
  private static final String SQL_LEAVE_QUEUE = "DELETE FROM queue WHERE id = ? AND game = ?";
  private static final String SQL_QUERY_QUEUE = "SELECT id FROM queue WHERE id = ? AND game = ?";
  private static final String SQL_QUERY_SELF =
      "SELECT host, client FROM $gameTable WHERE client = ?";
  private static final String SQL_QUERY_REQUEST =
      "SELECT host, client FROM $gameTable WHERE client = ? OR host = ?";
  private static final String SQL_WRITE_REQUEST =
      "INSERT INTO $gameTable(host, client) VALUES ( ?, ?)";
  private static final String SQL_DELETE_REQUEST =
      "DELETE FROM $gameTable WHERE host = ? AND client = ?";
  private static final String SQL_DELETE_REQUEST_BY_CLIENT = "DELETE FROM $gameTable WHERE client = ?";
  private static final String QUEUE_TABLE = "queue_";
  private static final String COLUMN_HOST = "host";
  /* for unique, random gen from UUID class */
  public static final String PUTREQUEST_RESULT0 = "6c7c8a4b-b2d0-4576-b048-74c8f7c76d61";
  public static final String PUTREQUEST_COMPLETE = "2fc55d0f-2565-457c-8569-70eb37317c27";
  public static final String PUTREQUEST_RESULT2 = "52c0492b-fa8a-4f90-bd7f-44a84858ccd4";
  private String gameID;
  private String gameTableName;
  private JSONArray gameList;

  public MatchingSQLAction(String game) {
    gameID = game;
    gameTableName = QUEUE_TABLE + game;
    getList();
  }

  public JSONArray getList() {
    String userGameID;
    Connection dbConnection = Database.getConnection();
    try {
      PreparedStatement query = dbConnection.prepareStatement(SQL_GETLIST);
      query.setString(1, gameID);
      ResultSet queryResult = query.executeQuery();
      gameList = new JSONArray();
      while (queryResult.next()) {
        userGameID = queryResult.getString(1);
        gameList.put(FGCJSON.createIDObject(userGameID));
      }
      ConsoleLog.gameIDPrint(gameID, "there have " + gameList.length() + " people matching");
    } catch (SQLException e) {
      ConsoleLog.sqlErrorPrint(SQL_GETLIST, gameID);
      e.printStackTrace();
    } finally {
      Database.returnConnection(dbConnection);
    }
    return gameList;
  }

  public void joinGame(String userGameID) {
    Connection dbConnection = Database.getConnection();
    try {
      PreparedStatement query = dbConnection.prepareStatement(SQL_JOIN_QUEUE);
      query.setString(1, userGameID);
      query.setString(2, gameID);
      query.executeUpdate();
    } catch (SQLException e) {
      ConsoleLog.sqlErrorPrint(SQL_JOIN_QUEUE, userGameID + ", " + gameID);
      e.printStackTrace();
    } finally {
      Database.returnConnection(dbConnection);
    }
  }

  public synchronized String putRequest(String host, String client, boolean delete) {
    Connection dbConnection = Database.getConnection();
    try {
      PreparedStatement query;
      ResultSet queryResult;
      String sqlCommand;

      if (client == null) {
        String result = null;
        query = dbConnection.prepareStatement(SQL_LEAVE_QUEUE);
        query.setString(1, host);
        query.setString(2, gameID);
        query.executeUpdate();
        
        sqlCommand = SQL_QUERY_SELF.replace("$gameTable", gameTableName);
        query = dbConnection.prepareStatement(sqlCommand);
        query.setString(1, host);
        queryResult = query.executeQuery();
        if (queryResult.first())
          result = queryResult.getString(COLUMN_HOST);
        
        sqlCommand = SQL_DELETE_REQUEST_BY_CLIENT.replace("$gameTable", gameTableName);
        query = dbConnection.prepareStatement(sqlCommand);
        query.setString(1, host);
        query.executeUpdate();
        
        return result;
      }

      else if (delete) {
        sqlCommand = SQL_DELETE_REQUEST.replace("$gameTable", gameTableName);
        query = dbConnection.prepareStatement(sqlCommand);
        query.setString(1, host);
        query.setString(2, client);
        query.executeUpdate();
        return PUTREQUEST_COMPLETE;
      }

      /* check is that you been lock by other player */
      sqlCommand = SQL_QUERY_SELF.replace("$gameTable", gameTableName);
      query = dbConnection.prepareStatement(sqlCommand);
      query.setString(1, host);
      queryResult = query.executeQuery();
      if (queryResult.first()) {
        ConsoleLog.gameIDPrint(gameID, host + " send request to " + client + " failed. (" + queryResult.getString(COLUMN_HOST) + " send request first!)");
        return queryResult.getString(COLUMN_HOST);
      }
      

      /* check the player you want to play with is available or not */
      sqlCommand = SQL_QUERY_REQUEST.replace("$gameTable", gameTableName);
      query = dbConnection.prepareStatement(sqlCommand);
      query.setString(1, client);
      query.setString(2, host);
      queryResult = query.executeQuery();
      if (queryResult.first()) {
        ConsoleLog.gameIDPrint(gameID, host + " send request to " + client + "failed. (not available)");
        return PUTREQUEST_RESULT2;
      }
      
      query = dbConnection.prepareStatement(SQL_QUERY_QUEUE);
      query.setString(1, client);
      query.setString(2, gameID);
      queryResult = query.executeQuery();
      if(!queryResult.first()) {
        ConsoleLog.gameIDPrint(gameID, host + " send request to " + client + "failed. (disconnected.)");
        return PUTREQUEST_RESULT2;
      }

      /* write user request into game queue table */
      sqlCommand = SQL_WRITE_REQUEST.replace("$gameTable", gameTableName);
      query = dbConnection.prepareStatement(sqlCommand);
      query.setString(1, host);
      query.setString(2, client);
      query.executeUpdate();

    } catch (SQLException e) {
      ConsoleLog.sqlErrorPrint("when putRequest in " + gameID);
      e.printStackTrace();
    } finally {
      Database.returnConnection(dbConnection);
    }
    ConsoleLog.gameIDPrint(gameID, host + " send request to " + client);
    return PUTREQUEST_COMPLETE;
  }
}
