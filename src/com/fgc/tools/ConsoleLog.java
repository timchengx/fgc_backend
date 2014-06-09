package com.fgc.tools;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;


/* this class will handle all fgc backend's console output */
public class ConsoleLog {
  private static DateFormat dateFormat;
  static {
    /* initialize time format */
    dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
  }
  
  /* normal println */
  public static void println(String print) {
    printTime();
    System.out.println(print);
  }
  
  /* invoked when tried to print something */
  private static void printTime() {
    System.out.print(dateFormat.format(new Date()) + ": ");
  }
  
  /* print error */
  public static void errorPrint(String message) {
    printTime();
    System.err.println("ERROR: " + message);
  }
  
  /* print SQL error with message */
  public static void sqlErrorPrint(String message) {
    errorPrint("when query db, " + message);
  }
  
  /* print SQL error with statement and message */
  public static void sqlErrorPrint(String statement, String strings) {
    String message = "statement = " + statement + "with string = " + strings;
    sqlErrorPrint(message);
  }
  
  /* print game's message */
  public static void gameIDPrint(String gameID, String message) {
    println(gameID + ": " + message);
  }
}
