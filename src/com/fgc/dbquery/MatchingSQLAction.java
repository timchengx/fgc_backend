package com.fgc.dbquery;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.json.JSONArray;

import com.fgc.data.JSON;
import com.fgc.tools.ConsoleLog;

public class MatchingSQLAction {

  private static final String SQL_GETLIST = "SELECT id FROM queue WHERE game = ?";
  private static final String SQL_JOINQUEUE = "INSERT INTO queue(id , game) VALUES (?, ?)";
  private static final String SQL_QUERYSELF = "SELECT client FROM ? WHERE client = ?";
  private static final String SQL_QUERYOTHER =
      "SELECT host, client FROM ? WHERE client = ? OR host = ?";
  private static final String SQL_WRITE_TO_GAMEQUEUE = "INSERT INTO ?(host, client) VALUES ( ?, ?)";
  private static final String SQL_DELETE_REQUEST = "DELETE FROM ? WHERE host = ? AND client = ?";
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
          gameList.put(JSON.createIDObject(userGameID));
        }
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
      PreparedStatement query = dbConnection.prepareStatement(SQL_JOINQUEUE);
      query.setString(1, userGameID);
      query.setString(2, gameID);
      query.executeUpdate();
    } catch (SQLException e) {
      ConsoleLog.sqlErrorPrint(SQL_JOINQUEUE, userGameID + ", " + gameID);
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
      if(delete) {
        query = dbConnection.prepareStatement(SQL_DELETE_REQUEST);
        query.setString(1, gameTableName);
        query.setString(2, host);
        query.setString(3, client);
        query.executeUpdate();
        return PUTREQUEST_COMPLETE;
      }
      /* check is that you been lock by other player */
      query = dbConnection.prepareStatement(SQL_QUERYSELF);
      query.setString(1, gameTableName);
      query.setString(2, host);
      queryResult = query.executeQuery();
      if(queryResult.first()) {
        Database.returnConnection(dbConnection);
        return queryResult.getString(COLUMN_HOST);
      }
      
      /* check the player you want to play with is available or not */
      query = dbConnection.prepareStatement(SQL_QUERYOTHER);
      query.setString(1, gameTableName);
      query.setString(2, client);
      query.setString(3, host);
      queryResult = query.executeQuery();
      if(queryResult.first()) {
        Database.returnConnection(dbConnection);
        return PUTREQUEST_RESULT2;
      }
      
      /* write user request into game queue table */
      query = dbConnection.prepareStatement(SQL_WRITE_TO_GAMEQUEUE);
      query.setString(1, gameTableName);
      query.setString(2, host);
      query.setString(3, client);
      query.executeUpdate();
      
    } catch (SQLException e) {
      ConsoleLog.sqlErrorPrint("when putRequest in " + gameID);
      e.printStackTrace();
    } finally {
      Database.returnConnection(dbConnection);
    }
    return PUTREQUEST_COMPLETE;
  }
}
