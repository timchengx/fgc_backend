package com.fgc.data;

import org.json.JSONObject;

public class JSON {
  public static final String KEY_TOKEN = "token";
  public static final String KEY_GAMEID = "gameID";
  public static final String KEY_RESULT = "result";
  public static final String KEY_LIST = "list";
  public static final String KEY_ID = "id";
  public static final String KEY_INVITE = "invite";
  public static final String KEY_ACCEPT = "accept";

  public static JSONObject jsonResultTrue() {
    JSONObject json = new JSONObject();
    json.put("result", true);
    return json;
  }

  public static JSONObject jsonResultFalse() {
    JSONObject json = new JSONObject();
    json.put(KEY_RESULT, false);
    return json;
  }
  public static JSONObject jsonIDObject(String id) {
    JSONObject json = new JSONObject();
    json.put(KEY_ID, id);
    return json;
  }
}
