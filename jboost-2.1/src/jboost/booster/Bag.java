package jboost.booster;

import jboost.NotSupportedException;

/**
 * This is the abstract definition of a bag. A bag maintains sufficient
 * statistics for one subset of the training indices. An example may be added or
 * subtracted from a bag. An entire list or bag may also be added or subtracted.
 */

public abstract class Bag {

  /** total weight for examples of each label */
  protected double[] m_w;

  /**
   * Resets the bag to empty (or to its default setting as defined by the
   * booster)
   */
  public abstract void reset();

  /**
   * Get the weights of the bag.
   */
  public double[] getWeights() {
    return m_w;
  }

  /**
   * If the bag has no weight, then it is considered to be weightless.
   */
  public abstract boolean isWeightless();

  /**
   * Adds one example index to the bag.
   */
  public abstract void addExample(int i);

  /**
   * Subtracts one example index from the bag.
   */
  public abstract void subtractExample(int i);

  /**
   * Adds a list of example indices to the bag. A default implementation is
   * provided in terms of addExample.
   */
  public void addExampleList(int[] l) {
    for (int i = 0; i < l.length; i++)
      addExample(l[i]);
  }

  /**
   * Subtracts a list of example indices to the bag. A default implementation is
   * provided in terms of subtractExample.
   */
  public void subtractExampleList(int[] l) {
    for (int i = 0; i < l.length; i++) {
      subtractExample(l[i]);
    }
  }

  /**
   * Adds the given bag to this one. It is assumed that the two bags are
   * disjoint and the same type.
   */
  public abstract void addBag(Bag b);

  /**
   * Subtracts the given bag from this one. It is assumed that the bag being
   * subtracted is a subset of the other one, and that the two bags are the same
   * type.
   */
  public abstract void subtractBag(Bag b);

  /**
   * Copies a given bag of the same type into this one.
   */
  public abstract void copyBag(Bag b);

  /**
   * Updates the weight of a single example contained in this bag. In other
   * words, subtracts its old weight and adds its new weight.
   */
  public abstract void refresh(int i);

  /**
   * Updates the m_weights of a list of examples contained in this bag. In other
   * words, subtracts their old m_weights and adds their new m_weights.
   */
  public void refreshList(int[] l) {
    for (int i = 0; i < l.length; i++) {
      refresh(l[i]);
    }
  }

  /**
   * Computes the loss for this bag. This loss is only meaningful for additive
   * losses.
   */
  public abstract double getLoss();

  /**
   * When data splitting is used, this is the same as getLoss() but computed
   * using only examples in part s of the split. s<0 indicates that the entire
   * dataset should be used. The default implementation of this method simply
   * throws an exception.
   */
  public double getLoss(int s) throws NotSupportedException {
    throw new NotSupportedException("getLoss", this.getClass().toString());
  }

  /*
   * Computes a non-default loss for this bag. The loss types are defined as
   * public constants in jboost.booster.AbstractBooster. Not all bags implement
   * all loss types. To check if a loss type is allowed, use isAllowedLoss().
   */
  /* public abstract double getSpecialLoss(int lossType); */

  /*
   * When data splitting is used, this is the same as getSpecialLoss(lossType)
   * but computed using only examples in part s of the split. s<0 indicates
   * that the entire dataset should be used. The default implementation of this
   * method simply throws an exception. public double getSpecialLoss(int s, int
   * lossType) throws NotSupportedException { throw new
   * NotSupportedException("getSpecialLoss",this.getClass().toString()); }
   */

  /**
   * Checks if a given loss type is allowed for this bag. public abstract
   * boolean isAllowedLoss(int lossType);
   */

}
