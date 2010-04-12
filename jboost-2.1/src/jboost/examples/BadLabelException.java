package jboost.examples;

/** an Exception that is thrown when a label has in inappropriate value */
public class BadLabelException extends Exception {

  public BadLabelException(String message) {
    this.message = message;
  }

  public String getMessage() {
    return (message);
  }

  private String message;
}
