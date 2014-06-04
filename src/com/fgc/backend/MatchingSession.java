package com.fgc.backend;

import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.fgc.data.GameRoomList;
import com.fgc.data.JSON;
import com.fgc.data.MatchingGamesSQL;
import com.fgc.data.User;
import com.fgc.data.WaitingReplyUsers;
import com.fgc.dbquery.MatchingSQLAction;
import com.fgc.tools.ConsoleLog;
import com.fgc.tools.OnePersonCheckDaemon;

public class MatchingSession implements Runnable {
  private User user;
  private String gameID;
  private String userReply;
  private JSONObject listJSON;
  private JSONArray arrayJSON;
  private JSONObject replyJSON;
  public static long SLEEPTIME = 10000;
  private MatchingSQLAction sqlData;
  private boolean isFirstRun;
  private boolean isDisconnect;
  private OnePersonCheckDaemon daemon;


  public MatchingSession(User client, String id) {
    user = client;
    gameID = id;
    isFirstRun = true;
    isDisconnect = false;
    daemon = null;
  }

  @Override
  public void run() {
    String result;
    boolean acceptOrNot;

    if (isFirstRun) {
      sqlData = MatchingGamesSQL.getMatchingList(gameID);
      listJSON = JSON.createResultTrue();
      sqlData.joinGame(user.getUserGameName());
      isFirstRun = false;
    } else
      listJSON = JSON.createResultObject(2);

    while (true) {
      arrayJSON = sqlData.getList();
      removeSelfInJSON();

      while (arrayJSON.length() == 0) {
        listJSON.put(JSON.KEY_LIST, JSONObject.NULL);
        user.send(listJSON.toString());
        if (daemon == null) {
          daemon = new OnePersonCheckDaemon(this, user);
          new Thread(daemon).start();
        }
        if (isDisconnect) {
          dataCorrupt();
          sqlData.putRequest(user.getUserGameName(), null, true);
          return;
        }

        try {
          Thread.sleep(SLEEPTIME);
        } catch (InterruptedException e) {
          ConsoleLog.errorPrint(gameID + ": a thread sleep fail(" + user.getUserGameName() + ")");
        }
        arrayJSON = sqlData.getList();
        removeSelfInJSON();
        listJSON.remove(JSON.KEY_LIST);
      }

      listJSON.put(JSON.KEY_LIST, arrayJSON);
      user.send(listJSON.toString());

      if (daemon == null)
        receive();
      else
        daemon = null;

      try {
        getInviteID();
      } catch (JSONException e) {
        result = sqlData.putRequest(user.getUserGameName(), null, true);
        if (result != null)
          WaitingReplyUsers.getUser(result).receiveInviteReply(false);
        dataCorrupt();
        return;
      } catch (NullPointerException e) {
        result = sqlData.putRequest(user.getUserGameName(), null, true);
        if (result != null)
          WaitingReplyUsers.getUser(result).receiveInviteReply(false);
        dataCorrupt();
        return;
      }

      result = sqlData.putRequest(user.getUserGameName(), getInviteID(), false);

      if (result.equals(MatchingSQLAction.PUTREQUEST_RESULT2)) {
        listJSON = JSON.createResultObject(2);
      } else if (!result.equals(MatchingSQLAction.PUTREQUEST_COMPLETE)) {
        listJSON = JSON.createResultObject(1);
        listJSON.put(JSON.KEY_ID, result);
        user.send(listJSON.toString());
        receive();
        try {
          acceptOrNot = isAccept();
        } catch (JSONException e) {
          WaitingReplyUsers.getUser(result).receiveInviteReply(false);
          sqlData.putRequest(user.getUserGameName(), null, true);
          dataCorrupt();
          return;
        } catch (NullPointerException e) {
          WaitingReplyUsers.getUser(result).receiveInviteReply(false);
          sqlData.putRequest(user.getUserGameName(), null, true);
          dataCorrupt();
          return;
        }
        WaitingReplyUsers.getUser(result).receiveInviteReply(acceptOrNot);
        if (!acceptOrNot)
          listJSON = JSON.createResultObject(2);
        else {
          user.send(JSON.createResultObject(0).toString());
          GameRoomSession room = GameRoomList.getRoom(result);
          room.joinRoom(user);
          new Thread(room).start();
          break;
        }
      } else {
        WaitingReplyUsers.putWaiting(user.getUserGameName(), this);
        break;
      }
    }
  }

  private void receive() {
    //user.send(listJSON.toString());
    try {
      userReply = user.receive();
    } catch (IOException e) {
      ConsoleLog.errorPrint("IOException in matching session.");
      user.close();
    }
  }

  public void receiveInviteReply(boolean result) {
    WaitingReplyUsers.remove(user.getUserGameName());
    if (result) {
      user.send(JSON.createResultObject(0).toString());
      GameRoomList.putRoom(user.getUserGameName(), new GameRoomSession(user));
    } else {
      ConsoleLog.gameIDPrint(gameID, user.getUserGameName() + " send invite to " + getInviteID()
          + " rejected.");
      sqlData.putRequest(user.getUserGameName(), getInviteID(), true);
      new Thread(this).start();
    }
  }

  private String getInviteID() throws JSONException {
    replyJSON = new JSONObject(userReply);
    return replyJSON.getString(JSON.KEY_INVITE);
  }

  private boolean isAccept() throws JSONException {
    replyJSON = new JSONObject(userReply);
    return replyJSON.getBoolean(JSON.KEY_ACCEPT);
  }

  private void removeSelfInJSON() {
    if (arrayJSON.length() != 0) {
      for (int i = 0; i < arrayJSON.length(); i++) {
        if (arrayJSON.getJSONObject(i).getString(JSON.KEY_ID).equals(user.getUserGameName())) {
          arrayJSON.remove(i);
          break;
        }
      }
    }
  }

  private void dataCorrupt() {
    ConsoleLog.errorPrint(gameID + ": " + user.getUserGameName()
        + " disconnect. JSON data corrupt. (" + userReply + ")");
    user.send(JSON.createResultFalse().toString());
    user.close();
  }

  public static void setSleepTime(long time) {
    SLEEPTIME = time;
  }

  public void notifyResult(boolean result, String data) {
    if (result)
      isDisconnect = true;
    userReply = data;
  }

}
