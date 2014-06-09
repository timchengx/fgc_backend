package com.fgc.backend;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import com.fgc.data.User;
import com.fgc.dbquery.LoginSQLCheck;
import com.fgc.tools.ConsoleLog;
import com.fgc.tools.FGCJSON;

/* this session handles user authentication for connected user */
public class AuthenticationSession implements Runnable {
  private User user;

  public AuthenticationSession(User client) {
    user = client;
  }

  @Override
  public void run() {
    ConsoleLog.println("System: a user connected");
    String loginString = null;
    try {
      /* receive the user's login json data */
      loginString = user.receive();
      
      /* if something wrong with the json data, kick him out of system */
      if (loginString == null || loginString.isEmpty()) {
        ConsoleLog.println("System: a user disconnect in auth.");
        user.close();
        return;
      }
      /* extract json data to string */
      JSONObject loginJSON = new JSONObject(loginString);
      String token = loginJSON.getString(FGCJSON.KEY_TOKEN);
      String gameID = loginJSON.getString(FGCJSON.KEY_GAMEID);
      
      /* check database is this login succeed or not */
      String gameName = LoginSQLCheck.login(token, gameID);
      
      /* 
       * if user's game name is returned, mean's that login succeed 
       * navigate the user to the matching session
       */
      if (gameName != null) {
        user.setInformation(gameName, gameID);  // save this user's gamename and the game that he's play
        /* go to matching session */
        new Thread(new MatchingSession(user, LoginSQLCheck.fixGameName(gameID))).start();
        ConsoleLog.gameIDPrint(gameID, gameName + " login!");
      } else {  // if no game name returned, means that login fail, kick him out of system
        user.send(FGCJSON.createResultFalse().toString());
        user.close();
      }
    } catch (IOException e) {
      ConsoleLog.println("System: IOException in auth.");
      user.close();
    } catch (JSONException e) { // if json data can't be determine, kick him out of system
      ConsoleLog.println("System: disconnect a user (JSON data invalid in auth)" + " (" + loginString + ")");
      user.send(FGCJSON.createResultFalse().toString());
      user.close();
    }
  }
}
