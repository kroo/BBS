package jboost.monitor;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;

import jboost.Predictor;
import jboost.booster.Booster;
import jboost.booster.RobustBoost;
import jboost.controller.Configuration;
import jboost.examples.ExampleSet;

/**
 * A class whose responsibility is to generate the log files that will later be
 * analyzed using scripts
 * 
 * @author Yoav Freund
 */
public class Monitor {

  private static Date startTime;
  private static Date afterInitTime;
  private static Date endTime;
  private String outputStem;

  private static final String FIELD_SEPARATOR = ": ";
  private static final char SEPARATOR = ',';

  /** a high-level log of the program's progress. */
  private String infoFilename;
  private PrintWriter infoStream;

  /** training and testing streams and variables */
  private int boostingPrintRate;
  private PrintWriter trainBoostingStream;
  private String trainBoostingFilename;
  private ByteArrayOutputStream trainBoostingBuffer;
  private PrintWriter testBoostingStream;
  private String testBoostingFilename;
  private ByteArrayOutputStream testBoostingBuffer;

  /**
   * a stream for logging resampling activity private PrintWriter
   * samplingStream; private String samplingOutputFilename;
   */

  /** log file info */
  private static String logOutputFilename;
  private static PrintWriter logStream;

  private ExampleSet trainSet; // the training ExampleSet
  private ExampleSet testSet; // the test ExampleSet
  private Booster m_booster; // used to get theoretical bound and m_margins
  /**
   * a public variable that stores the logging level for this run. The variable
   * should be checked before each call to log(). Calls to log should be of the
   * form</br> <tt>
   if(Monitor.logLevel> 5) Monitor.log("log message");
   </tt>
   */
  public static int logLevel = 0;

  public static void init_log(Configuration config) throws IOException {
    String stem = config.getString("S", "data");
    logOutputFilename = config.getString("log", stem + ".log");
    logLevel = config.getInt("loglevel", 2);
    if (logLevel < 2) {
      logStream = new PrintWriter(System.out);
    }
    else {
      logStream = new PrintWriter(new BufferedWriter(new FileWriter(logOutputFilename)));
    }
    startTime = new Date(); // remember time at start to report it later
  }

  /** a central place to print debugging logs */
  public static void log(Object message) {
    logStream.println(message);
  }

  /** close the logging file */
  public static void closeLog() {
    logStream.close();
  }

  /**
   * The constructor
   * 
   * @param config
   *            a configuration object with the run-time parameters
   * @param trainSet
   *            the training set (to calcualte training error)
   * @param testSet
   *            the test set
   * @param m_booster
   *            the m_booster (to compute m_margins)
   */
  public Monitor(Booster booster, ExampleSet training, ExampleSet testing, Configuration config) {
    trainSet = training;
    testSet = testing;
    m_booster = booster;
    outputStem = config.getString("S", "noname_out");
    infoFilename = config.getString("info", outputStem + ".info");

    trainBoostingFilename = outputStem + ".train.boosting.info";
    testBoostingFilename = outputStem + ".test.boosting.info";

    try {
      infoStream = new PrintWriter(new BufferedWriter(new FileWriter(infoFilename)));
      infoStream.println("Command line parameters: " + config.getString("args"));
      infoStream.println();
      infoStream.println("Configuration parameters:\n" + config);
      infoStream.println();

      // RobustBoost: binary case
      if (m_booster instanceof RobustBoost) {
        RobustBoost rb = (RobustBoost) m_booster;
        infoStream.println("RobustBoost parameters:");
        infoStream.println(rb.getParameters());
        infoStream.println();
      }

      infoStream.println("FILENAMES");
      infoStream.println("specFilename = " + config.getSpecFileName());
      infoStream.println("trainFilename = " + config.getTrainFileName());
      infoStream.println("testFilename = " + config.getTestFileName());
      infoStream.println("trainBoostingInfo = " + trainBoostingFilename);
      infoStream.println("testBoostingInfo = " + testBoostingFilename);
      infoStream.println("resultOutputFilename = " + config.getResultOutputFileName());
      infoStream.println("logOutputFilename = " + logOutputFilename);
      infoStream.println("");
      infoStream.println("Train set size = " + trainSet.getExampleNo());
      infoStream.println("Test set size = " + testSet.getExampleNo());
      infoStream.println("");
      boostingPrintRate = config.getInt("a", 0);

      trainBoostingStream = new PrintWriter(new BufferedWriter(new FileWriter(trainBoostingFilename)));
      testBoostingStream = new PrintWriter(new BufferedWriter(new FileWriter(testBoostingFilename)));

      trainBoostingBuffer = new ByteArrayOutputStream();
      testBoostingBuffer = new ByteArrayOutputStream();

      afterInitTime = new Date();
      infoStream.println("Init  Start time = " + startTime);
      infoStream.println("Learn Start time = " + afterInitTime);

      // RobustBoost: binary case
      if (booster instanceof RobustBoost) {
        infoStream.println("iter \tbound \ttrain \ttest \ttime");
      }
      else {
        infoStream.println("iter \tbound \ttrain \ttest");
      }

      infoStream.flush();
    }
    catch (IOException e) {
      throw new RuntimeException("monitor failed to open file for output\n" + e.getMessage());
    }
  }

