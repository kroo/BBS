/*
 * Created on Jan 12, 2004
 *
 */
package jboost.booster;

import jboost.controller.Configuration;
import jboost.examples.Label;

/**
 * This is the definition of the required interface for a booster. A skeletal
 * implementation is provided by AbstractBooster.
 * 
 * @author cschavis
 */
public interface Booster {

  /**
   * Factory method to create an instance of a Booster
   * 
   * @param configuration
   */
  public abstract void init(Configuration config);

  /**
   * Create and return a new Bag which initially contains the elements in the
   * list 'defaults'.
   * 
   * @param defaults
   *            the initial items to add to the Bag
   */
  public abstract Bag newBag(int[] defaults);

  /**
   * Create and return a new Bag which is a clone of 'original'
   * 
   * @param original
   *            the bag to copy into a new Bag
   */
  public abstract Bag newBag(Bag original);

  /**
   * Find the best binary split for a sorted list of example indices with given
   * split points.
   * 
   * @param l
   *            an array of example indices, sorted.
   * @param sp
   *            an array with true in position i when a split between positions
   *            i-1 and i should be checked
   * @param b0 -
   *            a bag with all points below the best split (upon return)
   * @param b1 -
   *            a bag with all points at or above the best split (upon return)
   * @return the index in l where the best split occurred (possibly 0 if the
   *         best split puts all points on one side)
   */
  public abstract int findBestSplit(Bag b0, Bag b1, int[] l, boolean[] sp);

  /**
   * Compute the loss associated with an array of bags where small loss is
   * considered "better". The default implementation of this procedure assumes
   * that the loss is additive across bags.
   * 
   * @param bags
   *            array of bags whose losses will be added up and returned
   * @return loss the sum of the losses for all the bags
   */
  public abstract double getLoss(Bag[] bags);

  /**
   * Returns a list of margin values for the training data. The actual number of
   * values returned may not be equal to the number of training examples; for
   * instance, in multiclass problems, there may be k values returned for each
   * training example where k is the number of classes. This list is meant for
   * statistical purposes only (e.g., plotting all margin values).
   */
  public abstract double[][] getMargins();

  public abstract double[][] getWeights();

  public abstract double[][] getPotentials();

  public abstract double getTotalWeight();

  public int getNumExamples();

  public abstract String getParamString();

  /**
   * Returns the current theoretical bound on the training error.
   */
  public abstract double getTheoryBound();

  /**
   * Returns the predictions associated with a list of bags representing a
   * partition of the data.
   */
  public abstract Prediction[] getPredictions(Bag[] b);

  /**
   * Returns the predictions associated with a list of bags representing a
   * partition of the data.
   */
  // XXX DJH: What is 'partition'? Similar to 'elements' below?
  // XXX YF: I think you are probably correct in your interpretation, need to
  // check the inplementation, verify, and update the
  // XXX Description of this method and change "examples" to "elements" in the
  // javadoc for "update".
  public abstract Prediction[] getPredictions(Bag[] b, int[][] partition);

  /**
   * Updates training set data structures to reflect the addition of a new base
   * classifier.
   * 
   * @param predictions
   *            an array of predictions
   * @param examples
   *            an array of arrays of example indexes for which the prediction
   *            is not zero. The first index in this two dimensional array
   *            defines the prediction and corresponds to an entry in <i>
   *            predictions</i>. the second index runs over the examples
   *            indices that correspond to that part.
   */
  public abstract void update(Prediction[] predictions, int[][] elements);

  /**
   * Use the margin value to calculate the weight for the example
   * 
   * @param margin
   * @return weight
   */
  public double calculateWeight(double margin);

  /**
   * Create and return a new Bag which contains no elements
   */
  public abstract Bag newBag();

  /**
   * Clear the examples list
   */
  public abstract void clear();

  /**
   * Finalize the data structures of the booster. Execute after the constructor
   * and a series of calls to addExample()
   */
  public abstract void finalizeData();

  /**
   * Add an example to location index in the list of examples handled by the
   * booster.
   * 
   * @param index
   *            the position where the example should be added
   * @param label
   *            the label to add
   */
  public abstract void addExample(int index, Label label);

  public abstract void addExample(int index, Label label, double weight);

  public abstract void addExample(int index, Label label, double weight, double margin);

}
