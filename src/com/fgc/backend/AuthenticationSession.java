package com.fgc.backend;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

import com.fgc.data.User;
import com.fgc.dbquery.LoginSQLCheck;
import com.fgc.tools.ConsoleLog;
import com.fgc.tools.FGCJSON;

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
      loginString = user.receive();
      if (loginString == null || loginString.isEmpty()) {
        ConsoleLog.println("System: a user disconnect in auth.");
        user.close();
        return;
      }
      JSONObject loginJSON = new JSONObject(loginString);
      String token = loginJSON.getString(FGCJSON.KEY_TOKEN);
      String gameID = loginJSON.getString(FGCJSON.KEY_GAMEID);
      String gameName = LoginSQLCheck.login(token, gameID);
      if (gameName != null) {
        user.setInformation(gameName, gameID);
        new Thread(new MatchingSession(user, LoginSQLCheck.fixGameName(gameID))).start();
        ConsoleLog.gameIDPrint(gameID, gameName + " login!");
      } else {
        user.send(FGCJSON.createResultFalse().toString());
        user.close();
      }
    } catch (IOException e) {
      ConsoleLog.println("System: IOException in auth.");
      user.close();
    } catch (JSONException e) {
      ConsoleLog.println("System: disconnect a user (JSON data invalid in auth)" + " (" + loginString + ")");
      user.send(FGCJSON.createResultFalse().toString());
      user.close();
    }
  }
}
