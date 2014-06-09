package com.fgc.tools;

import java.io.IOException;

import com.fgc.backend.MatchingSession;
import com.fgc.data.User;

/*  
 *  this thread will be use to receive client's data
 *  when client is waiting another player online 
 *  because MatchingSession current is 
 *  receiving SQL's data
 */
public class OnePersonCheckDaemon implements Runnable {
  private MatchingSession session;  // the session need to notify
  private User user;                // user who is waiting
  
  public OnePersonCheckDaemon(MatchingSession ms, User connection) {
    session = ms;
    user = connection;
  }
  @Override
  public void run() {
    ConsoleLog.gameIDPrint(user.getGameID(), user.getUserGameName() + " waiting thread start ");
    try {
      String data = null;
      data = user.receive();    // tried to receive user's data
      
      /* if data is corrupt or user is disconnected, notify session */
      if(data == null) {
        ConsoleLog.gameIDPrint(user.getGameID(), user.getUserGameName() + " thread get null");
        session.notifyResult(true, data);
      }
      
      /* if received data, notify and send to session */
      else {
        ConsoleLog.gameIDPrint(user.getGameID(), user.getUserGameName() + " thread received data");
        session.notifyResult(false, data);
      }
    } catch (IOException e) {
      session.notifyResult(true, null);
    }
  }

}
