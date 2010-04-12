package jboost.tokenizer;

import java.io.IOException;

import jboost.examples.Attribute;
import jboost.examples.Example;
import jboost.examples.ExampleDescription;
import jboost.examples.IntegerAttribute;
import jboost.examples.Label;
import jboost.examples.RealAttribute;
import jboost.monitor.Monitor;

/**
 * Transforms a set of input data files into a stream of memory-based examples
 * 
 * @author: alon orlitsky (re-written by Nigel Duffy 9/8/00)
 * @version: $Header:
 *           /cvsroot/jboost/jboost/src/jboost/tokenizer/ExampleStream.java,v
 *           1.1.1.1 2007/05/16 04:06:02 aarvey Exp $
 */
public class ExampleStream {

  /**
   * Constructor. Takes a DataStream which in turn knows how to read specific
   * data and specification formats.
   * 
   * @param DataStream
   *            Stream from underlying data source, produces a stream of textual
   *            attribute lists.
   */
  public ExampleStream(DataStream dstream) {
    ds = dstream;
    maxBadAtt = ds.maxBadAtt;
    maxBadExa = ds.maxBadExa;
    numBadAtt = 0;
    numBadExa = 0;
  }

  /**
   * Get the example description.
   * 
   * @return exampleDescription
   */
  public ExampleDescription getExampleDescription() {
    return (ds.ed);
  }

  /**
   * Returns the example corresponding to a given object
   * 
   * @param o
   *            the object to create an example for
   * @return example corresponding to object
   */
  public Example getExample(Object o) throws ParseException {

    String[] exaText = null;
    Example retval = null;
    try {
      exaText = ds.getExampleText(o);
      if (exaText == null) {
        return (null);
      }
      retval = parseExampleText(exaText);
    }
    catch (BadExaException e) { // bad example
      System.err.println("BadExaException at data item " + o);
      Monitor.log("BadExaException at data item " + o);
    }
    return (retval);
  }

  /**
   * Sequentially goes through examples and returns the next good one, null if
   * none found.
   * 
   * @return next good example
   */
  public Example getExample() throws ParseException {

    String[] exaText = null;
    Example retval = null;
    while (retval == null) {
      try {
        exaText = ds.getExampleText();
        if (exaText == null) {
          break;
        }
        retval = parseExampleText(exaText);
      }
      catch (BadExaException e) { // bad example
        System.err.println("BadExaException " + "Example beginning at line " + ds.getLineNumber() + "\n" + e.getMessage());
        Monitor.log("BadExaException " + "Example beginning at line " + ds.getLineNumber() + "\n" + e.getMessage());
        numBadExa++;
        if (numBadExa > maxBadExa) {
          throw new BadDataFileException("More than " + maxBadExa + " bad examples.", ds.getLineNumber());
        }
      }
    }
    return (retval);
  }

  /**
   * Transforms an array of strings (recieved from a DataStream) into an Example
   * If the ExampleDescription has the useSamplingWeights flag set to true, then
   * this method will ignore any attributes named DataStream.WEIGHT_ATTR
   * 
   * @param exaText
   *            text of an example
   * @return example
   */
  public Example parseExampleText(String[] exaText) throws BadExaException {
    if (Monitor.logLevel > 3) {
      Monitor.log("ExampleStream.parseExampleText Parsing Example:");
    }

    int numAtt = ds.ed.getNoOfAttributes();
    Attribute[] attArray = new Attribute[numAtt];
    Label label = null;
    double weight = 1.0;
    int index = -1;
    numBadAtt = 0;
    int labelIndex = ds.ed.getLabelIndex();
    int weightIndex = ds.ed.getWeightIndex();
    int indexIndex = ds.ed.getIndexIndex();

    // iterate through each attribute
    // if the attribute is the 'label' ,'INDEX' or the 'weight' then grab its
    // value
    // otherwise, add it to the attribute array
    for (int i = 0, j = 0; i < exaText.length; i++) {
      try {
        if (i == labelIndex) {
          label = (Label) ds.ed.getLabelDescription().str2Att(exaText[i]);
        }
        else if (i == weightIndex) {
          weight = ((RealAttribute) ds.ed.getWeightDescription().str2Att(exaText[i])).getValue();
        }
        else if (i == indexIndex) {
          index = ((IntegerAttribute) ds.ed.getIndexDescription().str2Att(exaText[i])).getValue();
        }
        else {
          if (Monitor.logLevel > 3) {
            Monitor.log(ds.ed.getAttributeDescription(j).getAttributeName() + ":" + exaText[i]);
          }
          attArray[j] = ds.ed.getAttributeDescription(j).str2Att(exaText[i]);
          j++;
        }
      }
      catch (BadAttException e) {
        System.err.println("BadAttException: Line " + ds.getLineNumber() + "\n" + e.getMessage() + "\nContinuing to parse example.");
        e.printStackTrace();
        numBadAtt++;
        if (numBadAtt > maxBadAtt) {
          throw new BadExaException("Number of bad attributes in example exceeds " + maxBadAtt + " skipping rest of example.", ds.getLineNumber());
        }
        attArray[j++] = null;
      }
    } // found all attributes

    return new Example(attArray, index, label, weight, ds.ed);
  }

  // -------------------------------- Private Members
  // ----------------------------//

  /** Maximum allowed number of bad attributes in a single example */
  private int maxBadAtt;
  /** Maximum allowed number of bad examples in a data stream */
  private int maxBadExa;
  /** Number of bad attributes in the current example */
  private int numBadAtt;
  /** Number of bad examples in the current datastream */
  private int numBadExa;
  /** The stream of examples in textual form. */
  private DataStream ds;

  // -------------------------------- Test Code
  // ----------------------------------//

  /**
   * Tests ExampleStream. Run as:
   * <p>
   * java ExampleStream filename
   * <p>
   * where filename is a name of a data file.
   */
  public static void main(String[] args) throws Exception {
    try {
      mainCore(args);
    }
    catch (IOException e) {
      System.err.println("IO exception: " + e.getMessage());
      e.printStackTrace();
    }
    catch (RuntimeException e) {
      System.err.println("Runtime exception: " + e.getMessage());
      e.printStackTrace();
    }
  }

  public static void mainCore(String[] args) throws IOException, Exception {

    /*
     * Example nextExa; if (args.length != 1) // wrong number of arguments throw
     * new RuntimeException("Usage: ExampleStream <stem>"); DataStream ds=new
     * jboost_DataStream(true,args[0]); ExampleStream es = new
     * ExampleStream(ds); try { while ((nextExa=es.getExample()) != null)
     * if(Monitor.logLevel>3) Monitor.log(ds.ed.toString(nextExa)); } catch
     * (BadDataFileException e) { System.err.println("BadDataFileException\n" +
     * e.getMessage()); } // es.finalize; // if(Monitor.logLevel>3)
     * Monitor.log("Parsing train file."); // ExampleStream es = new
     * ExampleStream(ed, args[0] + ".train"); // while (
     * (nextExa=es.getExample()) != null) // if(Monitor.logLevel>3)
     * Monitor.log(ed.toString(nextExa)); //es.finalize;
     */
  }
}
