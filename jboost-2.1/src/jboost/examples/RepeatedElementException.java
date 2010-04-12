package jboost.examples;

class RepeatedElementException extends Exception {

  RepeatedElementException(String message) {
    this.message = message;
  }

  public String getMessage() {
    return (message);
  }

  private String message;
}
