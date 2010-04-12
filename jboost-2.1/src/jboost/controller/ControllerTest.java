/*
 * Created on Jan 5, 2004
 */
package jboost.controller;

import java.io.File;

import jboost.atree.InstrumentedAlternatingTree;
import jboost.booster.AdaBoost;
import jboost.booster.LogLossBoost;
import jboost.monitor.Monitor;
import junit.framework.TestCase;

/**
 * @author cschavis
 */
public class ControllerTest extends TestCase {

  private Controller m_controller;
  private Configuration m_config;

  /*
   * @see TestCase#setUp()
   */
  protected void setUp() throws Exception {
    super.setUp();
    String[] args = { "-CONFIG", "src/jboost/controller/jboost.config" };
    System.out.println(args[1]);
    m_config = new Configuration(null, args);
    Monitor.init_log(m_config);
    m_controller = new Controller(m_config);

  }

  protected void setUpWeighted() throws Exception {
    String[] args = { "-CONFIG", "src/jboost/controller/weightedjboost.config" };
    System.out.println(args[1]);
    m_config = new Configuration(null, args);
    Monitor.init_log(m_config);
    m_controller = new Controller(m_config);
  }

  /*
   * @see TestCase#tearDown()
   */
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  /**
   * Test the processing of configuration options This test executes
   * getInputFileNames(), getSpecFileName(), getTrainFileName()
   * getTestFileName(), getResultOutputFileName()
   */
  public final void testConfigurationOptions() {

  }

  /**
   * Test building the default booster, which is AdaBoost This test passes in
   * different configuration options for the booster selection.
   */
  public final void testAdaBooster() {
    // first verify that the correct booster is set up by default.
    // should be AdaBoost
    Class boosterClass = m_controller.getBooster().getClass();
    AdaBoost adaboost = new AdaBoost();
    assertTrue(boosterClass.isInstance(adaboost));
  }

  /**
   * Test building the LogLoss booster
   */
  public final void testLogLossBooster() {
    // change booster type
    m_config.addOption("booster_type", "jboost.booster.LogLossBoost");
    try {
      m_controller = new Controller(m_config);
    }
    catch (Exception e) {
      fail("Unexepected Exception");
    }
    Class boosterClass = m_controller.getBooster().getClass();
    LogLossBoost logloss = new LogLossBoost();
    assertTrue(boosterClass.isInstance(logloss));
  }

  /**
   * Test building an invalid booster
   */
  public final void testBogusBooster() {
    // try to create controller with bogus booster
    m_config.addOption("booster_type", "jboost.booster.bogus");
    try {
      m_controller = new Controller(m_config);
      fail("Exception expected with invalid Booster name.");
    }
    catch (Exception success) {
    }
  }

  /**
   * Test the read/write tree functionality
   */
  public final void testLoadTree() {
    try {
      System.out.println("Learning from stream");
      m_controller.startLearning();
      m_controller.outputLearningResults();
      // set config option for loading tree
      m_config.addOption("serialTreeInput", "src/jboost/controller/atree.serialized");
      m_controller.startLearning();
      // do some sort of comparison?
    }
    catch (Exception e) {
      e.printStackTrace();
      fail();
    }
  }

  /**
   * Test demo files using AdaBoost Write out serialized tree, then re-read and
   * continue boosting. Compare output to non-serialized tree
   */
  public final void testAdaBoostCycle() {
    try {
      int rounds = 80;
      // learn for 40 rounds, write to file, reload and learn for 40 more rounds
      m_config.addOption("numRounds", Integer.toString(rounds / 2));
      m_controller.startLearning();
      m_controller.outputLearningResults();
      setUp();
      m_config.addOption("numRounds", Integer.toString(rounds / 2));
      m_config.addOption("serialTreeInput", "src/jboost/controller/atree.serialized");
      m_controller.startLearning();
      InstrumentedAlternatingTree firstTree = m_controller.getTree();

      // reset the controller and learn for 80 rounds
      setUp();
      m_config.addOption("numRounds", Integer.toString(rounds));
      m_config.addOption("serialTreeInput", null);
      m_controller.startLearning();
      InstrumentedAlternatingTree secondTree = m_controller.getTree();
      System.out.println(firstTree.toString());
      System.out.println(secondTree.toString());
      // assertTrue(firstTree.toString().equals(secondTree.toString()));
    }
    catch (Exception e) {
      e.printStackTrace();
      fail();
    }
  }

