package com.fgc.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.junit.Before;
import org.junit.Test;

import com.fgc.dbquery.Database;
import com.fgc.dbquery.LoginSQLCheck;

public class LoginSQLTest {

  @Before
  public void setDB() throws SQLException {
    Database.setDatabase("140.134.27.124", "fgc", "root", "", 1, 100);
    Database.startDatabase();
  }

  // login method test
  @Test
  public void loginTest() throws SQLException {
    LoginSQLCheck.login("error", "error");
    // input test data for assert
    LoginSQLTest
        .setSQLEntry("UPDATE `fgc`.`user` SET `token`='123', `tokenDeadline`='2000-07-01 00:00:00' WHERE `uid`='14'");
    assertNull(LoginSQLCheck.login("123", "fgcChess"));

    LoginSQLTest
        .setSQLEntry("UPDATE `fgc`.`user` SET `token`='123', `tokenDeadline`='2100-07-01 00:00:00' WHERE `uid`='14'");
    assertNull(LoginSQLCheck.login("123", "error"));

    LoginSQLTest
        .setSQLEntry("UPDATE `fgc`.`user` SET `token`='123', `tokenDeadline`='2100-07-01 00:00:00' WHERE `uid`='14'");
    assertEquals("test1", LoginSQLCheck.login("123", "fgcChess"));

    LoginSQLTest
        .setSQLEntry("UPDATE `fgc`.`user` SET `token`='123', `tokenDeadline`='2100-07-01 00:00:00' WHERE `uid`='14'");
    assertNull(LoginSQLCheck.login("123", "JCG"));
  }

  // fixGameID method test
  @Test
  public void fixGameIDTest() {
    assertEquals("fgcChess", LoginSQLCheck.fixGameName("fgcChess"));
    assertEquals("error", LoginSQLCheck.fixGameName("error"));
  }

  public static void setSQLEntry(String str) throws SQLException {
    Connection conn = Database.getConnection();
    PreparedStatement sqlQuery = conn.prepareStatement(str);
    sqlQuery.execute();
    Database.returnConnection(conn);
  }
}
