package jboost.learner;

/**
 * An exception that indicates that an attribute is not what it is expected to
 * be
 */
public class IncompAttException extends RuntimeException {

  private String message;

  public IncompAttException(String m) {
    message = m;
  }

  IncompAttException(int example, int attribute, String expected, Class actual) {
    message = "IncompAttException: example no." + example + " attribute no. " + attribute + ", expected class " + expected + ", actual " + actual;
  }

  IncompAttException(String pre, int attribute, Class actual) {
    message = pre + ", attribute no. " + attribute + ", actual " + actual;
  }

  public String getMessage() {
    return (message);
  }
}
