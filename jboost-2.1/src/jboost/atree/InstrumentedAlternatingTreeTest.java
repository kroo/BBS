/*
 * Created on Jan 17, 2004
 *
 */
package jboost.atree;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Vector;

import jboost.CandidateSplit;
import jboost.Predictor;
import jboost.booster.AdaBoost;
import jboost.booster.Booster;
import jboost.controller.Configuration;
import jboost.examples.Attribute;
import jboost.examples.AttributeDescription;
import jboost.examples.DiscreteAttribute;
import jboost.examples.Example;
import jboost.examples.ExampleDescription;
import jboost.examples.ExampleSet;
import jboost.examples.Instance;
import jboost.examples.Label;
import jboost.learner.EqualitySplitterBuilder;
import jboost.learner.IncompAttException;
import jboost.learner.SplitterBuilder;
import jboost.tokenizer.DataStream;
import jboost.tokenizer.jboost_DataStream;
import junit.framework.TestCase;

/**
 * @author cschavis
 */
public class InstrumentedAlternatingTreeTest extends TestCase {

  DataStream m_datastream;
  Booster m_booster;
  Booster m_booster2;
  int[] m_trainLabels;
  int[] m_trainFeature1;
  int[] m_trainFeature2;
  int[] m_testLabels;
  int[] m_testValues;
  int[] m_exampleIndices = new int[12];
  ExampleSet m_examples;
  SplitterBuilder m_builder;
  Vector m_builders;

  /*
   * @see TestCase#setUp()
   */
  protected void setUp() throws Exception {
    super.setUp();
    // build examples

    m_datastream = new jboost_DataStream(false, "feature1 (zero,one,two)\n labels (one,two)\n");
    ExampleDescription description = m_datastream.getExampleDescription();
    m_examples = new ExampleSet(description);
    m_booster = new AdaBoost();
    m_booster2 = new AdaBoost();
    m_builder = new EqualitySplitterBuilder(0, m_booster, new AttributeDescription[] { description.getAttributeDescription(0) });

    m_trainLabels = new int[] { 0, 0, 1, 0, 0, 1, 0, 0, 1, 1, 1, 0 };
    m_trainFeature1 = new int[] { 0, 2, 2, 2, 1, 2, 0, 1, 0, 0, 2, 1 };
    m_testLabels = new int[] { 0, 0, 1, 0, 0, 1, 0, 0, 1, 1, 1, 0 };
    m_testValues = new int[] { 0, 2, 2, 2, 1, 2, 0, 1, 0, 0, 2, 1 };

    Example x;
    Attribute[] attributes = new Attribute[1];
    Label l;
    m_exampleIndices = new int[m_trainLabels.length];

    for (int i = 0; i < m_trainLabels.length; i++) {
      l = new Label(m_trainLabels[i]);
      attributes[0] = new DiscreteAttribute(m_trainFeature1[i]);
      x = new Example(attributes, l);

      try {
        m_builder.addExample(i, x);
        m_booster.addExample(i, l);
        m_booster2.addExample(i, l);
        m_examples.addExample(i, x);
        m_exampleIndices[i] = i;
      }
      catch (IncompAttException e) {
      }
    }

    m_builder.finalizeData();
    m_booster.finalizeData();
    m_booster2.finalizeData();
    m_examples.finalizeData();
    m_builders = new Vector();
    m_builders.add(m_builder);

  }

  /*
   * @see TestCase#tearDown()
   */
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  public final void testGetCandidates() {
    // TODO Implement getCandidates().
    InstrumentedAlternatingTree iat;
    try {
      iat = new InstrumentedAlternatingTree(m_builders, m_booster, m_exampleIndices, new Configuration());
      int rounds = 2;
      for (int j = 0; j < rounds; j++) {
        Vector cand = iat.getCandidates();

        CandidateSplit bC = null;
        for (int i = 0; i < cand.size(); i++) {
          bC = (CandidateSplit) cand.get(i);
          iat.addCandidate(bC);
        }
      }

      Predictor c = iat.getCombinedPredictor();

    }
    catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

  }

  public final void testInstrumentAlternatingTree() {

    // create an instrumented tree
    InstrumentedAlternatingTree first = null;
    try {
      first = new InstrumentedAlternatingTree(m_builders, m_booster, m_exampleIndices, new Configuration());
      int rounds = 100;
      for (int j = 0; j < rounds; j++) {
        Vector cand = first.getCandidates();
        CandidateSplit bC = null;
        // This piece should be replaced by a more general tool to measure the
        // goodness of a
        // split.
        int best = 0;
        double bestLoss = ((CandidateSplit) cand.get(0)).getLoss();
        double tmpLoss;
        for (int i = 1; i < cand.size(); i++) {
          if ((tmpLoss = ((CandidateSplit) cand.get(i)).getLoss()) < bestLoss) {
            bestLoss = tmpLoss;
            best = i;
          }
        }
        bC = (CandidateSplit) cand.get(best);
        first.addCandidate(bC);
      }

      // turn the instrumented tree into an alternating tree
      AlternatingTree tree = (AlternatingTree) first.getCombinedPredictor();
      // serialize the tree
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      ObjectOutputStream os;

      os = new ObjectOutputStream(bos);
      os.writeObject(tree);
      os.flush();
      os.close();

      // de-serialize
      ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
      ObjectInputStream is;
      AlternatingTree newTree = null;

      is = new ObjectInputStream(bis);
      newTree = (AlternatingTree) is.readObject();
      is.close();

      InstrumentedAlternatingTree second = new InstrumentedAlternatingTree(newTree, m_builders, m_booster2, m_exampleIndices, new Configuration());
      AlternatingTree secondTree = (AlternatingTree) second.getCombinedPredictor();

      for (int i = 0; i < m_trainLabels.length; i++) {
        Instance test = m_examples.getExample(i).getInstance();
        assertTrue(tree.predict(test).equals(secondTree.predict(test)));
      }

      // assert that the boosters are equivalent
      assertTrue(m_booster.toString().equals(m_booster2.toString()));
    }
    catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      fail();
    }

  }

  public final void testAddCandidate() {
    // TODO Implement addCandidate().
  }

  public final void testGetLastBasePredictor() {
    // TODO Implement getLastBasePredictor().
  }

  public final void testAddExample() {
    // TODO Implement addExample().
  }

  public final void testFinalizeData() {
    // TODO Implement finalizeData().
  }

  public final void testResetData() {
    // TODO Implement resetData().
  }

  public final void testGetCombinedPredictor() {
    // TODO Implement getCombinedPredictor().
  }

  public final void testAdjustPredictions() {
    // TODO Implement adjustPredictions().
  }

  public final void testAddPredictorNodeToList() {
    // TODO Implement addPredictorNodeToList().
  }

  public final void testAddSplitterNode() {
    // TODO Implement addSplitterNode().
  }

  public final void testGetBooster() {
    // TODO Implement getBooster().
  }

  public final void testMakeExampleMask() {
    // TODO Implement makeExampleMask().
  }

  public final void testGenerateLearner() {
    // TODO Implement generateLearner().
  }

  public final void testInstrumentPredictor() {
    // TODO Implement instrumentPredictor().
  }

}
