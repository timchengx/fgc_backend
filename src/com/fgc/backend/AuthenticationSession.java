package com.fgc.backend;

import org.json.JSONObject;

import com.fgc.data.JSON;
import com.fgc.data.User;
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
      // run sql check login....

      //assume auth ok
      new Thread(new MatchingSession(user, gameID)).start();
      user.setInformation("user001", "gameid001");
      // if not...
//      user.send(JSON.jsonResultFalse());
//      user.close();
    } catch (Exception e) {
      ConsoleLog.errorPrint("Fail to get an user's login information");
      e.printStackTrace();
      return;
    }
  }

}
