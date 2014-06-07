package com.fgc.tools;

import java.io.IOException;

import com.fgc.backend.MatchingSession;
import com.fgc.data.User;

public class OnePersonCheckDaemon implements Runnable {
  private MatchingSession session;
  private User user;
  
  public OnePersonCheckDaemon(MatchingSession ms, User connection) {
    session = ms;
    user = connection;
  }
  @Override
  public void run() {
    ConsoleLog.gameIDPrint(user.getGameID(), user.getUserGameName() + " waiting thread start ");
    try {
      String data = null;
      data = user.receive();
      if(data == null) {
        ConsoleLog.gameIDPrint(user.getGameID(), user.getUserGameName() + " thread get null");
        session.notifyResult(true, data);
      }
      else {
        ConsoleLog.gameIDPrint(user.getGameID(), user.getUserGameName() + " thread received data");
        session.notifyResult(false, data);
      }
    } catch (IOException e) {
      session.notifyResult(true, null);
    }
  }

}
