package jboost.tokenizer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.StringTokenizer;
import java.util.Vector;

import jboost.controller.Configuration;
import jboost.examples.AttributeDescription;
import jboost.examples.ExampleDescription;
import jboost.monitor.Monitor;

/**
 * Parses a data file in the UCI format.
 * 
 * @author Nigel Duffy
 * @version $Header:
 *          /cvsroot/jboost/jboost/src/jboost/tokenizer/UCI_DataStream.java,v
 *          1.1.1.1 2007/05/16 04:06:02 aarvey Exp $
 */
public class UCI_DataStream extends DataStream {

  /** Returns the line number of the current example in the data source. */
  public long getLineNumber() {
    return (dfile.firstLineNum());
  }

  /**
   * Using the same example description use this DataStream on a different data
   * source.
   */
  public void reset(String data) throws IOException {
    BufferedReader br = new BufferedReader(new FileReader(data));
    // throws IOException
    dfile = new LTStreamTokenizer(br, ".\n");
  }

  /**
   * Constructor
   * 
   * @param spec
   *            The name of the specification file.
   * @param data
   *            The name of the data file.
   */
  public UCI_DataStream(String spec, String data) throws IOException, SpecFileException, ClassNotFoundException {
    ed = new ExampleDescription();
    ReadSpecFile(true, spec);

    BufferedReader br = new BufferedReader(new FileReader(data));
    // throws IOException
    dfile = new LTStreamTokenizer(br, ".\n");
    maxBadAtt = 0;
    maxBadExa = 0;
    maxMisAtt = noAttr;
  }

  /**
   * Constructor if isFile == false, examples are read from standard input.
   * 
   * @param isFile
   *            Is the second parameter a specification or a name of a file?.
   * @param stem
   *            A string containing the specification or the stem for the data
   *            files.
   */
  public UCI_DataStream(boolean isFile, String stem) throws IOException, SpecFileException, ClassNotFoundException {
    ed = new ExampleDescription();
    BufferedReader br;
    if (isFile) {
      ReadSpecFile(true, stem + ".spec");

      br = new BufferedReader(new FileReader(stem + ".train"));
      // throws IOException
    }
    else {
      ReadSpecFile(false, stem);
      br = new BufferedReader(new InputStreamReader(System.in));
    }
    dfile = new LTStreamTokenizer(br, ".\n");

  }

  /**
   * Creates the textual form of a single specific example Not yet implemented.
   * 
   * @param o
   *            The object to transform into an example
   */
  public String[] getExampleText(Object o) throws BadExaException {
    return (null);
  }

  /**
   * Gets an array of attributes in a standard textual form that can then be
   * transformed into an example by ExampleStream.
   */
  public String[] getExampleText() throws BadExaException {
    String[] retval = new String[noAttr + 1];
    String exa = null;
    LTStringTokenizer exaTokens = null;
    String nextAtt = null;
    String exaStr = null;
    int i = 0;
    while ((exaStr = dfile.next()) != null) { // potential example found
      exaTokens = new LTStringTokenizer(exaStr, ",");
      for (i = 0; i < noAttr; i++) {
        if ((nextAtt = exaTokens.next()) == null) throw new BadExaException(" Expected " + noAttr + " attributes, found only " + i + ".", dfile.firstLineNum());
        // attribute found, parse it
        nextAtt = nextAtt.trim();
        if (nextAtt.equals("?")) nextAtt = "";
        retval[i] = nextAtt;
      } // found all attributes
      if (exaTokens.next() != null) { // an extra one
        throw new BadExaException("Expected " + noAttr + " attributes, found more.\nSkipping rest of example.", dfile.firstLineNum());
      } // reading label
      retval[i] = exaTokens.rest();
      return (retval);
    } // no good examples found
    if (!dfile.rest().trim().equals("")) System.err.println("Warning: more data after last example.");
    return null;
  }

  /** Retreives the data specification in a standard internal form. */
  public ExampleDescription getExampleDescription() {
    return (ed);
  }

