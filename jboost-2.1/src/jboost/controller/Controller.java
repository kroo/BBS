package jboost.controller;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Iterator;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

import jboost.CandidateSplit;
import jboost.NotSupportedException;
import jboost.Predictor;
import jboost.WritablePredictor;
import jboost.atree.AlternatingTree;
import jboost.atree.InstrumentException;
import jboost.atree.InstrumentedAlternatingTree;
import jboost.booster.AbstractBooster;
import jboost.booster.Booster;
import jboost.booster.Prediction;
import jboost.booster.RobustBoost;
import jboost.examples.BadLabelException;
import jboost.examples.Example;
import jboost.examples.ExampleDescription;
import jboost.examples.ExampleSet;
import jboost.examples.Label;
import jboost.examples.LabelDescription;
import jboost.examples.WordTable;
import jboost.learner.IncompAttException;
import jboost.learner.SplitterBuilder;
import jboost.learner.SplitterBuilderFamily;
import jboost.monitor.Monitor;
import jboost.tokenizer.DataStream;
import jboost.tokenizer.ExampleStream;
import jboost.tokenizer.ParseException;
import jboost.tokenizer.jboost_DataStream;
import jboost.util.ExecutorSinglet;

/**
 * The Controller class is the primary source of execution for jboost code. This
 * class is designed to be used in an executable jar file. The main() method
 * will use configuration data to build a learner and read in the train and test
 * examples.
 */

public class Controller {

  // main objects
  private Booster m_booster;
  private InstrumentedAlternatingTree m_learningTree;
  private Vector m_splitterBuilderVector;

  // configuration object
  private Configuration m_config;

  // m_monitor
  private Monitor m_monitor;
  private ExampleStream m_trainStream;
  private ExampleStream m_testStream;
  private ExampleDescription m_exampleDescription;
  private int[] m_trainSetIndices;
  private AlternatingTree m_serializedTree;

  private ExampleSet m_trainSet;
  private ExampleSet m_testSet;

  private static final String DEFAULT_MANPAGE = "manpage";
  private static final int DEFAULT_NUMROUNDS = 100;

  public static void main(String[] argv) {
    Configuration configuration = null;
    Controller controller = null;
    try {
      // read the command line
      configuration = new Configuration(DEFAULT_MANPAGE, argv);
      Monitor.init_log(configuration);
      configuration.checkCommandValues();
      controller = new Controller(configuration);
      // the rest of the code can be called from an external main
      controller.startLearning();
      controller.outputLearningResults();
      shutdown();
    }
    catch (BadCommandException e) {
      configuration.printUsage();
      System.err.println("JBoost Exception: " + e.getMessage());
    }
    catch (Exception e) {
      System.err.println(e.getMessage());
      configuration.printUsage();
      e.printStackTrace();
    }
    finally {
      Monitor.closeLog();
    }
  }

  /**
   * Build a default controller
   */
  public Controller(Configuration configuration) throws Exception {
    m_config = configuration;
    init();
  }

  /**
   * Initialize the controller Read in data from the configuration and perform
   * operations that might generate exceptions
   */
  public void init() throws Exception {
    try {
      startTokenizer();
    }
    catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    // initialize thread pool, if required
    String nthreads_str = m_config.getNThreads();
    if (nthreads_str != null) { // something was specified
      int nthreads = Integer.parseInt(nthreads_str);
      if (nthreads > 0) {
        if (Monitor.logLevel > 1) {
          Monitor.log("Initializing thread pool of size " + nthreads);
        }
        ExecutorService pe = Executors.newFixedThreadPool(nthreads);
        ExecutorSinglet.setExecutor(pe);
      }
    }

    m_exampleDescription = m_trainStream.getExampleDescription();
    // initialize the m_booster
    LabelDescription labelDescription = m_exampleDescription.getLabelDescription();
    int noOfLabels = labelDescription.getNoOfValues();
    boolean multiLabel = labelDescription.isMultiLabel();
    m_booster = AbstractBooster.getInstance(m_config, noOfLabels, multiLabel);

    Monitor.log("The m_booster is: " + m_booster);
    if (Monitor.logLevel > 1) {
      Monitor.log("The m_booster is: " + m_booster);
    }

    // build the splitterBuilder array
    buildSplitterBuilderArray();

    // load serialized tree, if necessary
    String serializedFile = m_config.getString(Configuration.SERIALIZED_INPUT, null);
    if (serializedFile != null) {
      m_serializedTree = loadTree(serializedFile);
    }

    try {
      // read data
      readTrainData();
      System.out.println("Monitor log level: " + Monitor.logLevel);
      if (Monitor.logLevel > 0) {
        // XXX: why is this dependent on the logLevel?
        System.out.println("Reading test data");
        readTestData();
      }
    }
    catch (Exception e) {
      // XXX: what to do?
      e.printStackTrace();
      throw new InstantiationException(e.getMessage());
    }

    // initialize m_monitor
    m_monitor = new Monitor(m_booster, m_trainSet, m_testSet, m_config);
  }