  /**
   * Test demo files using LogLossBoost Write out serialized tree, then re-read
   * and continue boosting. Compare output to non-serialized tree
   */
  public final void testLogLossBoostCycle() {
    try {
      int rounds = 80;
      // learn for 40 rounds, write to file, reload and learn for 40 more rounds
      m_config.addOption("numRounds", Integer.toString(rounds / 2));
      m_config.addOption("booster_type", "jboost.booster.LogLossBoost");
      m_controller = new Controller(m_config);
      m_controller.startLearning();
      m_controller.outputLearningResults();

      setUp();
      m_config.addOption("numRounds", Integer.toString(rounds / 2));
      m_config.addOption("booster_type", "jboost.booster.LogLossBoost");
      m_config.addOption("serialTreeInput", "src/jboost/controller/atree.serialized");
      m_controller = new Controller(m_config);
      m_controller.startLearning();
      InstrumentedAlternatingTree firstTree = m_controller.getTree();

      // reset the controller and learn for 80 rounds
      setUp();
      m_config.addOption("numRounds", Integer.toString(rounds));
      m_config.addOption("booster_type", "jboost.booster.LogLossBoost");
      m_config.addOption("serialTreeInput", null);
      m_controller = new Controller(m_config);
      m_controller.startLearning();
      InstrumentedAlternatingTree secondTree = m_controller.getTree();
      System.out.println(firstTree.toString());
      System.out.println(secondTree.toString());
      // assertTrue(firstTree.toString().equals(secondTree.toString()));
    }
    catch (Exception e) {
      e.printStackTrace();
      fail();
    }
  }

  /**
   * Test demo files using RobustBoost Write out serialized tree, then re-read
   * and continue boosting. Compare output to non-serialized tree
   */
  public final void testRobustBoostCycle() {
    try {
      int rounds = 40;
      // learn for 40 rounds, write to file, reload and learn for 40 more rounds
      m_config.addOption("numRounds", Integer.toString(rounds / 2));
      m_config.addOption("booster_type", "jboost.booster.RobustBoost");
      m_config.addOption("rb_t", "0");
      m_config.addOption("rb_epsilon", "0.1");
      m_config.addOption("rb_theta", "0.0");
      m_config.addOption("rb_sigma_f", "0.01");
      m_controller = new Controller(m_config);
      m_controller.startLearning();
      m_controller.outputLearningResults();
      double[] a1 = m_controller.getMarginsDistribution();

      m_config.addOption("numRounds", Integer.toString(rounds / 2));
      m_config.addOption("booster_type", "jboost.booster.RobustBoost");
      m_config.addOption("rb_t", "0.41584");
      m_config.addOption("rb_epsilon", "0.1");
      m_config.addOption("rb_theta", "0.0");
      m_config.addOption("rb_sigma_f", "0.01");
      m_config.addOption("serialTreeInput", "src/jboost/controller/atree.serialized");
      m_controller = new Controller(m_config);
      m_controller.initializeTree();
      double[] a2 = m_controller.getMarginsDistribution();
      m_controller.executeMainLoop();

      for (int i = 0; i < a1.length; i++) {
        assertEquals(a1[i], a2[i], 1e-7);
      }

      InstrumentedAlternatingTree firstTree = m_controller.getTree();

      // reset the controller and learn for 80 rounds
      m_config.addOption("numRounds", Integer.toString(rounds));
      m_config.addOption("booster_type", "jboost.booster.RobustBoost");
      m_config.addOption("rb_t", "0");
      m_config.addOption("rb_epsilon", "0.1");
      m_config.addOption("rb_theta", "0.0");
      m_config.addOption("rb_sigma_f", "0.01");
      m_config.addOption("serialTreeInput", null);
      m_controller = new Controller(m_config);
      m_controller.startLearning();
      InstrumentedAlternatingTree secondTree = m_controller.getTree();
      System.out.println(firstTree.toString());
      System.out.println(secondTree.toString());
      // assertTrue(firstTree.toString().equals(secondTree.toString()));
    }
    catch (Exception e) {
      e.printStackTrace();
      fail();
    }
  }

