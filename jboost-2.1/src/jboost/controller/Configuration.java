package jboost.controller;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

import jboost.monitor.Monitor;
import jboost.util.FileLoader;

/**
 * This class provides the utilities for parsing the command line which
 * specifies the setting of the options in the different packages.
 * <p>
 * The syntax of the command line is as follows
 * <ul>
 * <li>A list of options seperated by spaces
 * <li>each option consists of a name and an (optional) value. seperated by
 * spaces
 * <li>the name (flag) is of the form -[a-zA-Z]\m_w* As a standard, we suggest
 * that options that are associated with a particular package are of the form
 * "\s*_\s*" where the first string is the name of the package and the second
 * identifies the particular option.
 * <li>the value can be either empty, a number, a string , or a sequence of
 * numbers or strings seperated by commas. The value cannot start with a "-\m_w"
 * </ul>
 * <p>
 * The option to specify the configuration file is CONFIG. Options have the
 * following precedence: command line, environment, configuration file.
 * <p>
 * All arguments to the software will be accessed through this class.
 * <p>
 * For example:
 * <p>// Construct a Configuration in the Controller
 * <p>
 * Configuration p=new Configuration("jboost.man"); // Specifies the man file.
 * <p>
 * p.parseArgv(argv); // Parse the command line
 * <p>
 * p.parseCommandFile("jboost.config"); // Parse the configuration file.
 * <p>
 * 
 * @author Nigel Duffy (parts rewritten by Aaron Arvey)
 */
public class Configuration {

  private HashMap m_parsedCommands;
  private Vector m_validCommands;
  private String m_unSpecified;

  public final static String VERSION = "2.0";

