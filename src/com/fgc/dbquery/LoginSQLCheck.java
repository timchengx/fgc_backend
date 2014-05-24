package com.fgc.dbquery;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import com.fgc.tools.ConsoleLog;

public class LoginSQLCheck {
  private static final String SQL_TOKEN = "SELECT token, tokenDeadline FROM user WHERE token = ?;";
  private static final String SQL_GAMEID = "SELECT COUNT(*) FROM game WHERE game = ?;";
  private static final String SQL_GETGAMENAME =
      "SELECT id FROM user JOIN avatar ON user.username = avatar.username WHERE token = ?";
  private static final String SQL_GETGAMEID = "SELECT game FROM game WHERE game = ?";
  private static final String COLUMN_VALIDTIME = "tokenDeadline";
  private static final String COLUMN_ID = "id";


  public static String login(String token, String gameID) {
    Connection dbConnection = Database.getConnection();
    String gameName = null;
    try {
      PreparedStatement query = dbConnection.prepareStatement(SQL_TOKEN);
      query.setString(1, token);
      ResultSet queryResult = query.executeQuery();
      if (queryResult.next()) {
        Timestamp expireTime = queryResult.getTimestamp(COLUMN_VALIDTIME);
        if (!expireTime.after(new java.util.Date())) {
          Database.returnConnection(dbConnection);
          return null;
        }
        query = dbConnection.prepareStatement(SQL_GAMEID);
        query.setString(1, gameID);
        queryResult = query.executeQuery();
        if (queryResult.next() && queryResult.getInt(1) != 1) {
          Database.returnConnection(dbConnection);
          return null;
        }
        query = dbConnection.prepareStatement(SQL_GETGAMENAME);
        query.setString(1, token);
        queryResult = query.executeQuery();
        if (queryResult.next()) {
          gameName = queryResult.getString(COLUMN_ID);
        }

      } else {
        Database.returnConnection(dbConnection);
        return null;
      }
    } catch (SQLException e) {
      ConsoleLog.sqlErrorPrint("token = " + token + " , gameID = " + gameID);
      e.printStackTrace();
      Database.returnConnection(dbConnection);
      return null;
    }
    Database.returnConnection(dbConnection);
    return gameName;
  }

  /* in case of someone's */
  public static String fixGameName(String gameID) {
    Connection dbConnection = Database.getConnection();
    try {
      PreparedStatement query = dbConnection.prepareStatement(SQL_GETGAMEID);
      query.setString(1, gameID);
      ResultSet queryResult = query.executeQuery();
      if (queryResult.next()) {
        gameID = queryResult.getString(1);
      }
    } catch (SQLException e) {
      ConsoleLog.sqlErrorPrint(SQL_GETGAMEID, gameID);
      e.printStackTrace();
    }
    Database.returnConnection(dbConnection);
    return gameID;
  }
}
