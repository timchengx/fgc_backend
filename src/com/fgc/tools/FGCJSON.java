package com.fgc.tools;

import org.json.JSONObject;


/* this class contains the JSON data that fgc backend will use frequently */
public class FGCJSON {
  
  /* the JSON key String */
  public static final String KEY_TOKEN = "token";
  public static final String KEY_GAMEID = "gameID";
  public static final String KEY_RESULT = "result";
  public static final String KEY_LIST = "list";
  public static final String KEY_ID = "id";
  public static final String KEY_INVITE = "invite";
  public static final String KEY_ACCEPT = "accept";
  public static final String KEY_RESULTID = "resultID";
  public static final String KEY_WHOFIRST = "whoFirst";
  public static final String KEY_DATA = "data";
  public static final String KEY_PUTITTHERE = "PutItThere";
  public static final String KEY_WINNER = "winner";

  
  /* create "result":true JSON message */
  public static JSONObject createResultTrue() {
    JSONObject json = new JSONObject();
    json.put("result", true);
    return json;
  }

  /* create "result":false JSON message */
  public static JSONObject createResultFalse() {
    JSONObject json = new JSONObject();
    json.put(KEY_RESULT, false);
    return json;
  }
  
  /* create "id":id JSON message */
  public static JSONObject createIDObject(String id) {
    JSONObject json = new JSONObject();
    json.put(KEY_ID, id);
    return json;
  }
  
  /* create "resultID":int JSON message (use in MatchingSession) */
  public static JSONObject createResultObject(int resultID) {
    JSONObject json = new JSONObject();
    json.put(FGCJSON.KEY_RESULTID, resultID);
    return json;
  }
  
}
