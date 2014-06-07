package com.fgc.backend;

import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.fgc.data.GameRoomList;
import com.fgc.data.MatchingGamesSQL;
import com.fgc.data.User;
import com.fgc.data.WaitingReplyUsers;
import com.fgc.dbquery.MatchingSQLAction;
import com.fgc.tools.ConsoleLog;
import com.fgc.tools.FGCJSON;
import com.fgc.tools.OnePersonCheckDaemon;

public class MatchingSession implements Runnable {
  private User user;
  private String gameID;
  private String userReply;
  private JSONObject listJSON;
  private JSONArray arrayJSON;
  private JSONObject replyJSON;
  public static long SLEEPTIME = 3000;
  private MatchingSQLAction sqlData;
  private boolean isFirstRun;
  private boolean isDisconnect;
  private Thread daemon;


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
      listJSON = FGCJSON.createResultTrue();
      sqlData.joinGame(user.getUserGameName());
      isFirstRun = false;
    } else
      listJSON = FGCJSON.createResultObject(2);

    while (true) {
      arrayJSON = sqlData.getList();
      removeSelfInJSON();

      while (arrayJSON.length() == 0) {
        listJSON.put(FGCJSON.KEY_LIST, JSONObject.NULL);
        user.send(listJSON.toString());
        if (daemon == null) {
          daemon = new Thread(new OnePersonCheckDaemon(this, user));
          daemon.start();
        }
        if (isDisconnect) {
          dataCorrupt("no body online, thread get null ");
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
        listJSON.remove(FGCJSON.KEY_LIST);
      }

      listJSON.put(FGCJSON.KEY_LIST, arrayJSON);
      user.send(listJSON.toString());

      if (daemon == null) {
        receive();
      } else {
        while (daemon.isAlive());
        daemon = null;
      }

      try {
        getInviteID();
      } catch (JSONException e) {
        result = sqlData.putRequest(user.getUserGameName(), null, true);
        if (result != null)
          WaitingReplyUsers.getUser(result).receiveInviteReply(false);
        dataCorrupt("get invite error");
        return;
      } catch (NullPointerException e) {
        result = sqlData.putRequest(user.getUserGameName(), null, true);
        if (result != null)
          WaitingReplyUsers.getUser(result).receiveInviteReply(false);
        dataCorrupt("json null");
        return;
      }

      result = sqlData.putRequest(user.getUserGameName(), getInviteID(), false);

      if (result.equals(MatchingSQLAction.PUTREQUEST_RESULT2)) {
        listJSON = FGCJSON.createResultObject(2);
      } else if (!result.equals(MatchingSQLAction.PUTREQUEST_COMPLETE)) {
        listJSON = FGCJSON.createResultObject(1);
        listJSON.put(FGCJSON.KEY_ID, result);
        user.send(listJSON.toString());
        receive();
        try {
          acceptOrNot = isAccept();
        } catch (JSONException e) {
          WaitingReplyUsers.getUser(result).receiveInviteReply(false);
          sqlData.putRequest(user.getUserGameName(), null, true);
          dataCorrupt("in result1 json format error");
          return;
        } catch (NullPointerException e) {
          WaitingReplyUsers.getUser(result).receiveInviteReply(false);
          sqlData.putRequest(user.getUserGameName(), null, true);
          dataCorrupt("in result1 data null");
          return;
        }
        WaitingReplyUsers.getUser(result).receiveInviteReply(acceptOrNot);
        if (!acceptOrNot)
          listJSON = FGCJSON.createResultObject(2);
        else {
          user.send(FGCJSON.createResultObject(0).toString());
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
    // user.send(listJSON.toString());
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
      user.send(FGCJSON.createResultObject(0).toString());
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
    return replyJSON.getString(FGCJSON.KEY_INVITE);
  }

  private boolean isAccept() throws JSONException {
    replyJSON = new JSONObject(userReply);
    return replyJSON.getBoolean(FGCJSON.KEY_ACCEPT);
  }

  private void removeSelfInJSON() {
    if (arrayJSON.length() != 0) {
      for (int i = 0; i < arrayJSON.length(); i++) {
        if (arrayJSON.getJSONObject(i).getString(FGCJSON.KEY_ID).equals(user.getUserGameName())) {
          arrayJSON.remove(i);
          break;
        }
      }
    }
  }

  private void dataCorrupt(String message) {
    ConsoleLog.errorPrint(gameID + ": (" + message + ") " + user.getUserGameName()
        + " disconnect. data corrupt. (" + userReply + ")");
    user.send(FGCJSON.createResultFalse().toString());
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
