package com.fgc.dbquery;

import java.util.HashMap;

public class MatchingGamesSQL {
  private static HashMap<String, MatchingSQLAction> list;
  static {
    list = new HashMap<String, MatchingSQLAction>();
  }
  public static MatchingSQLAction getMatchingList(String gameID) {
    MatchingSQLAction action = list.get(gameID);
    if(action == null) {
      action = new MatchingSQLAction(gameID);
      list.put(gameID, action);
    }
    return action;
  }
}