  private final static String m_usage =
      "" + "jboost Version: " + VERSION + "\n" + "\n" + "******** Config Options:\n" + "\n" + "  -p N       Specify number of threads (default: 1)\n"
          + "  -CONFIG    The name of the configuration file (default \"jboost.config\")\n"
          + "             All options can be specified in this file instead of\n" + "             on the command line.\n"
          + "  -V         Print version and exit\n" + "\n" + "******** Data File Options:\n" + "\n"
          + "  -S stem        Base (stem) name for the files (default: \"data\")\n" + "  -n file.spec   Specfile name (default: stem+\".spec\")\n"
          + "  -t file.train  Training file name (default: stem+\".train\")\n" + "  -T file.test   Test file name (default: stem+\".test\")\n"
          + "  -serialTreeInput file.tree    Java object output of adtree (can be loaded\n" + "                                at a later date)\n"
          + "  -weightThreshold T    Set threshold for accepting an example\n" + "\n" + "******** Boosting Options:\n" + "\n"
          + "  -b type   The type of booster to use (default: AdaBoost).\n" + "            AdaBoost     Loss function: exp(-margin)\n"
          + "            LogLossBoost Loss: log(1 + exp(-margin))\n" + "            RobustBoost  Loss: min(1,1-erf((margin - mu(time))/sigma(time)))\n"
          + "  -numRounds N  The number of rounds of boosting that are to be executed.\n"
          + "                This option should be used with AdaBoost and LogitBoost\n"
          + "  -ATreeType type   The type of ATree to create.  There are several options:\n" + "     ADD_ALL               Create a full ADTree (default)\n"
          + "     ADD_ROOT              Add splits only at the root producing a glat tree.\n"
          + "                           This is equivalent to boosting decision stumps\n" + "     ADD_SINGLES           Create a decision tree\n"
          + "     ADD_ROOT_OR_SINGLES   Create a linear combination of decision trees.\n"
          + "                             This is equivalent to simultaneously growing \n" + "                             boosted decision trees.\n"
          + "  -BoosTexter           Only make a zero prediction at the root node.\n" + "  -booster_smooth sf   Smoothing factor for prediction computation\n"
          + "                       (default: 0.5) Described Schapire & Singer 1999\n" + "                       (smoothing the predictions), \n"
          + "                       $epsilon = sf / total_num_examples$  \n" + "\n" + "******** RobustBoost Options:\n" + "\n"
          + "  -rb_time       NUM          See documentation.\n" + "  -rb_epsilon    NUM          See documentation.\n"
          + "  -rb_theta      NUM          See documentation.\n" + "  -rb_theta_0    NUM          See documentation.\n"
          + "  -rb_theta_1    NUM          See documentation.\n" + "  -rb_sigma_f    NUM          See documentation.\n"
          + "  -rb_sigma_f_0  NUM          See documentation.\n" + "  -rb_sigma_f_1  NUM          See documentation.\n"
          + "  -rb_cost_0     NUM          See documentation.\n" + "  -rb_cost_1     NUM          See documentation.\n"
          + "  -rb_conf_rated <true|false> See documentation.\n" + "  -rb_potentialSlack   NUM    See documentation.\n" + "\n" + "******** Output Options:\n"
          + "\n" + "  -O file.tree   Output tree file name (default: stem+\".output.tree\")\n"
          + "  -serialTreeOutput file.tree   Java object output of adtree (can be loaded\n" + "                                at a later date)\n"
          + "  -P filename    Output Python code file name (default: stem+\".output.py\"\n"
          + "  -j filename    Output java code file name (default: stem+\".output.java\"\n"
          + "  -c filename    Output C code file name (default: stem+\".output.c\")\n"
          + "  -m filename    Output matlab code file name (default: stem+\".output.java\"\n"
          + "  -cOutputProc name  Name of procedure for output C code (default: 'predict')\n"
          + "  -javaStandAlone    Output java code that can stand alone, but\n" + "                     cannot read jboost-format data\n"
          + "  -javaOutputClass name     Name of class for output java code\n" + "                                          (default: 'Predict')\n"
          + "  -javaOutputMethod name    Name of method for output java code\n" + "                                          (default: 'predict')\n" + "\n"
          + "******** Logging Options:\n" + "\n" + "  -info filename      High-level log file name (default: stem+\".info\")\n"
          + "  -log  filename      Debugging log (default stem+\".log\")\n" + "  -loglevel N   Amount of information to be output to log \n"
          + "                The larger N is, the more information will be output.\n" + "                This is meant to be used as a debugging tool.\n"
          + "  -a iter      Generate margin (score) logs \n" + "               iter>0   log only on iteration iter,\n"
          + "               iter=-1  log on iters 1,2..9,10,20,...,90,100,200 ...)\n" + "               iter=-2  log on all iterations\n"
          + "               iter=-3  log only on the last iteration of boosting\n";

  /**
   * Constructor - takes no parameters, requiring the list of options to be
   * filled later
   */
  public Configuration() {
    m_parsedCommands = new HashMap(20);
    m_validCommands = new Vector(20);
    m_unSpecified = new String();
  }

  /**
   * Default Constructor - parses the command line and gets parameters from the
   * default configuration file "jboost.config" unless an alternative is
   * specified in -CONFIG
   * 
   * @param Name
   *            of the man page file
   * @param list
   *            of arguments from the command line
   */
  public Configuration(String manfile, String[] argv) throws IOException, BadCommandException {
    m_parsedCommands = new HashMap(20);
    m_validCommands = new Vector(20);
    m_unSpecified = new String();
    String tmp = null;
    parseArgv(argv);
    String commandfile = getString("CONFIG", "jboost.config");
    parseCommandFile(commandfile);
    initControllerOptions(manfile, argv);
  }

  /** Print m_usage message */
  public void printUsage() {
    System.out.print(m_usage);
    if (Monitor.logLevel > 3) {
      Monitor.log(m_usage);
    }
  }

