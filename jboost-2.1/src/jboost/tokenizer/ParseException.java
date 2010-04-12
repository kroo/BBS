package jboost.tokenizer;

/** Indicates that an error occured while reading the data file */

// are all the following necessary?
public class ParseException extends Exception {

  long lineNum;

  public ParseException(String errorMessage, long lineNum) {
    super(errorMessage);
    this.lineNum = lineNum;
  }

  public ParseException(String errorMessage) {
    this(errorMessage, 0); // eliminate eventually
  }
}
