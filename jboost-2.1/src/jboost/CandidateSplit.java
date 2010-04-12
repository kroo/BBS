package jboost;

import jboost.booster.Bag;
import jboost.learner.Splitter;
import jboost.learner.SplitterBuilder;

/**
 * A class that holds a candidate base-rule. This includes the Splitter, the
 * associated bags, the loss, and the ability to calculate the split on the
 * training data induced by this rule.
 * 
 * @version $Header: /cvsroot/jboost/jboost/src/jboost/CandidateSplit.java,v
 *          1.1.1.1 2007/05/16 04:06:02 aarvey Exp $
 * @author Yoav Freund
 */
public class CandidateSplit {

  public CandidateSplit(SplitterBuilder b, Splitter s, Bag[] bags, double loss) {
    builder = b;
    splitter = s;
    partition = bags;
    this.loss = loss;
  }

  public CandidateSplit() {
    builder = null;
    splitter = null;
    partition = null;
  }

  /** The builder that generated this candidate */
  protected SplitterBuilder builder;

  public SplitterBuilder getBuilder() {
    return (builder);
  }

  /** Called to determine the partition of the data induced by this split. */
  public int[][] getDataSplit() {
    return (builder.split(splitter));
  }

  protected Splitter splitter;

  /** Get the {@link Splitter} that defines the data splitting rule. */
  public Splitter getSplitter() {
    return (splitter);
  }

  /**
   * the array of bags corresponding to the partition resulting from application
   * of the splitter to the training set.
   */
  protected Bag[] partition;

  public Bag[] getPartition() {
    return (partition);
  }

  protected double loss;

  /** Obtain the loss of this split on the training data. */
  public double getLoss() {
    return loss;
  }

  /** Convert this candidate split to a string. */
  public String toString() {
    String retval = new String();
    if (splitter != null) retval += splitter.toString();
    retval += " Loss: " + loss + "\n";
    return (retval);
  }
}
