package com.fgc.test;
import java.sql.Connection;

import org.junit.Before;
import org.junit.Test;

import com.fgc.dbquery.Database;
public class DatabaseTest {
  
  @Before
  public void setDB() {
    Database.setDatabase("140.134.27.124", "fgc", "root", "", 1, 100);
    Database.startDatabase();
    Database.startDatabase();
  }
  @Test
  public void getAndReturnConnection() throws InterruptedException {
    Database.getConnection();
    Database.getConnection();
    Connection conn = Database.getConnection();
    Database.returnConnection(conn);
    Database.returnConnection(conn);
    Database.returnConnection(null);
    Thread.sleep(1000);
  }
  @Test
  public void errorDBSetting() throws InterruptedException {
    Database.setDatabase("localhost", "fgc", "root", "", 1, 100);
    Thread.sleep(1000);
  }
}
