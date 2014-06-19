package com.fgc.backend;

import java.io.IOException;
import java.util.Random;

import org.json.JSONException;
import org.json.JSONObject;

import com.fgc.data.GameRoomList;
import com.fgc.data.User;
import com.fgc.dbquery.GamingSQLAction;
import com.fgc.tools.ConsoleLog;
import com.fgc.tools.FGCJSON;

/* this class handles the game playing session */
public class GameRoomSession implements Runnable {
  private static Random dice = new Random();
  private User firstUser;
  private User secondUser;
  private JSONObject firstUserData;
  private JSONObject secondUserData;
  private String gameID;
  private int sqlRoomID;
  boolean firstUserFirst;
  private static final boolean FIRST_USER = true;
  private static final boolean SECOND_USER = false;

  /* constructor */
  public GameRoomSession(User first) {
    firstUser = first;
    gameID = first.getGameID();
  }

  public void joinRoom(User second) {
    secondUser = second;
  }

  @Override
  public void run() {
    /* initialize and send whofirst to players */
    initialize();
    sendWhoFirst();

    /* if is not firstUser make the first step, receive second user's data before enter loop */
    if (!firstUserFirst) {
      try {
        secondUserReceive();    //receive data
      } catch (IOException e) { // disconnect if receive data occur error
        disconnect(SECOND_USER);
        return;
      }
      if (secondUserData.has(FGCJSON.KEY_PUTITTHERE) && secondUserData.getBoolean(FGCJSON.KEY_PUTITTHERE)) {
        writeToSQL(secondUserData);
      }
      
      gameMessagePrint(secondUser.getUserGameName() + " send " + secondUserData.toString());
      firstUser.send(secondUserData.toString());    // pass second user's data to first user
    }

    /* send and receive loop, exit when game has result */
    while (true) {
      try {
        firstUserReceive();
      } catch (IOException e) {
        disconnect(FIRST_USER);
        break;
      }
      if (firstUserData.has(FGCJSON.KEY_WINNER)) {  // game has result
        gameFinish(true);
        break;
      }
      if (firstUserData.has(FGCJSON.KEY_PUTITTHERE) && firstUserData.getBoolean(FGCJSON.KEY_PUTITTHERE)) {
        writeToSQL(firstUserData);
      }

      gameMessagePrint(firstUser.getUserGameName() + " send " + firstUserData.toString());
      secondUser.send(firstUserData.toString());



      try {
        secondUserReceive();
      } catch (IOException e) {
        disconnect(SECOND_USER);
        break;
      }
      if (secondUserData.has(FGCJSON.KEY_WINNER)) { // game has result
        gameFinish(false);
        break;
      }
      if (secondUserData.has(FGCJSON.KEY_PUTITTHERE) && secondUserData.getBoolean(FGCJSON.KEY_PUTITTHERE)) {
        writeToSQL(secondUserData);
      }

      gameMessagePrint(secondUser.getUserGameName() + " send " + secondUserData.toString());
      firstUser.send(secondUserData.toString());
    }


  }

  /* write step data to database */
  private void writeToSQL(JSONObject data) {
    GamingSQLAction.appendGameRecord(sqlRoomID, data.getString(FGCJSON.KEY_DATA));
  }

  /* invoke when one of player send {"winner":(id)} */
  private void gameFinish(boolean isFirstUserSend) {
    String winner;
    /* when receive winner data, send {"result":true} to them and disconnect */
    if (isFirstUserSend) {
      gameMessagePrint("receive " + firstUser.getUserGameName() + " end game message("
          + firstUserData.toString() + ")");
      winner = firstUserData.getString(FGCJSON.KEY_WINNER);
      firstUser.send(FGCJSON.createResultTrue().toString());
      firstUser.close();
    } else {
      gameMessagePrint("receive " + secondUser.getUserGameName() + " end game message("
          + secondUserData.toString() + ")");
      winner = secondUserData.getString(FGCJSON.KEY_WINNER);
      secondUser.send(FGCJSON.createResultTrue().toString());
      secondUser.close();
    }
    // wait another user send finish...
    if (isFirstUserSend) {
      try {
        secondUserReceive();
        secondUser.send(FGCJSON.createResultTrue().toString());
        secondUser.close();
        gameMessagePrint("receive " + secondUser.getUserGameName() + " end game message("
            + secondUserData.toString() + ")");
        
        /* if both winner id is equal, update both player's win/lost count */
        if (secondUserData.has(FGCJSON.KEY_WINNER)
            && winner.equals(secondUserData.getString(FGCJSON.KEY_WINNER))) {

          GamingSQLAction.setUserGameStats(gameID, winner, true);
          if (winner.equals(firstUser.getUserGameName()))
            GamingSQLAction.setUserGameStats(gameID, secondUser.getUserGameName(), false);
          else
            GamingSQLAction.setUserGameStats(gameID, firstUser.getUserGameName(), false);
        } else {
          // if not or error occur... do nothing
        }
      } catch (IOException e) {
        ConsoleLog.errorPrint(secondUser.getUserGameName() + "in game " + gameID
            + "disconnect without send winner message");
      }

    } else { // (!isFirstUserSend)
      try {
        firstUserReceive();
        firstUser.send(FGCJSON.createResultTrue().toString());
        firstUser.close();
        gameMessagePrint("receive " + firstUser.getUserGameName() + " end game message("
            + firstUserData.toString() + ")");
      } catch (IOException e) {
        ConsoleLog.errorPrint(firstUser.getUserGameName() + "in game " + gameID
            + "disconnect without send winner message");
      }
      /* if both winner id is equal, update both player's win/lost count */
      if (secondUserData.has(FGCJSON.KEY_WINNER)
          && winner.equals(firstUserData.getString(FGCJSON.KEY_WINNER))) {

        GamingSQLAction.setUserGameStats(gameID, winner, true);
        if (winner.equals(firstUser.getUserGameName()))
          GamingSQLAction.setUserGameStats(gameID, secondUser.getUserGameName(), false);
        else
          GamingSQLAction.setUserGameStats(gameID, firstUser.getUserGameName(), false);
      } else {
        // if not or error occur... do nothing
      }
    }
    closeRoom();
  }

