package com.fgc.backend;

import java.io.IOException;
import java.util.Random;

import org.json.JSONException;
import org.json.JSONObject;

import com.fgc.data.GameRoomList;
import com.fgc.data.JSON;
import com.fgc.data.User;
import com.fgc.dbquery.GamingSQLAction;
import com.fgc.tools.ConsoleLog;

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

  public GameRoomSession(User first) {
    firstUser = first;
    gameID = first.getGameID();
  }

  public void joinRoom(User second) {
    secondUser = second;
  }

  @Override
  public void run() {
    initialize();
    sendWhoFirst();

    if (!firstUserFirst) {
      try {
        secondUserReceive();
      } catch (IOException e) {
        disconnect(SECOND_USER);
        return;
      }
      gameMessagePrint(secondUser.getUserGameName() + " send " + secondUserData.toString());
      firstUser.send(secondUserData.toString());
    }


    while (true) {
      try {
        firstUserReceive();
      } catch (IOException e) {
        disconnect(FIRST_USER);
        break;
      }
      if (firstUserData.has(JSON.KEY_WINNER)) {
        gameFinish(true);
        break;
      }
      if (firstUserData.has(JSON.KEY_PUTITTHERE) && firstUserData.getBoolean(JSON.KEY_PUTITTHERE)) {
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
      if (secondUserData.has(JSON.KEY_WINNER)) {
        gameFinish(false);
        break;
      }
      if (secondUserData.has(JSON.KEY_PUTITTHERE) && secondUserData.getBoolean(JSON.KEY_PUTITTHERE)) {
        writeToSQL(firstUserData);
      }

      gameMessagePrint(secondUser.getUserGameName() + " send " + secondUserData.toString());
      firstUser.send(secondUserData.toString());
    }


  }

  private void writeToSQL(JSONObject data) {
    GamingSQLAction.appendGameRecord(sqlRoomID, data.getString(JSON.KEY_DATA));
  }

  private void gameFinish(boolean isFirstUserSend) {
    String winner;
    if (isFirstUserSend) {
      gameMessagePrint("receive " + firstUser.getUserGameName() + " end game message("
          + firstUserData.toString() + ")");
      winner = firstUserData.getString(JSON.KEY_WINNER);
      firstUser.send(JSON.createResultTrue().toString());
      firstUser.close();
    } else {
      gameMessagePrint("receive " + secondUser.getUserGameName() + " end game message("
          + secondUserData.toString() + ")");
      winner = secondUserData.getString(JSON.KEY_WINNER);
      secondUser.send(JSON.createResultTrue().toString());
      secondUser.close();
    }
    // wait another user send finish...
    if (isFirstUserSend) {
      try {
        secondUserReceive();
        secondUser.send(JSON.createResultTrue().toString());
        secondUser.close();
        gameMessagePrint("receive " + secondUser.getUserGameName() + " end game message("
            + secondUserData.toString() + ")");
        if (secondUserData.has(JSON.KEY_WINNER)
            && winner.equals(secondUserData.getString(JSON.KEY_WINNER))) {

          GamingSQLAction.setUserGameStats(gameID, winner, true);
          if (winner.equals(firstUser.getUserGameName()))
            GamingSQLAction.setUserGameStats(gameID, secondUser.getUserGameName(), false);
          else
            GamingSQLAction.setUserGameStats(gameID, firstUser.getUserGameName(), false);
        } else {
          // error... do nothing
        }
      } catch (IOException e) {
        ConsoleLog.errorPrint(secondUser.getUserGameName() + "in game " + gameID
            + "disconnect without send winner message");
      }

    } else { // (!isFirstUserSend)
      try {
        firstUserReceive();
        firstUser.send(JSON.createResultTrue().toString());
        firstUser.close();
        gameMessagePrint("receive " + firstUser.getUserGameName() + " end game message("
            + firstUserData.toString() + ")");
      } catch (IOException e) {
        ConsoleLog.errorPrint(firstUser.getUserGameName() + "in game " + gameID
            + "disconnect without send winner message");
      }
      if (secondUserData.has(JSON.KEY_WINNER)
          && winner.equals(firstUserData.getString(JSON.KEY_WINNER))) {

        GamingSQLAction.setUserGameStats(gameID, winner, true);
        if (winner.equals(firstUser.getUserGameName()))
          GamingSQLAction.setUserGameStats(gameID, secondUser.getUserGameName(), false);
        else
          GamingSQLAction.setUserGameStats(gameID, firstUser.getUserGameName(), false);
      } else {
        // error... do nothing
      }
    }
    closeRoom();
  }

  private void sendWhoFirst() {
    JSONObject passData;
    firstUserFirst = dice.nextBoolean();

    passData = JSON.createIDObject(secondUser.getUserGameName());
    passData.put(JSON.KEY_WHOFIRST, firstUserFirst);
    firstUser.send(passData.toString());

    passData = JSON.createIDObject(firstUser.getUserGameName());
    passData.put(JSON.KEY_WHOFIRST, !firstUserFirst);
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

  private void closeRoom() {
    remoteRoomFromGameRooms();
  }

  private void disconnect(boolean isFirstUser) {
    JSONObject finalResult = JSON.createResultTrue();
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
    remoteRoomFromGameRooms();
  }

  private void remoteRoomFromGameRooms() {
    GameRoomList.removeRoom(firstUser.getUserGameName());
    GamingSQLAction.removeFromGameQueue(gameID, firstUser.getUserGameName(),
        secondUser.getUserGameName());
    GamingSQLAction.finishGame(sqlRoomID);
  }

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

  private void initialize() {
    String firstPlayUserName;
    String secondPlayUserName;

    GamingSQLAction.removeFromQueue(gameID, firstUser.getUserGameName());
    GamingSQLAction.removeFromQueue(gameID, secondUser.getUserGameName());
    GamingSQLAction.addGameCount(gameID, firstUser.getUserGameName());
    GamingSQLAction.addGameCount(gameID, secondUser.getUserGameName());


    if (!firstUserFirst) {
      firstPlayUserName = secondUser.getUserGameName();
      secondPlayUserName = firstUser.getUserGameName();
    } else {
      firstPlayUserName = firstUser.getUserGameName();
      secondPlayUserName = secondUser.getUserGameName();
    }

    sqlRoomID = GamingSQLAction.createGameRecord(gameID, firstPlayUserName, secondPlayUserName);
    gameMessagePrint("start a game");

  }

}
