package jboost.atree;

/**
 * An exception that indicates a problem in instrumenting a ComplexLearner
 * 
 * @author Nigel Duffy
 */
public class InstrumentException extends Exception {

  InstrumentException(String m) {
    message = m;
  }

  public String getMessage() {
    return (message);
  }

  private String message;
}
