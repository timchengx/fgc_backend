package com.fgc.backend;

import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONObject;

import com.fgc.data.GameRooms;
import com.fgc.data.JSON;
import com.fgc.data.User;
import com.fgc.data.WaitingReplyUsers;
import com.fgc.dbquery.MatchingGames;
import com.fgc.dbquery.MatchingSQLAction;
import com.fgc.tools.ConsoleLog;

public class MatchingSession implements Runnable {
  private User user;
  private String gameID;
  private String userReply;
  private JSONObject listJSON;
  private JSONArray arrayJSON;
  private JSONObject replyJSON;
  public static long SLEEPTIME = 30000;
  private MatchingSQLAction sqlData;
  private boolean isFirstRun;

  public static void setSleepTime(long time) {
    SLEEPTIME = time;
  }

  public MatchingSession(User client, String id) {
    user = client;
    gameID = id;
    isFirstRun = true;
  }

  @Override
  public void run() {

    if (isFirstRun) {
      sqlData = MatchingGames.getMatchingList(gameID);
      listJSON = JSON.createResultTrue();
      sqlData.joinGame(user.getGameID());
      isFirstRun = false;
    } else
      listJSON = JSON.createResultObject(2);
    while (true) {
      arrayJSON = sqlData.getList();
      removeSelfInJSON();

      while (arrayJSON.length() == 0) {
        listJSON.put(JSON.KEY_LIST, JSONObject.NULL);
        user.send(listJSON.toString());
        try {
          Thread.sleep(SLEEPTIME);
        } catch (InterruptedException e) {
          ConsoleLog.errorPrint(getClass().getSimpleName() + "with user " + user.getGameID()
              + "in game " + gameID + "Thread sleep fail");
          e.printStackTrace();
        }
        arrayJSON = sqlData.getList();
        removeSelfInJSON();
        listJSON.remove(JSON.KEY_LIST);
      }

      listJSON.put(JSON.KEY_LIST, arrayJSON);
      sendReceive();
      getInviteID();
      String result = sqlData.putRequest(user.getGameID(), getInviteID(), false);

      if (result.equals(MatchingSQLAction.PUTREQUEST_RESULT2)) {
        listJSON = JSON.createResultObject(2);
      } else if (!result.equals(MatchingSQLAction.PUTREQUEST_COMPLETE)) {
        listJSON = JSON.createResultObject(1);
        listJSON.put(JSON.KEY_ID, result);
        sendReceive();
        boolean acceptOrNot = isAccept();
        WaitingReplyUsers.getUser(result).receiveInviteReply(acceptOrNot);
        if(!acceptOrNot)
          listJSON = JSON.createResultObject(2);
        else {
          GameRoomSession room = GameRooms.getRoom(result);
          room.joinRoom(user);
          new Thread(room).start();
          sqlData.putRequest(result, user.getGameName(), true);
          break;
        }
      } else
        break;
    }
  }

  private void sendReceive() {
    user.send(listJSON.toString());

    try {
      userReply = user.receive();
    } catch (IOException e) {
      ConsoleLog.errorPrint(user.getGameName() + "disconnect in Matching");
      user.close();
      e.printStackTrace();
      return;
    }
  }

  public void receiveInviteReply(boolean result) {
    if (result) {
      GameRooms.putRoom(user.getGameName(), new GameRoomSession(user));
    } else
      new Thread(this).start();
  }

  private String getInviteID() {
    replyJSON = new JSONObject(userReply);
    return replyJSON.getString(JSON.KEY_INVITE); // check has require data...
  }

  private boolean isAccept() {
    replyJSON = new JSONObject(userReply);
    return replyJSON.getBoolean(JSON.KEY_ACCEPT); // check has require data...
  }

  private void removeSelfInJSON() {
    if (arrayJSON.length() != 0) {
      for (int i = 0; i < arrayJSON.length(); i++) {
        if (arrayJSON.getJSONObject(i).getString(JSON.KEY_ID).equals(user.getGameName())) {
          arrayJSON.remove(i);
          break;
        }
      }
    }
  }

}
