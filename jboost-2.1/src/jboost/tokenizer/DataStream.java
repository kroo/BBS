package jboost.tokenizer;

import java.io.IOException;

import jboost.examples.ExampleDescription;

/**
 * This abstract class defines the ability to read a given format of data. For
 * example, jboost format, UCI format, Email, from a JDBC source etc. Including
 * the schema or specification of the data.
 * 
 * @author Nigel Duffy
 * @version $Header:
 *          /cvsroot/jboost/jboost/src/jboost/tokenizer/DataStream.java,v 1.2
 *          2007/10/23 23:52:26 aarvey Exp $
 */
public abstract class DataStream {

  public static String WEIGHT_ATTR = "weight";
  public static String INDEX_ATTR = "INDEX";
  public static String LABELS_ATTR = "labels";

  /**
   * Creates the textual form of a single specific example Not yet implemented.
   * 
   * @param o
   *            The object to transform into an example
   */
  public abstract String[] getExampleText(Object o) throws ParseException;

  /** Returns the line number of the current example in the data source. */
  public abstract long getLineNumber();

  /**
   * Creates out a string containing the information necessary to read the
   * datafile.
   */
  public abstract String toString();

  /** Retreives the data specification in a standard internal form. */
  public abstract ExampleDescription getExampleDescription();

  /**
   * Gets an array of attributes in a standard textual form that can then be
   * transformed into an example by ExampleStream.
   */
  public abstract String[] getExampleText() throws ParseException;

  /**
   * Using the same example description use this DataStream on a different data
   * source.
   */
  public abstract void reset(String data) throws IOException;

  /** reopen the same data source */
  public void reset() throws IOException {
    reset(data);
  }

  /**
   * Constructor
   * 
   * @param spec
   *            The name of the specification file.
   * @param data
   *            The name of the data file.
   */
  public DataStream(String spec, String data) throws IOException, SpecFileException {
    this.data = data;
  }

  /**
   * Constructor
   * 
   * @param isFile
   *            Is the second parameter a specification or a name of a file?.
   * @param stem
   *            A string containing the specification or the stem for the data
   *            files.
   */
  public DataStream(boolean isFile, String stem) throws IOException, SpecFileException {
  }

  /** Default constructor. */
  public DataStream() throws IOException, SpecFileException {
  }

  // ------------------------------- Accessor Methods
  // ------------------------------------//

  // ------------------------------- Protected Members
  // -----------------------------------//

  /**
   * Information about the example that is used by {@link
   * jboost.tokenizer.ExampleStream} to transform the field strings into the
   * jboost internal represantation
   */
  protected ExampleDescription ed;
  /** Number of attributes in a single example */
  protected int noAttr = 0;
  /** Maximal number of bad attributes allowed in a single good example */
  protected int maxBadAtt;
  /**
   * Maximal number of missing attributes allowed in a single good example
   */
  protected int maxMisAtt;
  /** Maximal number of bad examples allowed in a single stream */
  protected int maxBadExa;
  /** Counter for number of bad examples seen so far */
  protected int numBadExa = 0;

  // ------------------------------- Private Members
  // -------------------------------------//

  private String data;

  // ------------------------------- Test Code
  // -------------------------------------------//\
}