  /** Print out all parsed commands that this configuration currently contains. */
  public String toString() {
    String retval = new String();
    Set commands = m_parsedCommands.keySet();
    for (Iterator iter = commands.iterator(); iter.hasNext();) {
      String option = (String) iter.next();
      retval += option + " = " + m_parsedCommands.get(option) + "\n";
    }
    return retval;
  }

  /**
   * Parse the list of arguments given on the command line
   * 
   * @param argv
   *            the array containing the parameters for this configuration
   */
  public void parseArgv(String[] argv) throws BadCommandException {
    int i = 0;
    String command = null;
    if (argv != null) {
      String args = "";
      for (i = 0; i < argv.length; i++) {
        if (argv[i].charAt(0) != '-') {
          throw (new BadCommandException("Flag does not start with '-': " + argv[i]));
        }

        if (!Character.isLetter(argv[i].charAt(1))) {
          throw (new BadCommandException("Name does not start with a letter: " + argv[i]));
        }

        /** What if value is empty? */
        if ((i + 1) >= argv.length || (argv[i + 1].charAt(0) == '-' && Character.isLetter(argv[i + 1].charAt(1)))) {
          command = argv[i].substring(1);
          m_parsedCommands.put(command, "true");
        }
        else {
          /** else the value is not empty */
          command = argv[i].substring(1);
          m_parsedCommands.put(command, argv[i + 1]);
          i++;
        }
        args = args + " " + argv[i];
      }
      addOption("args", args);
    }
  }

  /**
   * Parse a string tokenizer that provides arguments in the same
   * 
   * @param st
   */
  public void parseStringTokenizer(StringTokenizer st) throws BadCommandException {
    String[] args = new String[st.countTokens()];
    int i = 0;
    while (st.hasMoreTokens()) {
      args[i] = st.nextToken();
      System.out.println(args[i]);
      i++;
    }
    parseArgv(args);
  }

  /**
   * Parse a string that contains arguments in the same form as a command line
   * 
   * @param command
   */
  public void parseCommandString(String command) throws BadCommandException {
    parseStringTokenizer(new StringTokenizer(command));
  }

  /**
   * Parse a file of strings that contain arguments in the same form as a
   * command line
   * 
   * @param filename
   *            containing configuration info
   */
  public void parseCommandFile(String filename) throws IOException, BadCommandException {
    String tmp = null;
    BufferedReader f = null;
    try {
      // Class c= this.getClass();
      f = new BufferedReader(FileLoader.createFileReader(filename));
      if (f == null) {
        throw new FileNotFoundException();
      }
    }
    catch (FileNotFoundException e) {
      String s = "WARNING: configuration file " + filename + " not found.  Continuing...";
      System.out.println(s);
      return;
    }
    tmp = f.readLine();
    while (tmp != null) {
      parseCommandString(tmp);
      tmp = f.readLine();
    }
  }

  /* service methods */

  /**
   * Returns the integer value corresponding to an option name or the def value
   * If the name is not contained in the parsedCommands, then returl the default
   * value, after updating the list of unspecified configuration options.
   * 
   * @return retval the value of the configuration parameter
   */
  public int getInt(String name, int def) {
    String v = null;
    int retval = def;
    if (m_parsedCommands.containsKey(name)) {
      try {
        v = (String) m_parsedCommands.get(name);
        retval = Integer.parseInt(v);
      }
      catch (NumberFormatException e) {
        if (Monitor.logLevel > 3) Monitor.log("Error Parsing Option: " + v);
      }
    }
    else {
      m_unSpecified += name + ",";
    }
    return (retval);
  }

  /**
   * Returns the double value corresponding to an option name or the def value
   * If the name is not contained in the parsedCommands, then returl the default
   * value, after updating the list of unspecified configuration options.
   * 
   * @param name
   *            of the option
   * @param def
   *            default value if option name is not found
   * @return retval the value of the configuration parameter
   */
  public double getDouble(String name, double def) {
    String v = null;
    double retval = def;
    if (m_parsedCommands.containsKey(name)) {
      try {
        v = (String) m_parsedCommands.get(name);
        retval = Double.parseDouble(v);
      }
      catch (NumberFormatException e) {
        if (Monitor.logLevel > 3) Monitor.log("Error Parsing Option: " + v);
      }
    }
    else {
      m_unSpecified += name + ",";
    }
    return retval;
  }

