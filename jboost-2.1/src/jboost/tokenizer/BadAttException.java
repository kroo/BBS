package jboost.tokenizer;

// eventually move all to examples and make this private 

/** Attribute cannot be parsed */
public class BadAttException extends ParseException {

  int firstLineNum;

  public BadAttException(String errorMessage, int lineNum, int firstLineNum) {
    super(errorMessage, lineNum);
    this.firstLineNum = firstLineNum;
  }

  public BadAttException(String errorMessage) {
    super(errorMessage);
  }
}
