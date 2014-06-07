package com.fgc.tools;

import org.json.JSONObject;

public class FGCJSON {
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

  public static JSONObject createResultTrue() {
    JSONObject json = new JSONObject();
    json.put("result", true);
    return json;
  }

  public static JSONObject createResultFalse() {
    JSONObject json = new JSONObject();
    json.put(KEY_RESULT, false);
    return json;
  }
  public static JSONObject createIDObject(String id) {
    JSONObject json = new JSONObject();
    json.put(KEY_ID, id);
    return json;
  }
  public static JSONObject createResultObject(int resultID) {
    JSONObject json = new JSONObject();
    json.put(FGCJSON.KEY_RESULTID, resultID);
    return json;
  }
  
}
