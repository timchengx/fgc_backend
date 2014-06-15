package com.fgc.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.fgc.dbquery.Database;
import com.fgc.dbquery.MatchingSQLAction;

public class MatchingSQLTest {
  MatchingSQLAction fgc;

  @Before
  public void setDB() throws SQLException {
    Database.setDatabase("140.134.27.124", "fgc", "root", "", 1, 100);
    Database.startDatabase();
    fgc = new MatchingSQLAction("fgcchess");
  }

  @Test
  public void joinGameAndgetListTest() throws SQLException {
    fgc.joinGame("test1");
    assertEquals("[{\"id\":\"test1\"}]", fgc.getList().toString());
    fgc.joinGame("error");
    deleteSQLEntry();
  }

  @Test
  public void putRequestTest() throws SQLException {
    fgc.joinGame("test1");
    fgc.joinGame("test2");
    assertEquals(MatchingSQLAction.PUTREQUEST_COMPLETE, fgc.putRequest("test1", "test2", false));
    assertEquals("test1", fgc.putRequest("test2", "test1", false));
    deleteSQLEntry();
  }

  @Test
  public void conflectRequestTest() throws SQLException {
    fgc.joinGame("test1");
    assertEquals(MatchingSQLAction.PUTREQUEST_RESULT2, fgc.putRequest("test1", "test2", false));
    deleteSQLEntry();
  }

  @Test
  public void notAvailableRequetTest() throws SQLException {
    fgc.joinGame("test1");
    fgc.joinGame("test2");
    fgc.joinGame("liquidT");
    assertEquals(MatchingSQLAction.PUTREQUEST_COMPLETE, fgc.putRequest("test1", "test2", false));
    assertEquals(MatchingSQLAction.PUTREQUEST_RESULT2, fgc.putRequest("liquidT", "test2", false));
    deleteSQLEntry();
  }

  @Test
  public void deleteRequestTest() throws SQLException {
    fgc.joinGame("test1");
    fgc.joinGame("test2");
    assertEquals(MatchingSQLAction.PUTREQUEST_COMPLETE, fgc.putRequest("test1", "test2", false));
    assertEquals(MatchingSQLAction.PUTREQUEST_COMPLETE, fgc.putRequest("test1", "test2", true));
    deleteSQLEntry();
  }
  
  @Test
  public void clientDisconnectTest() {
    fgc.joinGame("test1");
    fgc.joinGame("test2");
    assertEquals(MatchingSQLAction.PUTREQUEST_COMPLETE, fgc.putRequest("test1", "test2", false));
    assertEquals("test1", fgc.putRequest("test2", null, false));
    assertNull(fgc.putRequest("test1", null, false));
  }

  @Test
  public void putRequestError() {
    new MatchingSQLAction("errorGame").putRequest("dummy1", "dummy2", false);
  }

  @After
  public void deleteSQLEntry() throws SQLException {
    Connection conn = Database.getConnection();
    PreparedStatement sqlQuery = conn.prepareStatement("DELETE FROM fgc.queue");
    sqlQuery.executeUpdate();
    sqlQuery = conn.prepareStatement("DELETE FROM fgc.queue_fgcchess");
    sqlQuery.executeUpdate();
    Database.returnConnection(conn);
  }

}