  /**
   * Necessary shutdown procedures
   */
  public static void shutdown() {
    // shutdown pooled executor
    Executor e = ExecutorSinglet.getExecutor();
    if (e instanceof ThreadPoolExecutor) {
      ((ThreadPoolExecutor) e).shutdownNow();
    }
  }

  /**
   * Create the InstrumentedAlternatingTree or load it from a file. Then run the
   * learn method
   * 
   * @throws Exception
   */
  public void startLearning() throws Exception {
    initializeTree();
    executeMainLoop();
  }

  public void initializeTree() throws Exception {
    // build Alternating Tree
    if (m_serializedTree != null) {

      m_learningTree = new InstrumentedAlternatingTree(m_serializedTree, m_splitterBuilderVector, m_booster, m_trainSetIndices, m_config);

    }
    else {
      // initialize alternating tree
      m_learningTree = new InstrumentedAlternatingTree(m_splitterBuilderVector, m_booster, m_trainSetIndices, m_config);
      System.out.println("Finished creating root (iteration 0)");
      if (Monitor.logLevel > 1) {
        m_monitor.logIteration(0, // iteration
                               m_learningTree.getCombinedPredictor(), m_learningTree.getLastBasePredictor());
      }
    }
  }

  public void executeMainLoop() throws Exception {
    long start, stop;
    // main loop
    start = System.currentTimeMillis();
    learn(m_config.getInt("numRounds", DEFAULT_NUMROUNDS));
    stop = System.currentTimeMillis();

    if (Monitor.logLevel > 1) {
      Monitor.log("It took " + (stop - start) / 1000.0 + " seconds to learn.");
    }

  }

  /**
   * generate output files
   * 
   * @throws Exception
   */
  public void outputLearningResults() throws Exception {
    // output final result
    reportResults();

    WritablePredictor res = m_learningTree.getCombinedPredictor();

    if (Monitor.logLevel > 5) {
      Monitor.log(WordTable.globalTable);
    }

    // output a serialization of the alternating tree
    // String serializedFile= m_config.getSerializationOutputFileName();
    String serializedFile = m_config.getString("serialTreeOutput", null);
    if (serializedFile != null) {
      saveTree(res, serializedFile);
    }

    // output C code
    String cFile = m_config.getCoutputFileName();
    if (cFile != null) {
      generateCode(res, "C", cFile, m_config.getString("cOutputProc", "predict"));
    }

    // output Matlab code
    String matlab = m_config.getMatlabOutputFileName();
    if (matlab != null) {
      generateCode(res, "MatLab", matlab, m_config.getString("matlabOutputFunction", "predict"));
    }

    // output Matlab code
    String python = m_config.getPythonOutputFileName();
    if (python != null) {
      generateCode(res, "Python", python, m_config.getString("pythonOutputFunction", "predict"));
    }

    // output java code
    String java = m_config.getJavaOutputFileName();
    if (java != null) {
      generateCode(res, "java", java, m_config.getString("javaOutputClass", "predict"));
    }

    if (Monitor.logLevel > 1) {
      test(res);
    }

    m_monitor.close();
  }

