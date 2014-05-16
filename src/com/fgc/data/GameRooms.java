package com.fgc.data;

import java.util.HashMap;

import com.fgc.backend.GameRoomSession;

public class GameRooms {
  private static HashMap<String, GameRoomSession> table;
  static {
    table = new HashMap<String, GameRoomSession>();
  }

  public static GameRoomSession getRoom(String host) {
    return table.get(host);
  }

  public static void putRoom(String user, GameRoomSession gameRoom) {
    table.put(user, gameRoom);
  }
  public static void removeRoom(String user) {
    table.remove(user);
  }
}
