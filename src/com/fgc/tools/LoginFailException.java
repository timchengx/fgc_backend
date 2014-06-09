package com.fgc.tools;

/* This exception will be throw when login data have error occur */
public class LoginFailException extends Exception {
  public LoginFailException(String message) {
    super(message);
  }
}
