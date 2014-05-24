package com.fgc.dbquery;

public class SQLTest {

  /**
   * @param args
   */
  public static void main(String[] args) {
    Database.setDatabase("140.134.27.118", "fgc", "root", "toor", 5, 60000);
    System.out.println(LoginSQLCheck.login("84f038b6e36f12d4886092e5d3632030", "fgcChess"));
  }

}
