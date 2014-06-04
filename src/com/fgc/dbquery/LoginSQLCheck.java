package com.fgc.dbquery;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import com.fgc.tools.ConsoleLog;
import com.fgc.tools.LoginFailException;

public class LoginSQLCheck {
  private static final String SQL_TOKEN = "SELECT token, tokenDeadline FROM user WHERE token = ?;";
  private static final String SQL_GAMEID = "SELECT COUNT(*) FROM game WHERE game = ?;";
  private static final String SQL_GETGAMENAME =
      "SELECT id FROM user JOIN avatar ON user.username = avatar.username WHERE token = ? AND game = ?";
  private static final String SQL_GETGAMEID = "SELECT game FROM game WHERE game = ?";
  private static final String SQL_DELETE_TOKEN = "UPDATE user SET token = NULL , tokenDeadline = NULL WHERE token = ?";
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
        if (!expireTime.after(new java.util.Date()))
          throw new LoginFailException("token expired");

        query = dbConnection.prepareStatement(SQL_GAMEID);
        query.setString(1, gameID);
        queryResult = query.executeQuery();
        if (!queryResult.first())
          throw new LoginFailException("can't found gameID");
        if (queryResult.getInt(1) != 1)
          throw new LoginFailException("duplicate gameID");

        query = dbConnection.prepareStatement(SQL_GETGAMENAME);
        query.setString(1, token);
        query.setString(2, gameID);
        queryResult = query.executeQuery();
        if (queryResult.next())
          gameName = queryResult.getString(COLUMN_ID);
        else
          throw new LoginFailException("user didn't have game account");
        
        //加上多重登入檢查！
        
        query = dbConnection.prepareStatement(SQL_DELETE_TOKEN);
        query.setString(1, token);
        query.executeUpdate();

      } else {
        throw new LoginFailException("can't found token");
      }
    } catch (SQLException e) {
      ConsoleLog.sqlErrorPrint("token = " + token + " , gameID = " + gameID);
      e.printStackTrace();
    } catch (LoginFailException e) {
      ConsoleLog.println("A user using " + token + " want to get in " + gameID + " fail. ("
          + e.getMessage() + ")");
    } finally {
      Database.returnConnection(dbConnection);
    }
    return gameName;
  }

  /* in case of someone's gameName not perfectly match */
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
    } finally {
      Database.returnConnection(dbConnection);
    }
    return gameID;
  }
}