  /**
   * returns the String value corresponding to an option name or the def value
   * if parsing fails or the option is not found
   * 
   * @param name
   *            of the configuration parameter
   * @param def
   *            default value to use
   * @return retval the value of the configuration parameter
   */
  public String getString(String name, String def) {
    String retval = def;
    if (m_parsedCommands.containsKey(name)) {
      retval = (String) m_parsedCommands.get(name);
    }
    else {
      m_unSpecified += name + ",";
    }
    return retval;
  }

  /**
   * Returns a String value corresponding to the option name, but uses null as
   * the default
   * 
   * @param name
   * @return
   */
  public String getString(String name) {
    return getString(name, null);
  }

  /**
   * returns the a boolean value which is true if the option is set false
   * otherwise.
   * 
   * @param name
   *            of the configuration parameter
   * @param def
   *            default value to use
   * @return retval the value of the configuration parameter
   */
  public boolean getBool(String name, boolean def) {
    String v = null;
    boolean retval = def;
    if (m_parsedCommands.containsKey(name)) {
      v = (String) m_parsedCommands.get(name);
      if (v.equals("+") || v.equalsIgnoreCase("true") || v.equals("")) {
        retval = true;
      }
      else if (v.equals("-") || v.equalsIgnoreCase("false")) {
        retval = false;
      }
      else if (Monitor.logLevel > 3) {
        Monitor.log("Inappropriate command argument " + v + " received with option " + name);
      }
    }
    else {
      m_unSpecified += name + ",";
    }
    return retval;
  }

  /**
   * returns the array of Strings corresponding to an option name or the def
   * value if parsing fails or the option is not found
   * 
   * @param name
   *            of the configuration parameter
   * @param def
   *            default value to use
   * @param retval
   *            the value of the configuration parameter
   */
  public String[] getStringArray(String name, String[] def) {
    String v = null;

    if (m_parsedCommands.containsKey(name)) {
      v = (String) m_parsedCommands.get(name);
      try {
        StringTokenizer st = new StringTokenizer(v, ",");
        int tokCnt = st.countTokens();
        if (tokCnt != 0) {
          String[] retval = new String[tokCnt];
          int i = 0;
          while (st.hasMoreTokens()) {
            retval[i] = st.nextToken();
            i++;
          }
          // return the tokenized String array
          return (retval);
        }
      }
      catch (NumberFormatException e) {
        if (Monitor.logLevel > 3) Monitor.log("Error Parsing Option: " + v);

      }
    }
    else {
      m_unSpecified += name + ",";
    }
    return (def);
  }

  /**
   * returns the array of integers corresponding to an option name or the def
   * value if parsing fails or the option is not found
   * 
   * @param name
   *            of the configuration parameter
   * @param def
   *            default value to use
   * @param retval
   *            the value of the configuration parameter
   */
  public int[] getIntArray(String name, int[] def) {
    String v = null;
    if (m_parsedCommands.containsKey(name)) {
      v = (String) m_parsedCommands.get(name);
      try {
        StringTokenizer st = new StringTokenizer(v, ",");
        int tokCnt = st.countTokens();
        if (tokCnt != 0) {
          int[] retval = new int[tokCnt];
          int i = 0;
          while (st.hasMoreTokens()) {
            retval[i] = Integer.parseInt(st.nextToken());
            i++;
          }
          // return the tokenized int array
          return (retval);
        }
      }
      catch (NumberFormatException e) {
        if (Monitor.logLevel > 3) Monitor.log("Error Parsing Option: " + v);

      }
    }
    else {
      m_unSpecified += name + ",";
    }
    return (def);
  }

