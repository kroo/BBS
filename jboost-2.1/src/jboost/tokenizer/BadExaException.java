package jboost.tokenizer;

public class BadExaException extends ParseException {

  // is following constructor necessary?
  public BadExaException(String errorMessage, long lineNum) {
    super(errorMessage, lineNum);
  }
}
