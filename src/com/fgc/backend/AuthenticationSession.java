package com.fgc.backend;

import org.json.JSONObject;

import com.fgc.data.JSON;
import com.fgc.data.User;
import com.fgc.dbquery.LoginSQLCheck;
import com.fgc.tools.ConsoleLog;

public class AuthenticationSession implements Runnable {
  private User user;

  public AuthenticationSession(User client) {
    user = client;
  }

  @Override
  public void run() {
    try {
      String loginString = user.receive();
      JSONObject loginJSON = new JSONObject(loginString);
      String token = loginJSON.getString(JSON.KEY_TOKEN);
      String gameID = loginJSON.getString(JSON.KEY_GAMEID);
      String gameName = LoginSQLCheck.login(token, gameID);
      if(gameName != null) {
        user.setInformation(gameName, gameID);
        new Thread(new MatchingSession(user, gameID)).start();
      } else {
         throw new Exception();
      }
      
    } catch (Exception e) {
      ConsoleLog.errorPrint("Fail to get an user's login information");
      e.printStackTrace();
      user.send(JSON.createResultFalse().toString());
      user.close();
      return;
    }
  }

}
