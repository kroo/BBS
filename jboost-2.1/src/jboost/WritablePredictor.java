package jboost;

import java.io.FileNotFoundException;
import java.io.IOException;

import jboost.examples.ExampleDescription;

/**
 * An object that can classify <code>Instance</code>s, and that can also
 * write itself out in C or Java.
 */
public interface WritablePredictor extends Predictor {

  /**
   * Produces Java code for this combined classifier.
   * 
   * @param cname
   *            name of class of produced code
   * @param fname
   *            name of method for predicting on new data
   * @param specFileName
   *            name of the spec file to be used for reading data or null if
   *            code is not to read new data.
   * @param exampleDescription
   *            the description of examples
   */
  public abstract String toJava(String cname, String fname, String specFileName, ExampleDescription exampleDescription) throws FileNotFoundException,
                                                                                                                       IOException;

  /**
   * Produces C code for this combined classifier.
   * 
   * @param fname
   *            name of procedure for predicting on new data
   * @param exampleDescription
   *            the description of examples
   */
  public abstract String toC(String fname, ExampleDescription exampleDescription);

  /**
   * Produces Matlab code for this combined classifier.
   * 
   * @param fname
   *            name of procedure for predicting on new data
   * @param exampleDescription
   *            the description of examples
   */
  public abstract String toMatlab(String fname, ExampleDescription exampleDescription);

  /**
   * Produces Matlab code for this combined classifier.
   * 
   * @param fname
   *            name of procedure for predicting on new data
   * @param exampleDescription
   *            the description of examples
   */
  public abstract String toPython(String fname, ExampleDescription exampleDescription);
}