  /**
   * Test demo files using LogLossBoost with weighted examples Write out
   * serialized tree, then re-read and continue boosting. Compare output to
   * non-serialized tree
   */
  public final void testWeightedLogLossBoostCycle() {
    try {
      System.out.println("Running Weighted LogLoss booster cycle test.");
      int rounds = 100;
      String[] args = { "-CONFIG", System.getProperty("jboost.home") + File.separatorChar + "src/jboost/controller/weightedjboost.config" };
      m_config = new Configuration(null, args);
      // learn for 40 rounds, write to file, reload and learn for 40 more rounds
      m_config.addOption("numRounds", Integer.toString(rounds / 2));
      m_config.addOption("booster_type", "jboost.booster.LogLossBoost");
      m_config.addOption("j", "WeightedPredict");
      m_controller = new Controller(m_config);
      m_controller.startLearning();
      m_controller.outputLearningResults();

      m_config.addOption("numRounds", Integer.toString(rounds / 2));
      m_config.addOption("booster_type", "jboost.booster.LogLossBoost");
      m_config.addOption("serialTreeInput", "src/jboost/controller/atree.serialized");
      m_controller = new Controller(m_config);
      m_controller.startLearning();
      InstrumentedAlternatingTree firstTree = m_controller.getTree();

      // reset the controller and learn for 80 rounds
      m_config.addOption("numRounds", Integer.toString(rounds));
      m_config.addOption("booster_type", "jboost.booster.LogLossBoost");
      m_controller = new Controller(m_config);
      m_controller.startLearning();
      InstrumentedAlternatingTree secondTree = m_controller.getTree();
      System.out.println(firstTree.toString());
      System.out.println(secondTree.toString());
      // assertTrue(firstTree.toString().equals(secondTree.toString()));
    }
    catch (Exception e) {
      System.out.println(e.getMessage());
      e.printStackTrace();
      fail();
    }
  }

  /**
   * Test demo files using LogLossBoost with more than 2 labels Write out
   * serialized tree, then re-read and continue boosting. * Compare output to
   * non-serialized tree
   */
  public final void testMultiLabelLogLossBoostCycle() {
    try {
      System.out.println("Running Multilabel LogLoss booster cycle test.");
      int rounds = 100;
      String[] args = { "-CONFIG", System.getProperty("jboost.home") + File.separatorChar + "src/jboost/controller/multilabeljboost.config" };
      m_config = new Configuration(null, args);
      m_config.addOption("numRounds", Integer.toString(rounds / 2));
      m_config.addOption("booster_type", "jboost.booster.LogLossBoost");
      m_config.addOption("serialTreeOutput", "src/jboost/controller/multilabelatree.serialized");
      m_controller = new Controller(m_config);
      m_controller.startLearning();
      m_controller.outputLearningResults();

      m_config.addOption("numRounds", Integer.toString(rounds / 2));
      m_config.addOption("booster_type", "jboost.booster.LogLossBoost");
      m_config.addOption("serialTreeInput", "src/jboost/controller/multilabelatree.serialized");
      m_controller = new Controller(m_config);
      m_controller.startLearning();
      InstrumentedAlternatingTree firstTree = m_controller.getTree();

      // reset the controller and learn for 80 rounds
      m_config.addOption("numRounds", Integer.toString(rounds));
      m_config.addOption("booster_type", "jboost.booster.LogLossBoost");
      m_controller = new Controller(m_config);
      m_controller.startLearning();
      InstrumentedAlternatingTree secondTree = m_controller.getTree();
      System.out.println(firstTree.toString());
      System.out.println(secondTree.toString());
      // assertTrue(firstTree.toString().equals(secondTree.toString()));
    }
    catch (Exception e) {
      System.out.println(e.getMessage());
      e.printStackTrace();
      fail();
    }
  }

