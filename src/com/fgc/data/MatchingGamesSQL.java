package com.fgc.data;

import java.util.HashMap;

import com.fgc.dbquery.MatchingSQLAction;

/* 
 * this map class is design for saving SQL handling class
 * for every client to retrieve
 */
public class MatchingGamesSQL {
  private static HashMap<String, MatchingSQLAction> list;
  static {
    list = new HashMap<String, MatchingSQLAction>();
  }

  /* get sql class by gameID */
  public static MatchingSQLAction getMatchingList(String gameID) {
    MatchingSQLAction action = list.get(gameID);
    /* 
     * if the class that correspond to their game is not yet created 
     * create it and put it to the map
     */
    if (action == null) {
      action = new MatchingSQLAction(gameID);
      list.put(gameID, action);
    }
    return action;
  }
}
