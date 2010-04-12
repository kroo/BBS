package jboost.controller;

public class ConfigurationException extends Exception {

  String message;

  public ConfigurationException(String m) {
    message = m;
  }

  public String toString() {
    return (message);
  }
}