  /**
   * Test demo files using LogLossBoost with weighted examples Sample with
   * different thresholds. Verify that the number of accepted training examples
   * changes
   */
  public final void testSampledLogLossBoostCycle() {
    try {
      System.out.println("Running Sampled LogLoss booster cycle test.");
      int rounds = 30;
      String[] args = { "-CONFIG", System.getProperty("jboost.home") + File.separatorChar + "src/jboost/controller/weightedjboost.config" };
      m_config = new Configuration(null, args);
      m_config.addOption("numRounds", Integer.toString(rounds));
      m_config.addOption("booster_type", "jboost.booster.LogLossBoost");
      m_controller = new Controller(m_config);
      m_controller.startLearning();
      m_controller.outputLearningResults();
      m_config.addOption(Configuration.SERIALIZED_INPUT, "src/jboost/controller/weightedatree.serialized");
      // m_config.addOption(Configuration.SAMPLE_TRAINING_DATA, "true");
      m_config.addOption(Configuration.SAMPLE_THRESHOLD_WEIGHT, "0.005");
      m_controller = new Controller(m_config);
      m_controller.startLearning();
      m_controller.outputLearningResults();
    }
    catch (Exception e) {
      System.out.println(e.getMessage());
      e.printStackTrace();
      fail();
    }
  }

  /**
   * Test demo files using BrownBoost Write out serialized tree, then re-read
   * and continue boosting. Compare output to non-serialized tree
   */
  public final void testBrownBoostCycle() {
    try {
      int rounds = 100;
      m_config.addOption("numRounds", Integer.toString(rounds / 2));
      m_config.addOption("booster_type", "jboost.booster.BrownBoost");
      m_controller = new Controller(m_config);
      m_controller.startLearning();
      m_controller.outputLearningResults();

      m_config.addOption("numRounds", Integer.toString(rounds / 2));
      m_config.addOption("booster_type", "jboost.booster.LogLossBoost");
      m_config.addOption("serialTreeInput", "src/jboost/controller/atree.serialized");
      m_controller = new Controller(m_config);
      m_controller.startLearning();
      InstrumentedAlternatingTree firstTree = m_controller.getTree();

      // reset the controller and learn for 80 rounds
      m_config.addOption("numRounds", Integer.toString(rounds));
      m_config.addOption("booster_type", "jboost.booster.LogLossBoost");
      m_controller = new Controller(m_config);
      m_controller.startLearning();
      InstrumentedAlternatingTree secondTree = m_controller.getTree();
      System.out.println(firstTree.toString());
      System.out.println(secondTree.toString());
      // assertTrue(firstTree.toString().equals(secondTree.toString()));
    }
    catch (Exception e) {
      e.printStackTrace();
      fail();
    }
  }

  /**
   * Test the startup of the tokenizer
   */
  public final void notestStartTokenizer() {

  }

  /**
   * Test the learning process
   */
  public final void notestLearnFromStreams() {
    // TODO Implement learnFromStreams().
  }

  /**
   * Test the results output
   */
  public final void notestOutputLearningResults() {
    // TODO Implement outputLearningResults().
  }

  /**
   * Test the predictor generation
   */
  public final void notestGetPredictor() {
    // TODO Implement getPredictor().
  }

}