  /**
   * returns the array of doubles corresponding to an option name or the def
   * value if parsing fails or the option is not found
   * 
   * @param name
   *            of the configuration parameter
   * @param def
   *            default value to use
   * @param retval
   *            the value of the configuration parameter
   */
  public double[] getDoubleArray(String name, double[] def) {
    String v = null;
    if (m_parsedCommands.containsKey(name)) {
      v = (String) m_parsedCommands.get(name);
      try {
        StringTokenizer st = new StringTokenizer(v, ",");
        int tokCnt = st.countTokens();
        if (tokCnt != 0) {
          double[] retval = new double[tokCnt];
          int i = 0;
          while (st.hasMoreTokens()) {
            retval[i] = Double.parseDouble(st.nextToken());
            i++;
          }
          // return the tokenized double array
          return (retval);
        }
      }
      catch (NumberFormatException e) {
        if (Monitor.logLevel > 3) Monitor.log("Error Parsing Option: " + v);

      }
    }
    else {
      m_unSpecified += name + ",";
    }
    return (def);
  }

  /**
   * Add an option directly
   * 
   * @param n
   *            name of option
   * @param v
   *            value for option
   */
  public void addOption(String n, String v) {
    m_parsedCommands.put(n, v);
  }

  /**
   * Remove the named option from the list of options
   * 
   * @param name
   */
  public void removeOption(String name) {
    m_parsedCommands.remove(name);
  }

  /** Adds an option but checks to see if it is valid first */
  public void safeAddOption(String n, String v) throws Exception {
    if (isValid(n)) m_parsedCommands.put(n, v);
    else throw (new Exception("Invalid option " + n));
  }

  /**
   * Add a valid option this will be added to a list of valid options which can
   * be used to check if the input is reasonable
   */
  public void addValid(String n) {
    m_validCommands.add(n);
  }

