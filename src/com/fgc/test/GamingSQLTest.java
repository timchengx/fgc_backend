package com.fgc.test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.fgc.dbquery.Database;
import com.fgc.dbquery.GamingSQLAction;

public class GamingSQLTest {
  @Before
  public void setDB() {
    Database.setDatabase("140.134.27.124", "fgc", "root", "", 1, 100);
    Database.startDatabase();
  }

  @Test
  public void removeQueueTest() throws SQLException {
    Connection conn = Database.getConnection();
    PreparedStatement sqlQuery =
        conn.prepareStatement("INSERT INTO `fgc`.`queue` (`id`, `game`) VALUES ('test1', 'fgcchess')");
    sqlQuery.executeUpdate();
    GamingSQLAction.removeFromQueue("fgcchess", "test1");
  }

  @Test
  public void createGameRecordTest() throws SQLException {
    GamingSQLAction.createGameRecord("fgcchess", "test1", "test2");
    GamingSQLAction.createGameRecord("error", "test1", "test2");
  }

  @Test
  public void appendGameRecordTest() throws SQLException {
    int rid = GamingSQLAction.createGameRecord("fgcchess", "test1", "test2");
    GamingSQLAction.appendGameRecord(rid, "step1");
    GamingSQLAction.appendGameRecord(rid, "step2");
    GamingSQLAction.appendGameRecord(-1, "dummy");
  }

  @Test
  public void finishGameTest() {
    int rid = GamingSQLAction.createGameRecord("fgcchess", "test1", "test2");
    GamingSQLAction.finishGame(rid);
    GamingSQLAction.finishGame(-1);
  }

  @Test
  public void removeFromGameQueueTest() throws SQLException {
    Connection conn = Database.getConnection();
    PreparedStatement sqlQuery =
        conn.prepareStatement("INSERT INTO `fgc`.`queue_fgcchess` (`host`, `client`) VALUES ('test1', 'test2')");
    sqlQuery.execute();
    Database.returnConnection(conn);
    GamingSQLAction.removeFromGameQueue("fgcChess", "test1", "test2");
    GamingSQLAction.removeFromGameQueue("error", "test1", "test2");
    cleanGameHistory();
  }

  @Test
  public void addGameCountTest() {
    GamingSQLAction.addGameCount("fgcChess", "test1");
    GamingSQLAction.addGameCount("error", "error");
  }

  @Test
  public void setUserGameStatsTest() {
    GamingSQLAction.setUserGameStats("fgcChess", "test1", true);
    GamingSQLAction.setUserGameStats("fgcChess", "test1", false);
    GamingSQLAction.setUserGameStats("error", "error", true);
  }

  @After
  public void cleanGameHistory() throws SQLException {
    Connection conn = Database.getConnection();
    PreparedStatement sqlQuery = conn.prepareStatement("DELETE FROM fgc.game_record");
    sqlQuery.execute();
    sqlQuery = conn.prepareStatement("DELETE FROM fgc.queue_fgcchess");
    sqlQuery.execute();
    Database.returnConnection(conn);
  }
}