  /** print the m_labels of trainSet and testSet onto samplingStream */
  /*
   * private void logLabels() { ArrayList labels= trainSet.getBinaryLabels();
   * samplingStream.println("train labels, elements=" + labels.size()); for (int
   * i= 0; i < labels.size(); i++) { samplingStream.println(((Boolean)
   * labels.get(i)).booleanValue() ? "+1" : "-1"); } labels.clear(); // release
   * memory labels= testSet.getBinaryLabels(); samplingStream.println("test
   * labels, elements=" + labels.size()); for (int i= 0; i < labels.size(); i++) {
   * samplingStream.println(((Boolean) labels.get(i)).booleanValue() ? "+1" :
   * "-1"); } labels.clear(); // release memory labels= null; }
   */

  /** generate logs for current boosting iteration */
  public void logIteration(int iter, Predictor combined, Predictor base) {
    double trainError = trainSet.calcError(iter, combined, base);
    double testError = testSet.calcError(iter, combined, base);
    double theoryBound = m_booster.getTheoryBound();
    NumberFormat f = new DecimalFormat("0.0000");

    if (iter > 0) infoStream.print("\n");
    // RobustBoost: binary case
    if (m_booster instanceof RobustBoost) {
      double currentTime = ((RobustBoost) m_booster).getCurrentTime();
      infoStream.print(iter + "\t" + f.format(theoryBound) + "\t" + f.format(trainError) + "\t" + f.format(testError) + "\t" + f.format(currentTime));
    }
    // otherwise
    else {
      infoStream.print(iter + "\t" + f.format(theoryBound) + "\t" + f.format(trainError) + "\t" + f.format(testError));
    }
    infoStream.flush();
    logBoosting(iter, combined, base);
  }