  /**
   * Checks to see if a name is in the valid list
   * 
   * @param name
   *            of the command
   * @return true if the name is contained in the list of valid commands
   */
  public boolean isValid(String n) {
    int s = m_validCommands.size();
    int i;
    String command = null;
    for (i = 0; i < s; i++) {
      command = (String) m_validCommands.get(i);
      if (command.equals(n)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Searches the list of options for one with the given name private Command
   * findName(String n) { int s= m_parsedCommands.size(); int i; Command tmp=
   * null; for (i= 0; i < s; i++) { tmp= (Command) m_parsedCommands.get(i); if
   * (tmp.equals(n)) return (tmp); } return (null); }
   */

  /**
   * Return the list of unspecified options
   * 
   * @return list of unspecified options
   */
  public String getUnSpecified() {
    if (m_unSpecified != null) {
      return m_unSpecified.substring(0, m_unSpecified.length() - 1);
    }
    else {
      return null;
    }
  }

  /**
   * Yields a string containing all the configuration options not used
   */
  public String unused() {
    String retval = new String();
    /*
     * Command tmp= null; int i= 0; for (i= 0; i < m_parsedCommands.size(); i++) {
     * tmp= (Command) m_parsedCommands.get(i); if (tmp.getCount() == 0) retval +=
     * tmp + ","; } if (retval.length() > 0) retval= retval.substring(0,
     * retval.length() - 1); else retval= null; return (retval); }
     */
    return retval;
  }

  /**
   * The Configuration holds all the configurable data used by the Controller,
   * Monitor, and anything else that needs to get access to it. Using this
   * configuration object allows us to reduce the exposure of the Controller to
   * other classes that may need to share data with it. The data that this class
   * maintains are:
   * <li> specFileName
   * <li> trainFileName
   * <li> testFileName
   * <li> resultOutputFileName
   * <li> C output FileName
   * <li> Java output FileName
   * <li> matlab output FileName
   * <li> serialized Output file name
   * <li> trainSet
   * <li> testSet
   */
  // These are the option keywords used for this Configuration
  public static final String SPEC_FILENAME = "specFileName";
  public static final String TRAIN_FILENAME = "trainFileName";
  public static final String TEST_FILENAME = "testFileName";
  public static final String RESULTOUTPUT_FILENAME = "resultOutputFileName";
  public static final String C_OUTPUT_FILENAME = "cCodeOutputFileName";
  public static final String JAVA_OUTPUT_FILENAME = "javaCodeOutputFileName";
  public static final String MATLAB_OUTPUT_FILENAME = "matlabCodeOutputFileName";
  public static final String PYTHON_OUTPUT_FILENAME = "pythonCodeOutputFileName";
  public static final String SERIALIZED_OUTPUT_FILENAME = "serializationOutputFileName";
  public static final String SERIALIZED_INPUT = "serialTreeInput";
  public static final String SAMPLE_THRESHOLD_WEIGHT = "weightThreshold";
  public static final String N_THREADS = "nthreads";
  public static final String BOOSTER_RUNTIME = "boostingRuntime";
  public static final String BOOSTER_TYPE = "booster_type";
  public static final String JBOOST_VERSION = "version";

  public static final String DEFAULT_BOOSTER = "AdaBoost";

  public static final String ROBUSTBOOST_TIME = "rb_t";
  public static final String ROBUSTBOOST_EPSILON = "rb_epsilon";
  public static final String ROBUSTBOOST_THETA = "rb_theta";
  public static final String ROBUSTBOOST_THETA_0 = "rb_theta_0";
  public static final String ROBUSTBOOST_THETA_1 = "rb_theta_1";
  public static final String ROBUSTBOOST_SIGMA_F = "rb_sigma_f";
  public static final String ROBUSTBOOST_SIGMA_F_0 = "rb_sigma_f_0";
  public static final String ROBUSTBOOST_SIGMA_F_1 = "rb_sigma_f_1";
  public static final String ROBUSTBOOST_COST_0 = "rb_cost_0";
  public static final String ROBUSTBOOST_COST_1 = "rb_cost_1";
  public static final String ROBUSTBOOST_CONF_RATED = "rb_conf_rated";

  /**
   * Configure the specific entries that we need for the Controller
   */
  private void initControllerOptions(String manpage, String[] argv) throws BadCommandException, IOException {
    String stem = getString("S", "default");
    addOption(SPEC_FILENAME, getString("n", stem + ".spec"));
    addOption(TRAIN_FILENAME, getString("t", stem + ".train"));
    addOption(TEST_FILENAME, getString("T", stem + ".test"));
    addOption(RESULTOUTPUT_FILENAME, getString("O", stem + ".output.tree"));
    addOption(C_OUTPUT_FILENAME, getString("c", null));
    addOption(JAVA_OUTPUT_FILENAME, getString("j", null));
    addOption(MATLAB_OUTPUT_FILENAME, getString("m", null));
    addOption(PYTHON_OUTPUT_FILENAME, getString("P", null));
    addOption(SERIALIZED_OUTPUT_FILENAME, getString("serialTreeOutput", null));
    addOption(SERIALIZED_INPUT, getString("serialTreeInput", null));
    addOption(N_THREADS, getString("p", null));
    addOption(BOOSTER_RUNTIME, getString("r", "1.0"));
    addOption(BOOSTER_TYPE, "jboost.booster." + getString("b", DEFAULT_BOOSTER));
    addOption(JBOOST_VERSION, getString("V", null));
  }

  /**
   * Check the values of all the parameters, e.g. make sure that the filenames
   * for the input files exist.
   * 
   * @return
   */
  protected boolean checkCommandValues() throws BadCommandException {
    // check the input files
    try {
      if (getPrintVersion()) {
        System.out.println("JBoost version " + VERSION);
        System.exit(0);
      }
      String specname = getSpecFileName();
      String testname = getTestFileName();
      String trainname = getTrainFileName();
      Monitor.log("Spec: " + specname + ", Test: " + testname + ", Train: " + trainname);
      if (specname == null || testname == null || trainname == null) {
        throw new BadCommandException("ERROR: Have not assigned all (spec,train,test) input file names yet!");
      }
      File specfile = new File(specname);
      File testfile = new File(testname);
      File trainfile = new File(trainname);
      if (!(specfile.exists() && testfile.exists() && trainfile.exists())) {
        throw new BadCommandException("ERROR: Input file names (spec,train,test) do not exist!");
      }
    }
    catch (Exception e) {
      System.err.println("ERROR: Input files do not exist or are unreadable for other reasons!");
      throw new BadCommandException(e.getMessage());
    }

    // Check the results output file parameters
    try {
      String fname = getResultOutputFileName();
      if (fname == null) {
        throw new BadCommandException("ERROR: Have not assigned output file name yet!");
      }
    }
    catch (Exception e) {
      System.err.println("ERROR: File either does not exist or is unreadable for other reasons!");
      throw new BadCommandException(e.getMessage());
    }

    return true;
  }

  /**
   * @return Returns the resultOutputFileName.
   */
  public String getResultOutputFileName() {
    return getString(RESULTOUTPUT_FILENAME);
  }

  /**
   * @return Returns the specFileName.
   */
  public String getSpecFileName() {
    return getString(SPEC_FILENAME);
  }

  /**
   * @return Returns the testFileName.
   */
  public String getTestFileName() {
    return getString(TEST_FILENAME);
  }

  /**
   * @return Returns the trainFileName.
   */
  public String getTrainFileName() {
    return getString(TRAIN_FILENAME);
  }

  /**
   * @return Returns the name of the C output file
   */
  public String getCoutputFileName() {
    return getString(C_OUTPUT_FILENAME);
  }

  /**
   * @return Returns the name of the Java output file
   */
  public String getJavaOutputFileName() {
    return getString(JAVA_OUTPUT_FILENAME);
  }

  /**
   * @return Returns the name of the Matlab output file
   */
  public String getMatlabOutputFileName() {
    return getString(MATLAB_OUTPUT_FILENAME);
  }

  /**
   * @return Returns the name of the Python output file
   */
  public String getPythonOutputFileName() {
    return getString(PYTHON_OUTPUT_FILENAME);
  }

  /**
   * @return Returns the name of the serialized output file
   */
  public String getSerializationOutputFileName() {
    return getString(SERIALIZED_OUTPUT_FILENAME);
  }

  /**
   * @return Returns the name of the serialized input file
   */
  public String getSerializationInputFileName() {
    return getString(SERIALIZED_INPUT);
  }

  /**
   * @return Returns the specified number of threads
   */
  public String getNThreads() {
    return getString(N_THREADS);
  }

  /**
   * @return the runtime of the boosting algorithm (for adaptive versions of
   *         BBM)
   */
  public double getRuntime() {
    String runStr = getString(BOOSTER_RUNTIME);
    if (runStr == null) {
      return 0;
    }
    return Double.parseDouble(runStr);
  }

  /**
   * @return should we print the version?
   */
  public boolean getPrintVersion() {
    String str = getString(JBOOST_VERSION);
    if (str == null) {
      return false;
    }
    return true;
  }

}

/**
 * This class implements a single option storing its name and value
 */
class Command {

  String name;
  String value;
  int checkCount = 0;

  public boolean equals(Command c) {
    return (name.equals(c.name));
  }

  public boolean equals(String n) {
    return (name.equals(n));
  }

  public Command(String n, String v) {
    name = n;
    value = v;
  }

  public String getValue() {
    checkCount++;
    return (value);
  }

  public int getCount() {
    return (checkCount);
  }

  public String toString() {
    String retval = new String("-");
    retval += name + " " + value;
    return (retval);
  }
}
