package com.fgc.dbquery;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import com.fgc.tools.ConsoleLog;
import com.fgc.tools.LoginFailException;

/* the class handle login process */
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
      /* use token to lookup user data */
      PreparedStatement query = dbConnection.prepareStatement(SQL_TOKEN);
      query.setString(1, token);
      ResultSet queryResult = query.executeQuery();
      if (queryResult.next()) {
        /* check is token is expired or not */
        Timestamp expireTime = queryResult.getTimestamp(COLUMN_VALIDTIME);
        if (!expireTime.after(new java.util.Date()))
          throw new LoginFailException("token expired");    // throw exception when token expire

        /* check gameID is valid or not */
        query = dbConnection.prepareStatement(SQL_GAMEID);
        query.setString(1, gameID);
        queryResult = query.executeQuery();
        queryResult.first();
        if (queryResult.getInt(1) != 1)
          throw new LoginFailException("duplicate or can't found gameID");

        /* get user's game name id */
        query = dbConnection.prepareStatement(SQL_GETGAMENAME);
        query.setString(1, token);
        query.setString(2, gameID);
        queryResult = query.executeQuery();
        if (queryResult.next())
          gameName = queryResult.getString(COLUMN_ID);
        else /* throw error when user didn't created the game name id */
          throw new LoginFailException("user didn't have game account");
        
        // next iteration, multi login check
        
        /* the token has been used, so delete it */
        query = dbConnection.prepareStatement(SQL_DELETE_TOKEN);
        query.setString(1, token);
        query.executeUpdate();

      } else {
        throw new LoginFailException("can't found token");
      }
    } catch (SQLException e) {
      ConsoleLog.sqlErrorPrint("token = " + token + " , gameID = " + gameID);
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
      /* get correct case gameID */
      PreparedStatement query = dbConnection.prepareStatement(SQL_GETGAMEID);
      query.setString(1, gameID);
      ResultSet queryResult = query.executeQuery();
      if (queryResult.next()) {
        gameID = queryResult.getString(1);
      }
    } catch (SQLException e) {
      ConsoleLog.sqlErrorPrint(SQL_GETGAMEID, gameID);
    } finally {
      Database.returnConnection(dbConnection);
    }
    return gameID;
  }
}
