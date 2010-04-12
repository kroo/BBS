package jboost;

/**
 * an Exception that is used to alert that a particular method is not supported
 * for this class
 */
public class NotSupportedException extends Exception {

  private static final long serialVersionUID = -3925862347277553481L;

  public NotSupportedException(String m, String c) {
    message = "The class " + c + " does not support the method " + m + "\n";
  }

  public NotSupportedException(String m) {
    message = m + "\n";
  }

  public String getMessage() {
    return message;
  }

  private String message;
}