  /**
   * Provides logging for both the train and test sets.
   */
  private void logBoostingTrainTest(PrintWriter boostingStream, ExampleSet tSet, int iter, Predictor combined, Predictor base) {
    // Output the training data
    boostingStream.println("iteration=" + iter + FIELD_SEPARATOR + "elements=" + tSet.size() + FIELD_SEPARATOR + "boosting_params="
                           + m_booster.getParamString() + FIELD_SEPARATOR);

    // Get the relavant data structures (arrays and lists)
    ArrayList tMargin = tSet.calcMargins(iter, combined, base);
    ArrayList tScores = tSet.calcScores(iter, combined, base);
    ArrayList tLabelIndices = tSet.getBinaryLabels();

    double[] tIndex = null;
    if (tSet.hasIndex()) tIndex = tSet.getIndexes();

    double[][] tWeights = null;
    double[][] tPotentials = null;
    if (boostingStream.equals(trainBoostingStream)) {
      tWeights = m_booster.getWeights();
      tPotentials = m_booster.getPotentials();
    }

    NumberFormat f = new DecimalFormat("0.00000");
    double[] tmp = null;
    Boolean[] labeltmp = null;
    int j = 0;
    for (int i = 0; i < tMargin.size(); i++) {
      // output the example number
      boostingStream.print("" + i + FIELD_SEPARATOR);

      // If available, output the example index
      if (tIndex != null) {
        boostingStream.printf("%.0f" + FIELD_SEPARATOR, tIndex[i]);
      }

      // output the margins
      // I think this field is not very useful and should be eliminated
      // (YoavFreund 9/9/08)
      tmp = ((double[]) tMargin.get(i));
      for (j = 0; j < tmp.length; j++) {
        boostingStream.print(f.format(tmp[j]));
        if (j != tmp.length - 1) boostingStream.print(SEPARATOR);
      }
      boostingStream.print(FIELD_SEPARATOR);

      // output the scores
      tmp = ((double[]) tScores.get(i));
      for (j = 0; j < tmp.length; j++) {
        boostingStream.print(f.format(tmp[j]));
        if (j != tmp.length - 1) boostingStream.print(SEPARATOR);
      }
      boostingStream.print(FIELD_SEPARATOR);

      if (boostingStream.equals(trainBoostingStream)) {
        // output the weights
        for (j = 0; j < tWeights[i].length; j++) {
          // output it in log scale
          boostingStream.print(f.format(Math.log(tWeights[i][j])));
          if (j != tmp.length - 1) boostingStream.print(SEPARATOR);
        }
        boostingStream.print(FIELD_SEPARATOR);

        // output the potentials
        for (j = 0; j < tPotentials[i].length; j++) {
          boostingStream.print(f.format(tPotentials[i][j]));
          if (j != tmp.length - 1) boostingStream.print(SEPARATOR);
        }
        boostingStream.print(FIELD_SEPARATOR);

      }

      // output the labels
      labeltmp = ((Boolean[]) tLabelIndices.get(i));
      for (j = 0; j < labeltmp.length; j++) {
        boostingStream.print(labeltmp[j].booleanValue() ? "+1" : "-1");
        if (j != tmp.length - 1) boostingStream.print(SEPARATOR);
      }
      boostingStream.print(FIELD_SEPARATOR);
      boostingStream.println("");
    }

    // release memory
    tMargin.clear();
    tScores.clear();
    tLabelIndices.clear();
    tMargin = null;
    tScores = null;
    tLabelIndices = null;
    tWeights = null;
    tPotentials = null;
  }

  /** output the scores distribution of the training set */
  private void logBoosting(int iter, Predictor combined, Predictor base) {
    if ((boostingPrintRate == 0) || (boostingPrintRate > 0 && boostingPrintRate != iter)) return;

    if (boostingPrintRate == -1) {
      // print score when highest order digit in iter changes.
      double m = Math.floor(Math.log(iter) / Math.log(10.0));
      int t = (int) Math.pow(10.0, m);
      if (iter == 0) t = 1; // fix bug in "pow"
      if ((iter % t) != 0) return;
    }

    // save to buffer
    if (boostingPrintRate == -3) {
      trainBoostingBuffer.reset();
      testBoostingBuffer.reset();
      PrintWriter wTrain = new PrintWriter(trainBoostingBuffer);
      PrintWriter wTest = new PrintWriter(testBoostingBuffer);
      logBoostingTrainTest(wTrain, trainSet, iter, combined, base);
      logBoostingTrainTest(wTest, testSet, iter, combined, base);
      wTrain.close();
      wTest.close();
      return;
    }

    logBoostingTrainTest(trainBoostingStream, trainSet, iter, combined, base);
    logBoostingTrainTest(testBoostingStream, testSet, iter, combined, base);
    trainBoostingStream.flush();
    testBoostingStream.flush();
    infoStream.print(" \t# output boosting data #");
  }

  /** close the monitor output files */
  public void close() throws IOException {

    // print buffer to files
    if (boostingPrintRate == -3) {
      trainBoostingStream.print(trainBoostingBuffer.toString());
      testBoostingStream.print(testBoostingBuffer.toString());
      infoStream.print(" \t# output boosting data #");
    }

    endTime = new Date();
    infoStream.println("\nEnd time=" + endTime);

    // RobustBoost: binary case
    if (m_booster instanceof RobustBoost) {
      RobustBoost rb = (RobustBoost) m_booster;
      infoStream.println("\nrb_t = " + rb.getCurrentTime());
    }

    infoStream.close();

    if (trainBoostingStream != null) trainBoostingStream.close();
    if (testBoostingStream != null) testBoostingStream.close();

    log("finished closing output files");
  }
}
