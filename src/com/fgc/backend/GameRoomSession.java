package com.fgc.backend;

import java.io.IOException;
import java.util.Random;

import org.json.JSONObject;

import com.fgc.data.GameRooms;
import com.fgc.data.JSON;
import com.fgc.data.User;

public class GameRoomSession implements Runnable {
  private static Random dice = new Random();
  private User firstUser;
  private User secondUser;
  private JSONObject firstUserData;
  private JSONObject secondUserData;
  boolean firstUserFirst;

  public GameRoomSession(User first) {
    firstUser = first;
  }

  public void joinRoom(User second) {
    secondUser = second;
  }

  @Override
  public void run() {

    sendWhoFirst();
    if (!firstUserFirst) {
      secondUserReceive();
      firstUser.send(secondUserData.toString());
    }
    while (true) {
      firstUserReceive();
      if (firstUserData.has(JSON.KEY_WINNER)) {
        gameFinish(true);
        break;
      }
      if (firstUserData.getBoolean(JSON.KEY_PUTITTHERE)) {
        writeToSQL(firstUserData);
      }

      firstUser.send(secondUserData.toString());

      secondUserReceive();
      if (secondUserData.has(JSON.KEY_WINNER)) {
        gameFinish(false);
        break;
      }

      secondUser.send(firstUserData.toString());

    }


  }

  private void writeToSQL(JSONObject data) {
    // run some sql...
  }

  private void gameFinish(boolean isFirstUserSend) {
    String winner;
    if (isFirstUserSend) {
      winner = firstUserData.getString(JSON.KEY_WINNER);
      firstUser.send(JSON.jsonResultTrue().toString());
      firstUser.close();
    } else {
      winner = secondUserData.getString(JSON.KEY_WINNER);
      secondUser.send(JSON.jsonResultTrue().toString());
      secondUser.close();
    }
    // wait another user send finish...
    if (isFirstUserSend) {
      secondUserData = null;
      secondUserReceive();
      if (secondUserData == null || !secondUserData.has(JSON.KEY_WINNER)
          || !winner.equals(secondUserData.getString(JSON.KEY_WINNER))) {
        // error... write to sql
      } else {
        // ok... write to sql...
      }
    } else {
      firstUserData = null;
      firstUserReceive();
      if (firstUserData == null || !secondUserData.has(JSON.KEY_WINNER)
          || !winner.equals(firstUserData.getString(JSON.KEY_WINNER))) {
        // error... write to sql
      } else {
        // ok... write to sql...
      }
    }
    closeRoom();
  }

  private void sendWhoFirst() {
    JSONObject passData;
    firstUserFirst = dice.nextBoolean();

    passData = JSON.jsonIDObject(secondUser.getID());
    passData.put(JSON.KEY_WHOFIRST, firstUserFirst);
    firstUser.send(passData.toString());

    passData = JSON.jsonIDObject(firstUser.getID());
    passData.put(JSON.KEY_WHOFIRST, !firstUserFirst);
    secondUser.send(passData.toString());

  }

  private void secondUserReceive() {
    try {
      secondUserData = new JSONObject(secondUser.receive());

    } catch (IOException e) {
      disconnect(false);
      e.printStackTrace();
    }
  }

  private void firstUserReceive() {
    try {
      firstUserData = new JSONObject(firstUser.receive());
    } catch (IOException e) {
      disconnect(true);
      e.printStackTrace();
    }
  }

  private void closeRoom() {
    remove();
    // do some sql...
  }

  private void disconnect(boolean isFirst) {
    JSONObject finalResult = JSON.jsonResultTrue();
    if (isFirst) {
      firstUser.close();
      secondUser.send(finalResult.toString());
      secondUser.close();
    } else {
      secondUser.close();
      firstUser.send(finalResult.toString());
      firstUser.close();
    }
    remove();
  }

  private void remove() {
    GameRooms.removeRoom(firstUser.getName());
  }

}
