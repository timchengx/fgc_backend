package com.fgc.tools;

import org.json.JSONArray;

import com.fgc.data.JSON;

public class JSONTest {
  public static void main(String[] args) { 
    JSONArray array = new JSONArray();
    System.out.println(array);
    array.put(JSON.createIDObject("aaa"));
    System.out.println(array.length());
    new JSONTest();
  }
  public JSONTest() {
    System.out.println(this.getClass().getName());
  }
}
