package com.fgc.backend;

import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONObject;

import com.fgc.data.GameRooms;
import com.fgc.data.JSON;
import com.fgc.data.User;
import com.fgc.tools.ConsoleLog;
import com.fgc.tools.Database;

public class MatchingSession implements Runnable {
  private User user;
  private String gameID;
  private String userReply;
  private JSONObject listJSON;
  private JSONArray arrayJSON;
  private JSONObject replyJSON;
  public static final long SLEEPTIME = 1000;

  public MatchingSession(User client, String id) {
    user = client;
    gameID = id;
  }

  @Override
  public void run() {
    // run sql query...
    listJSON = JSON.jsonResultTrue();
    arrayJSON = new JSONArray();
    // fake some id...
    arrayJSON.put(JSON.jsonIDObject("aaa"));
    arrayJSON.put(JSON.jsonIDObject("bbb"));
    arrayJSON.put(JSON.jsonIDObject("ccc"));
    arrayJSON.put(JSON.jsonIDObject("ddd"));
    listJSON.put(JSON.KEY_LIST, arrayJSON);
    sendReceive();
    getInviteID();


    // simulate resultID 2
    listJSON = JSON.jsonResultObject(2);
    listJSON.put(JSON.KEY_LIST, arrayJSON);
    sendReceive();
    getInviteID();

    // simulate resultID 1
    listJSON = JSON.jsonResultObject(1);
    listJSON.put(JSON.KEY_ID, "liquidT");
    sendReceive();

    if (!isAccept()) {
      listJSON = JSON.jsonResultObject(2);
      listJSON.put(JSON.KEY_LIST, arrayJSON);
      sendReceive();
      getInviteID();
    }

    listJSON = JSON.jsonResultObject(0);
    sendReceive();

    if (Database.hackhackhack()) {
      GameRooms.putRoom(user.getName(), new GameRoomSession(user));
    } else {
      GameRoomSession room;
      while (true) {
        room = GameRooms.getRoom(getInviteID());
        if (room == null)
          try {
            Thread.sleep(SLEEPTIME);
          } catch (InterruptedException e) {
            ConsoleLog.errorPrint(user.getName() + " in " + getClass().getName() + " sleep fail.");
            user.close();
          }
        else {
          room.joinRoom(user);
          new Thread(room).start();
          break;
        }
      }
    }
  }

  // DIRTY HACKKKKKKKKKKK
  private void sendReceive() {
    user.send(listJSON.toString());

    try {
      userReply = user.receive();
    } catch (IOException e) {
      ConsoleLog.errorPrint(user.getName() + "disconnect in Matching");
      user.close();
      e.printStackTrace();
      return;
    }
  }

  private String getInviteID() {
    replyJSON = new JSONObject(userReply);
    return replyJSON.getString(JSON.KEY_INVITE); // check has require data...
  }

  private boolean isAccept() {
    replyJSON = new JSONObject(userReply);
    return replyJSON.getBoolean(JSON.KEY_ACCEPT); // check has require data...
  }

}
