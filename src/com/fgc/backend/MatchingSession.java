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
  public static long SLEEPTIME = 3000;
  private User user;
  private String gameID;
  private String userReply;
  
  private JSONObject listJSON;
  private JSONArray arrayJSON;
  private JSONObject replyJSON;
  
  private MatchingSQLAction sqlData;
  
  private boolean isFirstRun;   // is this thread been restart or is first run
  private boolean isDisconnect; // determine is user disconnect or not
  
  /*
   * if when user login, there is no player can player with,
   * this thread start for check is user disconnect on this state
   * (check by listen socket receive, if receive return null, means disconnect)
   */
  private Thread daemon;    

  /* constructor */
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

    /* 
     * if this session is first start, initialize SQL connect object
     * create JSON and write SQL to show that a user is login
     */
    if (isFirstRun) {
      sqlData = MatchingGamesSQL.getMatchingList(gameID);
      listJSON = FGCJSON.createResultTrue();
      sqlData.joinGame(user.getUserGameName());
      isFirstRun = false;
    } else  // if this session is been restart, means that invite request fail
      listJSON = FGCJSON.createResultObject(2);

    while (true) {
      /* get matching list and remove self in list */
      arrayJSON = sqlData.getList();
      removeSelfInJSON();

      /* 
       * if no one can match
       * send null list to client and wait next query
       */
      while (arrayJSON.length() == 0) {
        listJSON.put(FGCJSON.KEY_LIST, JSONObject.NULL);
        user.send(listJSON.toString());
        /* the daemon to determine is user disconnect in this state */
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
        /* after sleep query list again */
        arrayJSON = sqlData.getList();
        removeSelfInJSON();
        listJSON.remove(FGCJSON.KEY_LIST);
      }

      /* send list to user */
      listJSON.put(FGCJSON.KEY_LIST, arrayJSON);
      user.send(listJSON.toString());

      /* receive user's reply */
      if (daemon == null) {
        receive();
      } else {
        while (daemon.isAlive());
        daemon = null;
      }

      try {
        getInviteID();  //extract invite id
      } 
      /* 
       * if json data can't be determine, clean all request that this user
       * has send and notify other user request decline
       */
        catch (JSONException e) {
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
      /* write request to database and get resut */
      result = sqlData.putRequest(user.getUserGameName(), getInviteID(), false);

      /* if result2, re choice the player */
      if (result.equals(MatchingSQLAction.PUTREQUEST_RESULT2)) {
        listJSON = FGCJSON.createResultObject(2);
      /* if result1, send id to client and wait receive */
      } else if (!result.equals(MatchingSQLAction.PUTREQUEST_COMPLETE)) {
        listJSON = FGCJSON.createResultObject(1);
        listJSON.put(FGCJSON.KEY_ID, result);
        user.send(listJSON.toString());
        receive();
        try {
          acceptOrNot = isAccept(); // get user's reply (accept or not)
          /* 
           * if json data can't be determine, clean all request that this user
           * has send and notify other user request decline
           */
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
        /* notify another user invite result */
        WaitingReplyUsers.getUser(result).receiveInviteReply(acceptOrNot);
        /* if decline, send result2 to client */
        if (!acceptOrNot)
          listJSON = FGCJSON.createResultObject(2);
        else { 
          /* 
           * if accept, get room that been create by other player 
           * join the room and start gameroom session
           */
          user.send(FGCJSON.createResultObject(0).toString());
          GameRoomSession room = GameRoomList.getRoom(result);
          room.joinRoom(user);
          new Thread(room).start();
          break;
        }
      } else {
        /* if send request complete, end thread and wait other player's reply */
        WaitingReplyUsers.putWaiting(user.getUserGameName(), this);
        break;
      }
    }
  }

  /* receive user's data */
  private void receive() {
    try {
      userReply = user.receive();
    } catch (IOException e) {
      ConsoleLog.errorPrint("IOException in matching session.");
      user.close();
    }
  }

  /* 
   * if this session previously send invite to other user
   * this method will be invoked to handle invite result
   */
  public void receiveInviteReply(boolean result) {
    WaitingReplyUsers.remove(user.getUserGameName());
    /* if other player accept the request, user go to gameroom session */
    if (result) {
      user.send(FGCJSON.createResultObject(0).toString());
      GameRoomList.putRoom(user.getUserGameName(), new GameRoomSession(user));
    } else {    // if other player reject the request, restart matching session thread
      ConsoleLog.gameIDPrint(gameID, user.getUserGameName() + " send invite to " + getInviteID()
          + " rejected.");
      sqlData.putRequest(user.getUserGameName(), getInviteID(), true);
      new Thread(this).start();
    }
  }

  /* get user's invite data from json */
  private String getInviteID() throws JSONException {
    replyJSON = new JSONObject(userReply);
    return replyJSON.getString(FGCJSON.KEY_INVITE);
  }

  /* get user's result1 accept boolean result */
  private boolean isAccept() throws JSONException {
    replyJSON = new JSONObject(userReply);
    return replyJSON.getBoolean(FGCJSON.KEY_ACCEPT);
  }

  /* 
   * the matching list get from database will contain self in it
   * so remove self from json before send to client
   */
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

  /* invoke when received data from user can't be determine */
  private void dataCorrupt(String message) {
    ConsoleLog.errorPrint(gameID + ": (" + message + ") " + user.getUserGameName()
        + " disconnect. data corrupt. (" + userReply + ")");
    user.send(FGCJSON.createResultFalse().toString());
    user.close();
  }

  /* set query matching list interval time */
  public static void setSleepTime(long time) {
    SLEEPTIME = time;
  }

  /* 
   * when in no player waiting stage
   * if socket has send something this method will be invoke
   * to save the data
   */
  public void notifyResult(boolean result, String data) {
    if (result)
      isDisconnect = true;
    userReply = data;
  }

}