  /* send other player's id and players who first sequence  to both */
  private void sendWhoFirst() {
    JSONObject passData;
    
    passData = FGCJSON.createIDObject(secondUser.getUserGameName());
    passData.put(FGCJSON.KEY_WHOFIRST, firstUserFirst);
    firstUser.send(passData.toString());

    passData = FGCJSON.createIDObject(firstUser.getUserGameName());
    passData.put(FGCJSON.KEY_WHOFIRST, !firstUserFirst);
    secondUser.send(passData.toString());

  }

  /* might receive null(mean disconnect) */
  private void secondUserReceive() throws IOException {
    String data = secondUser.receive();
    if (data == null)
      throw new IOException();
    try {
      secondUserData = new JSONObject(data);
    } catch (JSONException e) {
      throw new IOException();
    }

  }

  /* might receive null(mean disconnect) */
  private void firstUserReceive() throws IOException {
    String data = firstUser.receive();
    if (data == null)
      throw new IOException();
    try {
      firstUserData = new JSONObject(data);
    } catch (JSONException e) {
      throw new IOException();
    }

  }

  /* invoke when someone unexpected disconnect */
  private void disconnect(boolean isFirstUser) {
    JSONObject finalResult = FGCJSON.createResultTrue();
    if (isFirstUser) {
      gameMessagePrint(firstUser.getUserGameName() + " disconnect without finish game.");
      firstUser.close();
      secondUser.send(finalResult.toString());
      secondUser.close();
    } else {
      gameMessagePrint(secondUser.getUserGameName() + " disconnect without finish game.");
      secondUser.close();
      firstUser.send(finalResult.toString());
      firstUser.close();
    }
    closeRoom();
  }

  /* remove this session from list and clean game queue and room */
  private void closeRoom() {
    GameRoomList.removeRoom(firstUser.getUserGameName());
    GamingSQLAction.removeFromGameQueue(gameID, firstUser.getUserGameName(),
        secondUser.getUserGameName());
    GamingSQLAction.finishGame(sqlRoomID);
  }

  /* print player's move in console */
  private void gameMessagePrint(String message) {
    if (firstUserFirst)
      ConsoleLog.gameIDPrint(gameID,
          "{" + firstUser.getUserGameName() + "," + secondUser.getUserGameName() + "}" + " (rid = "
              + sqlRoomID + ") " + message);
    else
      ConsoleLog.gameIDPrint(gameID,
          "{" + secondUser.getUserGameName() + "," + firstUser.getUserGameName() + "}" + " (rid = "
              + sqlRoomID + ") " + message);
  }

  /* initialze process */
  private void initialize() {
    String firstPlayUserName;
    String secondPlayUserName;
    firstUserFirst = dice.nextBoolean();    // determine who make the first move
    
    /* remove both player from game queue and add play game count */
    GamingSQLAction.removeFromQueue(gameID, firstUser.getUserGameName());
    GamingSQLAction.removeFromQueue(gameID, secondUser.getUserGameName());
    GamingSQLAction.addGameCount(gameID, firstUser.getUserGameName());
    GamingSQLAction.addGameCount(gameID, secondUser.getUserGameName());

    /* set game record first and second player by who make the first move */
    if (!firstUserFirst) {
      firstPlayUserName = secondUser.getUserGameName();
      secondPlayUserName = firstUser.getUserGameName();
    } else {
      firstPlayUserName = firstUser.getUserGameName();
      secondPlayUserName = secondUser.getUserGameName();
    }
    /* create a game record in database */
    sqlRoomID = GamingSQLAction.createGameRecord(gameID, firstPlayUserName, secondPlayUserName);
    gameMessagePrint("start a game");

  }

}
