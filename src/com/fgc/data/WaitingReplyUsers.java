package com.fgc.data;

import java.util.HashMap;

import com.fgc.backend.MatchingSession;

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
