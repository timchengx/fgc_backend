package com.fgc.data;

import java.util.HashMap;

import com.fgc.backend.MatchingSession;

/* 
 * this is a map for saving session(user) that waiting another client 
 * to reply their invite request
 */
public class WaitingReplyUsers {
  private static HashMap<String, MatchingSession> list;
  static {
    list = new HashMap<String, MatchingSession>();
  }
  public static void putWaiting(String userGameID, MatchingSession session) {
    list.put(userGameID, session);
  }
  public static MatchingSession getUser(String userGameID) {
    return list.get(userGameID);
  }
  public static void remove(String userGameID) {
    list.remove(userGameID);
  }
}
