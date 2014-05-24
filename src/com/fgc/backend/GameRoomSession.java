package com.fgc.backend;

import java.io.IOException;
import java.util.Random;

import org.json.JSONObject;

import com.fgc.data.GameRooms;
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

  public GameRoomSession(User first) {
    firstUser = first;
    gameID = first.getGameID();
  }

  public void joinRoom(User second) {
    secondUser = second;
  }

  @Override
  public void run() {

    GamingSQLAction.removeFromQueue(gameID, firstUser.getGameName());
    GamingSQLAction.removeFromQueue(gameID, secondUser.getGameName());
    GamingSQLAction.addGameCount(gameID, firstUser.getGameName());
    GamingSQLAction.addGameCount(gameID, secondUser.getGameName());

    sendWhoFirst();
    if (!firstUserFirst) {
      sqlRoomID =
          GamingSQLAction.createGameRecord(gameID, secondUser.getGameName(),
              firstUser.getGameName());
      try {
        secondUserReceive();
      } catch (IOException e) {
        ConsoleLog.errorPrint(secondUser.getGameID() + " with " + firstUser.getGameID()
            + "suddenly disconnect in game " + gameID);
        e.printStackTrace();
        disconnect(false);
        return;
      }
      firstUser.send(secondUserData.toString());
    } else
      sqlRoomID =
          GamingSQLAction.createGameRecord(gameID, firstUser.getGameName(),
              secondUser.getGameName());
    while (true) {
      try {
        firstUserReceive();
      } catch (IOException e) {
        ConsoleLog.errorPrint(firstUser.getGameID() + " with " + secondUser.getGameID()
            + "suddenly disconnect in game " + gameID);
        e.printStackTrace();
        disconnect(true);
        return;
      }
      if (firstUserData.has(JSON.KEY_WINNER)) {
        gameFinish(true);
        break;
      }
      if (firstUserData.getBoolean(JSON.KEY_PUTITTHERE)) {
        writeToSQL(firstUserData);
      }

      firstUser.send(secondUserData.toString());

      try {
        secondUserReceive();
      } catch (IOException e) {
        ConsoleLog.errorPrint(secondUser.getGameID() + " with " + firstUser.getGameID()
            + "suddenly disconnect in game " + gameID);
        e.printStackTrace();
        disconnect(false);
        return;
      }
      if (secondUserData.has(JSON.KEY_WINNER)) {
        gameFinish(false);
        break;
      }
      secondUser.send(firstUserData.toString());
    }


  }

  private void writeToSQL(JSONObject data) {
    GamingSQLAction.appendGameRecord(sqlRoomID, data.getString(JSON.KEY_DATA));
  }

  private void gameFinish(boolean isFirstUserSend) {
    String winner;
    if (isFirstUserSend) {
      winner = firstUserData.getString(JSON.KEY_WINNER);
      firstUser.send(JSON.createResultTrue().toString());
      firstUser.close();
    } else {
      winner = secondUserData.getString(JSON.KEY_WINNER);
      secondUser.send(JSON.createResultTrue().toString());
      secondUser.close();
    }
    // wait another user send finish...
    if (isFirstUserSend) {
      secondUserData = null;
      try {
        secondUserReceive();
        if (secondUserData == null || !secondUserData.has(JSON.KEY_WINNER)
            || !winner.equals(secondUserData.getString(JSON.KEY_WINNER))) {
          // error... do nothing
        } else {
          GamingSQLAction.setGameResult(gameID, winner, true);
          if (winner.equals(firstUser.getGameName()))
            GamingSQLAction.setGameResult(gameID, secondUser.getGameName(), false);
          else
            GamingSQLAction.setGameResult(gameID, firstUser.getGameName(), false);
        }
      } catch (IOException e) {
        ConsoleLog.errorPrint(secondUser.getGameName() + "in game " + gameID
            + "disconnect without send winner message");
        e.printStackTrace();
      }

    } else {
      firstUserData = null;
      try {
        firstUserReceive();
      } catch (IOException e) {
        ConsoleLog.errorPrint(firstUser.getGameName() + "in game " + gameID
            + "disconnect without send winner message");
        e.printStackTrace();
      }
      if (firstUserData == null || !secondUserData.has(JSON.KEY_WINNER)
          || !winner.equals(firstUserData.getString(JSON.KEY_WINNER))) {
        // error... do nothing
      } else {
        GamingSQLAction.setGameResult(gameID, winner, true);
        if (winner.equals(firstUser.getGameName()))
          GamingSQLAction.setGameResult(gameID, secondUser.getGameName(), false);
        else
          GamingSQLAction.setGameResult(gameID, firstUser.getGameName(), false);
      }
    }
    closeRoom();
  }

  private void sendWhoFirst() {
    JSONObject passData;
    firstUserFirst = dice.nextBoolean();

    passData = JSON.createIDObject(secondUser.getGameID());
    passData.put(JSON.KEY_WHOFIRST, firstUserFirst);
    firstUser.send(passData.toString());

    passData = JSON.createIDObject(firstUser.getGameID());
    passData.put(JSON.KEY_WHOFIRST, !firstUserFirst);
    secondUser.send(passData.toString());

  }

  private void secondUserReceive() throws IOException {
    secondUserData = new JSONObject(secondUser.receive());
  }

  private void firstUserReceive() throws IOException {
    firstUserData = new JSONObject(firstUser.receive());
  }

  private void closeRoom() {
    remoteRoomFromGameRooms();
    GamingSQLAction.finishGame(sqlRoomID);
  }

  private void disconnect(boolean isFirst) {
    JSONObject finalResult = JSON.createResultTrue();
    if (isFirst) {
      firstUser.close();
      secondUser.send(finalResult.toString());
      secondUser.close();
    } else {
      secondUser.close();
      firstUser.send(finalResult.toString());
      firstUser.close();
    }
    remoteRoomFromGameRooms();
    GamingSQLAction.finishGame(sqlRoomID);
  }

  private void remoteRoomFromGameRooms() {
    GameRooms.removeRoom(firstUser.getGameName());
  }

}
