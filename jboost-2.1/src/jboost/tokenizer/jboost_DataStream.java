package jboost.tokenizer;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
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
import jboost.util.FileLoader;

/**
 * Defines the functions necessary to read a specification and datafile in
 * jboost format. Uses the specification file ("stem".spec) to convert the data
 * files ("stem".train and "stem".test) into list of attrribute strings.
 * <p>
 * The format of the specification and data files is described in the <a
 * href="../../../jboost/tokenizer/datatypes.short">input format</a> file (not
 * in html format) while the output of the tokenizer is described in the <a
 * href=outformat>output format</a> file (not there yet..).
 * 
 * @author Nigel Duffy
 * @version $Header:
 *          /cvsroot/jboost/jboost/src/jboost/tokenizer/jboost_DataStream.java,v
 *          1.3 2008/01/24 22:48:54 aarvey Exp $
 */

public class jboost_DataStream extends DataStream {

  private LTStreamTokenizer m_data;

  // Global options
  private String m_exampleTerminator;
  private String m_attributeTerminator;
  private String m_elementSeparator;

  /**
   * Constructor
   * 
   * @param spec
   *            The name of the specification file.
   * @param data
   *            The name of the data file.
   */
  public jboost_DataStream(String spec, String data) throws IOException, SpecFileException, ClassNotFoundException {
    init(spec, data);
    if (Monitor.logLevel > 3) {
      Monitor.log("" + ed);
    }
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
  public jboost_DataStream(boolean isFile, String stem) throws IOException, SpecFileException, ClassNotFoundException {
    if (isFile) {
      init(stem + ".spec", stem + ".train");
    }
    else {
      ed = new ExampleDescription();
      readSpecFile(false, stem);
      BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
      m_data = new LTStreamTokenizer(br, m_exampleTerminator);
    }
  }

  /**
   * initialize this stream
   */
  private void init(String specFileName, String dataFileName) throws IOException, SpecFileException, ClassNotFoundException {
    ed = new ExampleDescription();
    readSpecFile(true, specFileName);
    BufferedReader br = new BufferedReader(FileLoader.createFileReader(dataFileName));
    // throws IOException
    m_data = new LTStreamTokenizer(br, m_exampleTerminator);
  }

  /**
   * Returns the line number of the current example in the data source
   */
  public long getLineNumber() {
    return (m_data.firstLineNum());
  }

  /**
   * Using the same example description use this DataStream on a different data
   * source.
   */
  public void reset(String data) throws IOException {
    BufferedReader br = new BufferedReader(new FileReader(data));
    // throws IOException
    m_data = new LTStreamTokenizer(br, m_exampleTerminator);
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
    int count = noAttr + 1;
    // increase the attribute count if the ExampleDescription includes a Weight
    if (ed.getWeightDescription() != null) {
      count++;
    }
    // increase the attribute count if the ExampleDescription includes an INDEX
    if (ed.getIndexDescription() != null) {
      count++;
    }
    String[] retval = new String[count];
    LTStringTokenizer exaTokens = null;
    String nextAtt = null;
    String exaStr = null;
    int i = 0;

    while ((exaStr = m_data.next()) != null) { // potential example found
      exaTokens = new LTStringTokenizer(exaStr, m_attributeTerminator);
      for (i = 0; i < retval.length - 1; i++) {
        if ((nextAtt = exaTokens.next()) == null) {
          throw new BadExaException("Expected " + count + " attributes, found only " + i + ".", m_data.firstLineNum());
        }
        // attribute found, parse it
        retval[i] = nextAtt;
      } // found all attributes

      /*
       * if (exaTokens.next() != null) { // an extra one throw new
       * BadExaException("Expected " + noAttr + " attributes, found more.\n" +
       * "Skipping rest of example.", m_data.firstLineNum()); } // reading label
       */
      retval[i] = exaTokens.rest();
      return (retval);
    } // no good examples found

    if (!m_data.rest().trim().equals("")) {
      System.err.println("Warning: more data after last example.");
    }

    return null;
  }

  /**
   * Retreives the data specification in a standard internal form.
   */
  public ExampleDescription getExampleDescription() {
    return (ed);
  }

  /**
   * Creates out a string containing the information necessary to read the
   * datafile.
   */
  public String toString() {
    String retval = new String();
    retval += "exampleTerminator=" + m_exampleTerminator + "\n";
    retval += "attributeTerminator=" + m_attributeTerminator + "\n";
    retval += "elementSeparator=" + m_elementSeparator + "\n";
    retval += "maxBadAtt=" + maxBadAtt + "\n";
    retval += "maxMisAtt=" + maxMisAtt + "\n";
    retval += "maxBadExa=" + maxBadExa + "\n";
    retval += ed + "\n";
    return (retval);
  }

  /**
   * Parses the file or string containing the specification of the data.
   * 
   * @param isFile
   *            Is the second parameter a file name?
   * @param str
   *            The name of a file containing a specification or a string
   *            containing a specification.
   */
  private void readSpecFile(boolean isFile, String specFile) throws SpecFileException, ClassNotFoundException, IOException {
    Configuration conf = new Configuration();
    conf.addValid("exampleTerminator");
    conf.addValid("attributeTerminator");
    conf.addValid("elementSeparator");
    conf.addValid("maxBadAtt");
    conf.addValid("maxMisAtt");
    conf.addValid("maxBadExa");

    BufferedReader reader = null;
    if (isFile) {
      reader = new BufferedReader(FileLoader.createFileReader(specFile));
      if (reader == null) {
        throw new FileNotFoundException(specFile);
      }
    }
    else {
      reader = new BufferedReader(new StringReader(specFile));
    }

    String line = null;
    line = reader.readLine();
    while (line != null) {
      if (!parsePreambleLine(line, conf)) {
        break;
      }
      else {
        line = reader.readLine();
      }
    }

    // Now done with reading the preamble, we must get the options
    m_exampleTerminator = conf.getString("exampleTerminator", ".");
    m_attributeTerminator = conf.getString("attributeTerminator", ",");
    m_elementSeparator = conf.getString("elementSeparator", null);
    maxBadAtt = conf.getInt("maxBadAtt", 0);
    maxMisAtt = conf.getInt("maxMisAtt", -1);
    maxBadExa = conf.getInt("maxBadExa", 20);

    // Need to check for errors in config file.
    if (Monitor.logLevel > 3) {
      Monitor.log("" + conf);
      Monitor.log(conf.unused());
    }

    int lineCount = 0;
    while (line != null) {
      parseBodyLine(line, lineCount);
      line = reader.readLine();
      lineCount++;
    }

    noAttr = ed.getNoOfAttributes();
    if (noAttr == 0) {
      throw new SpecFileException("Examples have no attributes.");
    }
    if (ed.getLabelDescription() == null) {
      throw new SpecFileException("No label description given.");
    }
  }

  /**
   * Sets the global parameters from the preamble text.
   */
  private boolean parsePreambleLine(String line, Configuration conf) {
    String clean = StringOp.cleanLine(line, "//");
    boolean retval = false;

    if (clean != null) {
      StringTokenizer tokens = new StringTokenizer(clean, "=");
      String first;
      String second;
      if (tokens.hasMoreTokens() == true) {
        first = tokens.nextToken();
        first = first.trim();
        if (conf.isValid(first) && tokens.hasMoreTokens()) {
          second = tokens.nextToken();
          second = second.trim();
          conf.addOption(first, second);
          retval = true;
        }
      }
    }
    else { // if the line is empty, just return true
      retval = true;
    }
    return retval;
  }

  /**
   * Reads the start of a line from the main body and uses the rest to construct
   * an appropriate AttributeDescription
   */
  private void parseBodyLine(String next, int lineCount) throws SpecFileException, ClassNotFoundException {

    String line = StringOp.cleanLine(next, "//");
    boolean parsed = true;
    // if the line is null, then skip it
    if (line != null) {
      parsed = false;
      StringTokenizer tokens = new StringTokenizer(line, " ");
      String options = null;
      Vector values = null;
      if (tokens.hasMoreTokens()) {
        // get the name of the attribute
        String name = tokens.nextToken();
        name = name.trim();
        if (tokens.hasMoreTokens()) {
          // get the type of the attribute
          String type = tokens.nextToken();
          type = type.trim();
          if (type.length() != 0) {

            // consider putting the code up to '****' in a separate method
            if (type.charAt(0) == '(') {
              type = "finite";
            }
            // make sure the type is one of the three supported
            if (!(type.equals("finite") || type.equals("text") || type.equals("number") || type.equals("string") || type.equals("int") || type.equals("bool"))) {
              throw new SpecFileException("Type not valid for attribute " + line);
            }
            Configuration conf = new Configuration();

            // support for attribute options
            // if type is "finite", then get the list of possible values
            // also, "finite" type supports an options list
            boolean badValues = false;
            if (type.equals("finite")) {
              String list = null;
              badValues = true;
              LTStringTokenizer lts = new LTStringTokenizer(line, "(");
              if (lts.next() != null) {
                lts.setTerminator(")");
                list = lts.next();
                if (list != null) {
                  options = lts.rest();
                  values = (Vector) StringOp.toUniqList(list, ",");
                  if (values != null) {
                    tokens = new StringTokenizer(options, " ");
                    // add options to configuration
                    conf.parseStringTokenizer(tokens);
                    badValues = false;
                  }
                }
              }
            }
            else {
              // find options for other type
              LTStringTokenizer lts = new LTStringTokenizer(line, "(");
              if (lts.next() != null) {
                badValues = true;
                lts.setTerminator(")");
                options = lts.next();
                try {
                  tokens = new StringTokenizer(options, " ");
                }
                catch (Exception e) {
                  System.out.println("lts:" + lts);
                  System.out.println("options:" + options);
                  throw new SpecFileException("Error in spec file!  Check for illegal characters (such as terminators in bad places). \n" + e.getMessage());
                }
                // add options to configuration
                conf.parseStringTokenizer(tokens);
                badValues = false;
              }
            }

            if (badValues) {
              throw (new SpecFileException("No valid list of values given " + "for finite attribute: " + line));
            }

            AttributeDescription attribute = AttributeDescription.build(name, type, conf, values);
            // ****

            if (attribute != null) {
              if (name.equals(DataStream.LABELS_ATTR)) {
                ed.setLabel(attribute, lineCount);
              }
              else if (name.equals(DataStream.WEIGHT_ATTR)) {
                ed.setWeight(attribute, lineCount);
              }
              else if (name.equals(DataStream.INDEX_ATTR)) {
                ed.setIndex(attribute, lineCount);
              }
              else {
                ed.addAttribute(attribute);
              }

              // spit out some logging info
              if (Monitor.logLevel > 3) {
                String errors = conf.getUnSpecified();
                if (errors != null) {
                  Monitor.log("Options not specfied for attribute: " + name + "\n" + errors + "\n");
                }
                errors = conf.unused();
                if (errors != null) {
                  Monitor.log("Options specfied but not used for attribute: " + name + "\n" + errors + "\n");
                }
              }
              // this line was parsed properly
              parsed = true;
            }
          }
        }
      }
    }
    if (!parsed) {
      throw (new SpecFileException("Cannot parse Spec file line:\n" + line));
    }
  }

}
