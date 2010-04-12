package jboost;

import java.util.Vector;

import jboost.booster.Booster;
import jboost.controller.Configuration;
import jboost.examples.Example;
import jboost.learner.SplitterBuilder;

/**
 * A complex learner that generates a classifier which combines many base
 * classifiers. The responsibility of a complex learner is to suggest candidate
 * base classifiers to add to the current rule and to add base classifiers as
 * directed. As a final result, it gives the generated complex classifier. For
 * now, do not assume that this combined classifier and the complex learner are
 * disjoint. In other words, they may share nodes in common, and if the complex
 * learner modifies a node then it will also be modified in the combined
 * classifier.
 * 
 * @version $Header: /cvsroot/jboost/jboost/src/jboost/ComplexLearner.java,v
 *          1.1.1.1 2007/05/16 04:06:02 aarvey Exp $
 * @author Yoav Freund
 */
public abstract class ComplexLearner {

  /** Generate a list of candidates */
  public abstract Vector getCandidates() throws NotSupportedException;

  /** Add a CandidateSplit */
  public abstract void addCandidate(CandidateSplit candidate) throws jboost.atree.InstrumentException;

  /**
   * Gets a (base) Predictor corresponding to the candidate that was just added
   * using addCandidate().
   */
  public abstract Predictor getLastBasePredictor();

  /** Add an example to the internal data structures */
  public abstract void addExample(Example e);

  /** Signify that all examples have been added */
  public abstract void finalizeData();

  /**
   * Reset all the learning infrastructure and internal data structures.
   */

  public abstract void resetData();

  /** Extract the combined p redictor. */
  public abstract WritablePredictor getCombinedPredictor();

  /**
   * Main Factory Method - generates a complex learner with an empty initial
   * preditor
   */
  public static final ComplexLearner generateLearner(Booster booster, SplitterBuilder[] sb, Configuration config) {
    return null;
  } // to be implemented

  /**
   * Instrumentation Factory - generates a comlex learner with a given predictor
   * as its initial predictor
   */
  public static final ComplexLearner instrumentPredictor(Predictor predictor, Booster booster, SplitterBuilder[] sb, Configuration config) {
    return null;
  } // to be implemented

  /** Modify the predictions of previously added base rules */
  public void adjustPredictions() {
  }

  protected boolean isFinalized;
}