  /** Converts the DataStream Description into a String */
  public String toString() {
    String retval = new String();
    retval += ed + "\n";
    return (retval);
  }

  // ------------------------------- Accessor Methods
  // ------------------------------------//

  // ------------------------------- Protected Members
  // -----------------------------------//

  // ------------------------------- Private Members
  // -------------------------------------//

  /**
   * Parses the file or string containing the specification of the data.
   * 
   * @param isFile
   *            Is the second parameter a file name?
   * @param str
   *            The name of a file containing a specification or a string
   *            containing a specification.
   */
  public void ReadSpecFile(boolean isFile, String str) throws SpecFileException, ClassNotFoundException, IOException {

    String tmp = null;
    BufferedReader f = null;
    if (isFile) f = new BufferedReader(new FileReader(str));
    else f = new BufferedReader(new StringReader(str));
    tmp = f.readLine();

    while (tmp != null) {
      tmp = StringOp.cleanLine(tmp, "|");
      if (tmp != null) tmp = tmp.trim();
      if (tmp == null) {
        tmp = f.readLine();
        continue;
      }
      if (tmp.charAt(tmp.length() - 1) == '.') {
        parseBodyLine(tmp);
        tmp = f.readLine();
      }
      else {
        tmp += f.readLine();
      }
    }
    noAttr = ed.getNoOfAttributes();
    if (noAttr == 0) throw new SpecFileException("Examples have no attributes.");
    if (ed.getLabelDescription() == null) throw new SpecFileException("No label description given.");
  }

  private LTStreamTokenizer dfile;
  private boolean labelSet = false;

  /**
   * Reads the start of a line from the main body and uses the rest to construct
   * an appropriate AttributeDescription
   */
  private void parseBodyLine(String a) throws SpecFileException, IOException, ClassNotFoundException {
    String b = StringOp.cleanLine(a, "|");
    if (b == null) return;
    String valList = null;
    String type = null;
    String name = null;
    Vector okValues = null;
    if (labelSet) {
      StringTokenizer st = new StringTokenizer(b, ":");
      if (st.hasMoreTokens() == false) throw (new SpecFileException("Cannot parse Spec file line:\n" + b));
      name = st.nextToken();
      name = name.trim();
      if (st.hasMoreTokens() == false) throw (new SpecFileException("Cannot parse Spec file line:\n" + b));
      type = st.nextToken();
    }
    else {
      type = b;
      name = "label";
    }
    type = type.trim();
    if (type.length() == 0) throw (new SpecFileException("Cannot parse Spec file line:\n" + b));

    // Deal with terminating period.
    if (type.charAt(type.length() - 1) == '.') type = type.substring(0, type.length() - 1);

    Configuration conf = new Configuration();

    if (type.equals("continuous")) type = "number";
    else if (type.equals("discrete")) type = "number";
    else {
      valList = type;
      type = "finite";
    }
    if (type.equals("finite")) {
      okValues = (Vector) StringOp.toUniqList(valList, ",");
      conf.addOption("caseSignificant", "+");
    }
    if (!(type.equals("finite") || type.equals("text") || type.equals("number"))) throw (new SpecFileException("Type not valid for attribute " + b));

    if (!labelSet) name = "labels";

    AttributeDescription ad = AttributeDescription.build(name, type, conf, okValues);
    if (ad == null) throw (new SpecFileException("Cannot parse Spec file line:\n" + b));

    // Needs to check for errors in config file.

    if (!labelSet) {
      ed.setLabel(ad);
      labelSet = true;
    }
    else ed.addAttribute(ad);
  }

  // ------------------------------- Test Code
  // -------------------------------------------//

  /** Main for debugging purposes only */
  static public void main(String[] argv) throws IOException, ParseException, Exception {
    UCI_DataStream ed = new UCI_DataStream(true, "test");
    String[] tmp = null;
    while ((tmp = ed.getExampleText()) != null) {
      for (int i = 0; i < tmp.length; i++) {
        if (Monitor.logLevel > 3) Monitor.log(tmp[i]);
      }
    }
    if (Monitor.logLevel > 3) Monitor.log("" + ed);
  }

}