  /**
   * Serialize the tree and the associated WordTable. No information about the
   * booster is stored. This tree can be reloaded and used with a completely
   * different booster.
   * 
   * @param atree
   * @param serializedTree
   * @throws Exception
   */
  public void saveTree(WritablePredictor atree, String serializedFile) throws Exception {

    if (serializedFile != null) {
      ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(serializedFile));
      oos.writeObject(atree);
      oos.writeObject(WordTable.globalTable);
      oos.close();
      System.out.println("Wrote tree to " + serializedFile);
    }
    else {
      throw new ConfigurationException("Missing output name for writing Alternting Tree.");
    }
  }

  /**
   * Load an existing tree from a file Instrument the tree using the examples
   * and booster specified in the configuration for this Controller.
   * 
   * @param serializedFile
   * @throws Exception
   */
  public AlternatingTree loadTree(String serializedFile) throws Exception {
    // output a serialization of the alternating tree
    m_exampleDescription = m_trainStream.getExampleDescription();
    AlternatingTree atree = null;
    ObjectInputStream input = new ObjectInputStream(new FileInputStream(serializedFile));
    atree = (AlternatingTree) input.readObject();
    WordTable.globalTable = (WordTable) input.readObject();
    System.out.println("Loaded tree from file " + serializedFile);

    return atree;
  }

  public WritablePredictor getPredictor() {
    return m_learningTree.getCombinedPredictor();
  }

  public ExampleStream getTrainStream() {
    return m_trainStream;
  }

  public ExampleStream getTestStream() {
    return m_testStream;
  }

  public Configuration getConfiguration() {
    return m_config;
  }

  public Booster getBooster() {
    return m_booster;
  }

  public InstrumentedAlternatingTree getTree() {
    return m_learningTree;
  }

  /**
   * The basic learner.
   * 
   * @param iterNo
   *            number of rounds to boost the learner
   * @throws NotSupportedException
   * @throws InstrumentException
   */
  private void learn(int iterNo) throws NotSupportedException, InstrumentException {
    // The stopping criterion should also be more general.
    long start, stop;
    double threshold = m_config.getDouble(Configuration.SAMPLE_THRESHOLD_WEIGHT, 0);
    Executor pe = ExecutorSinglet.getExecutor();
    for (int iter = 1; iter <= iterNo && !m_learningTree.boosterIsFinished(); iter++) {

      // System.out.println("Started learning iteration " + iter);

      start = System.currentTimeMillis();
      Vector candidates = m_learningTree.getCandidates();
      stop = System.currentTimeMillis();
      if (Monitor.logLevel > 3) {
        Monitor.log("Learning iteration " + iter + " candidates are:");
        Monitor.log(candidates.toString());
        Monitor.log("It took " + (stop - start) / 1000.0 + " seconds to generate candidates Vector for iteration " + iter);
      }

      // This piece should be replaced by a more general tool to
      // measure the goodness of a split.

      // Create a synchronization barrier that counts the number
      // of processed splitter builders
      CountDownLatch candidateCount = new CountDownLatch(candidates.size());

      // an array to record losses in
      double[] losses = new double[candidates.size()];

      int i = 0;
      for (Iterator ci = candidates.iterator(); ci.hasNext();) {
        CandidateSplit candidate = (CandidateSplit) ci.next();
        SplitEvaluatorWorker sew = new SplitEvaluatorWorker(candidate, losses, i, candidateCount);
        try {
          pe.execute(sew);
        }
        catch (RejectedExecutionException ie) {
          System.err.println("exception ocurred while handing off the candidate job to the pool: " + ie.getMessage());
          ie.printStackTrace();
        }
        i++;
      }

      try {
        candidateCount.await();
      }
      catch (InterruptedException ie) {
        if (candidateCount.getCount() != 0) {
          System.err.println("interrupted exception occurred, but the candidateCount is " + candidateCount.getCount());
        }
      }
      ;

      // run through the losses results to determine the best split
      int best = 0;
      if (losses.length == 0) {
        System.err.println("ERROR: There are no candidate weak hypotheses to add to the tree.");
        System.err.println("This is likely a bug in JBoost; please report to JBoost developers.");
        System.exit(2);
      }
      double bestLoss = losses[best];
      double tmpLoss;
      for (int li = 1; li < losses.length; li++) {
        if ((tmpLoss = losses[li]) < bestLoss) {
          bestLoss = tmpLoss;
          best = li;
        }
      }

      if (Monitor.logLevel > 3) {
        Monitor.log("Best candidate is: " + (CandidateSplit) candidates.get(best) + "\n");
      }

      // add the candidate with lowest loss
      start = System.currentTimeMillis();
      m_learningTree.addCandidate((CandidateSplit) candidates.get(best));
      stop = System.currentTimeMillis();
      if (Monitor.logLevel > 3) {
        Monitor.log("It took " + (stop - start) / 1000.0 + " seconds to add candidate for iteration " + iter);
      }

      // binary case of RobustBoost
      if (m_booster instanceof RobustBoost) {
        NumberFormat f = new DecimalFormat("0.00000");
        System.out.println("" + "[Iter= " + iter + ", Time= " + f.format(((RobustBoost) m_booster).getCurrentTime()) + ", Effective Examples= "
                           + f.format(((RobustBoost) m_booster).getEffectiveNumExamples()) + ", Active Examples= "
                           + ((RobustBoost) m_booster).getNumExamplesHigherThan(threshold) + "]");
      }
      else System.out.println("Finished learning iteration " + iter);

      if (Monitor.logLevel > 1) {
        m_monitor.logIteration(iter, m_learningTree.getCombinedPredictor(), m_learningTree.getLastBasePredictor());
      }
    }

    System.out.println("");
  }

  /**
   * @param cp
   *            a predictor
   * @throws IncompAttException
   */
  private void test(Predictor cp) throws NotSupportedException, InstrumentException {
    int size = m_testSet.getExampleNo();
    int i = 0;
    Example ex = null;
    Monitor.log("Testing rule.");
    LabelDescription labelDescription = m_exampleDescription.getLabelDescription();
    try {
      for (i = 0; i < size; i++) {
        ex = m_testSet.getExample(i);
        Prediction prediction = cp.predict(ex.getInstance());
        Label label = ex.getLabel();
        if (!prediction.getBestClass().equals(label)) {
          if (Monitor.logLevel > 3) {
            Monitor.log("Test Example " + i + "   -----------------------------");
            Monitor.log(ex);
            Monitor.log("------------------------------------------");
            Monitor.log(prediction);
            Monitor.log("Explanation: " + ((AlternatingTree) cp).explain(ex.getInstance()));
          }
        }
      }
    }
    catch (IncompAttException e) {
      // TODO add something here?
    }
  }

  /**
   * build the array of splitterBuilders
   * 
   * @throws IncompAttException
   */
  private void buildSplitterBuilderArray() throws IncompAttException {
    Vector sbf = SplitterBuilderFamily.factory(m_config);
    m_splitterBuilderVector = new Vector();

    for (int i = 0; i < sbf.size(); i++) {
      m_splitterBuilderVector.addAll(((SplitterBuilderFamily) sbf.get(i)).build(m_exampleDescription, m_config, m_booster));
    }

    if (Monitor.logLevel > 3) {
      Monitor.log("The initial array of splitter Builders is:");
      for (int i = 0; i < m_splitterBuilderVector.size(); i++) {
        Monitor.log("builder " + i + m_splitterBuilderVector.get(i));
      }
    }
  }

  /**
   * Add example to booster and training set. Update SplitterBuilder vector for
   * this example
   * 
   * @param counter
   * @param example
   * @param exampleWeight
   */
  private void addTrainingExample(int counter, Example example, double exampleWeight) {

    addTrainingExample(counter, example, exampleWeight, 0.0);

  }

  /**
   * Add example to booster and training set. Update SplitterBuilder vector for
   * this example
   * 
   * @param counter
   * @param example
   * @param exampleWeight
   * @param margin
   */
  private void addTrainingExample(int counter, Example example, double exampleWeight, double margin) {

    if (Monitor.logLevel > 0) {
      m_trainSet.addExample(counter, example);
    }

    m_booster.addExample(counter, example.getLabel(), exampleWeight, margin);

    for (int i = 0; i < m_splitterBuilderVector.size(); i++) {
      if (Monitor.logLevel > 5) {
        Monitor.log("the class of splitterBuilder " + i + " is " + m_splitterBuilderVector.get(i).getClass());
      }
      ((SplitterBuilder) m_splitterBuilderVector.get(i)).addExample(counter, example);
    }
  }

  /**
   * Read the training file and initialize the booster and the splitterBuilders
   * with its content
   * 
   * @throws BadLabelException
   * @throws IncompAttException
   * @throws ParseException
   */
  private void readTrainData() throws IncompAttException, ParseException {

    long start, stop;
    m_trainSet = new ExampleSet(m_exampleDescription);

    Example example = null;
    int counter = 0;
    int rejected = 0;
    start = System.currentTimeMillis();
    double threshold = m_config.getDouble(Configuration.SAMPLE_THRESHOLD_WEIGHT, 0);

    while ((example = m_trainStream.getExample()) != null) {
      double exampleWeight = example.getWeight();
      boolean accepted = true;
      double margin = 0.0;
      double weight = 0.0;
      // if we are sampling, then calculate the weight of this example
      // if the exampleWeight is zero, then don't accept this example

      if (m_serializedTree != null) {

        double[] margins = m_serializedTree.predict(example.getInstance()).getMargins(example.getLabel());
        margin = margins[0];

        // System.out.println("margin:"+margin);

        if (m_booster instanceof RobustBoost) {
          RobustBoost rb = (RobustBoost) m_booster;
          weight = rb.calculateWeight(example.getLabel(), margin, rb.getCurrentTime());
          // System.out.println("weight=" + weight);
        }
        else {
          weight = m_booster.calculateWeight(margin);
        }

        if (weight * exampleWeight < threshold) {
          accepted = false;
          rejected++;
        }

      }

      /*
       * exampleWeight= calculateExampleWeight(example, threshold); if
       * (exampleWeight < 0) { accepted= false; }
       */

      // The default behavior is to accept each example. An
      // example is only refused if we are sampling and its
      // weight is set to zero.
      if (accepted) {
        addTrainingExample(counter, example, exampleWeight, margin);
        // addTrainingExample(counter, example, exampleWeight);
        counter++;

        if ((counter % 100) == 0) {
          System.out.print("Read " + counter + " training examples\n");
        }
      }

    }

    stop = System.currentTimeMillis();
    System.out.println("Read " + counter + " training examples in " + (stop - start) / 1000.0 + " seconds.");
    System.out.println("Reject " + rejected + " training examples");
    m_trainSet.finalizeData();
    m_booster.finalizeData();
    for (int i = 0; i < m_splitterBuilderVector.size(); i++) {
      ((SplitterBuilder) m_splitterBuilderVector.get(i)).finalizeData();
    }
    m_trainSetIndices = new int[counter];
    for (int i = 0; i < counter; i++)
      m_trainSetIndices[i] = i;
  }

  /**
   * initialize the tokenizer
   * 
   * @throws Exception
   */
  private void startTokenizer() throws Exception {
    DataStream ds = null;
    ds = new jboost_DataStream(m_config.getSpecFileName(), m_config.getTrainFileName());
    m_trainStream = new ExampleStream(ds);
    ds = new jboost_DataStream(m_config.getSpecFileName(), m_config.getTestFileName());
    m_testStream = new ExampleStream(ds);
  }

  /** read the test data file */
  private void readTestData() throws BadLabelException, IncompAttException, ParseException {

    long start, stop;
    m_testSet = new ExampleSet(m_exampleDescription);
    Example example = null;
    int counter = 0;
    start = System.currentTimeMillis();
    while ((example = m_testStream.getExample()) != null) {
      m_testSet.addExample(counter, example);
      counter++;
      if ((counter % 100) == 0) {
        System.out.print("read " + counter + " test examples\n");
      }
    }
    stop = System.currentTimeMillis();
    System.out.println("read " + counter + " test examples in " + (stop - start) / 1000.0 + " seconds.");
    m_testSet.finalizeData();
  }

  public double[] getMarginsDistribution() {
    double[][] temp = m_booster.getMargins();
    double[] ret = new double[m_booster.getNumExamples()];
    for (int i = 0; i < ret.length; i++) {
      ret[i] = temp[i][0];
    }
    return ret;
  }

  private void reportResults() {
    try {
      PrintWriter resultOutputStream = new PrintWriter(new BufferedWriter(new FileWriter(m_config.getResultOutputFileName())));
      resultOutputStream.println(m_learningTree);
      resultOutputStream.close();
    }
    catch (Exception e) {
      System.err.println("Exception occured while attempting to write result");
      e.printStackTrace();
    }
  }

  private void generateCode(WritablePredictor predictor, String language, String codeOutputFileName, String procedureName) {
    try {
      String code = null;
      if (language.equals("C")) code = predictor.toC(procedureName, m_exampleDescription);
      else if (language.equals("MatLab")) code = predictor.toMatlab(procedureName, m_exampleDescription);
      else if (language.equals("Python")) code = predictor.toPython(procedureName, m_exampleDescription);
      else if (language.equals("java")) code =
          predictor.toJava(procedureName, m_config.getString("javaOutputMethod", "predict"),
                           (m_config.getBool("javaStandAlone", false) ? null : m_config.getSpecFileName()), m_exampleDescription);
      else throw new RuntimeException("Controller.generateCode: Unrecognized language:" + language);
      PrintWriter codeOutputStream = new PrintWriter(new BufferedWriter(new FileWriter(codeOutputFileName)));
      codeOutputStream.println(code);
      codeOutputStream.close();
    }
    catch (Exception e) {
      System.err.println("Exception occured while attempting to write " + language + " code");
      System.err.println("Message:" + e);
      e.printStackTrace();
    }
  }
}
