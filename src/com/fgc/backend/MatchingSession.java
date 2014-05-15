package com.fgc.backend;

import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONObject;

import com.fgc.data.JSON;
import com.fgc.data.User;

public class MatchingSession implements Runnable {
  private User user;
  private String gameID;
  private String userReply;
  public MatchingSession(User client, String id) {
    user = client;
    gameID = id;
  }
  @Override
  public void run() {
    //run sql query...
    JSONObject listJSON = JSON.jsonResultTrue();
    JSONArray arrayJSON = new JSONArray();
    //fake some id...
    arrayJSON.put(JSON.jsonIDObject("aaa"));
    arrayJSON.put(JSON.jsonIDObject("bbb"));
    arrayJSON.put(JSON.jsonIDObject("ccc"));
    arrayJSON.put(JSON.jsonIDObject("ddd"));
    listJSON.put(JSON.KEY_LIST, arrayJSON);
    user.send(listJSON.toString());
    //waiting receive...
    try {
      userReply = user.receive();
    } catch (IOException e) {
// print socket error and close thread 
      e.printStackTrace();
    }
  }

}
