package jboost.booster;

import java.io.Serializable;

import jboost.NotSupportedException;
import jboost.controller.Configuration;

/**
 * This is the abstract definition of a booster. The booster is responsible for
 * maintaining, for a set of examples, the m_margins and m_weights of these
 * examples. Each subclass implements a specific boosting algorithm, e.g.,
 * adaboost, brownboost, etc. The booster also computes the loss and predictions
 * for a partition of the data. AbstractBooster also operates as a factory,
 * where it generate the appropriate Booster with the appropriate configuration.
 * 
 * @author Rob Schapire (rewritten by Aaron Arvey)
 * @version $Header:
 *          /cvsroot/jboost/jboost/src/jboost/booster/AbstractBooster.java,v 1.9
 *          2008/04/10 07:54:17 dhsu Exp $
 */

public abstract class AbstractBooster implements Booster, Serializable {

  protected static final String PREFIX = "booster_";

  /**
   * Factory method to build a booster instance according to given
   * configuration. Uses reflection to do this.
   * 
   * @param c
   *            set of options for the booster
   * @param num_labels
   *            the number of m_labels in the data
   * @param isMultiLabel
   *            true if multilabled data
   * @return Booster
   */
  public static Booster getInstance(Configuration c, int num_labels, boolean isMultiLabel) throws ClassNotFoundException, InstantiationException,
                                                                                          IllegalAccessException, Exception {

    AbstractBooster result = null;

    // Get the booster type from configuration and
    // create a class of that type.
    String boosterType = c.getString(PREFIX + "type", "jboost.booster.AdaBoost");
    System.out.println("Booster type: " + boosterType);
    Class boosterClass = Class.forName(boosterType);
    result = (AbstractBooster) boosterClass.newInstance();
    result.init(c);

    if (isMultiLabel) {
      throw new NotSupportedException("JBoost does not support multi-lable and multi-class");
    }

    // If we are debugging, then wrap in paranoia
    boolean paranoid = c.getBool(PREFIX + "paranoid", false);
    if (paranoid) {
      result = new DebugWrap(result);
    }
    return result;
  }

  public int getNumExamples() {
    return 0;
  }

  public String getParamString() {
    return "No parameters defined";
  }

  /**
   * Create and return a new Bag which initially contains the elements in the
   * list.
   * 
   * @param list
   *            initial items to add to the Bag
   */
  public Bag newBag(int[] list) {
    Bag bag = newBag();
    bag.addExampleList(list);
    return bag;
  }

  /**
   * Clone a bag
   * 
   * @param orig
   *            the bag to clone
   * @return new bag
   */
  public Bag newBag(Bag orig) {
    Bag newbag = newBag();
    newbag.copyBag(orig);
    return newbag;
  }

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
  public int findBestSplit(Bag b0, Bag b1, int[] l, boolean[] sp) {
    Bag[] bags = new Bag[2];

    bags[0] = newBag(); // init an empty bag
    bags[1] = newBag(l); // init a full bag

    b0.reset();
    b1.copyBag(bags[1]);

    if (l.length == 0) return 0;

    double bestLoss = getLoss(bags);
    int bestIndex = 0;
    double loss;

    for (int i = 0; i < l.length - 1; i++) {
      bags[1].subtractExample(l[i]);
      bags[0].addExample(l[i]);
      if (sp[i + 1]) { // if this is a potential split point
        if ((loss = getLoss(bags)) < bestLoss) {
          bestLoss = loss;
          bestIndex = i + 1;
          b0.copyBag(bags[0]);
          b1.copyBag(bags[1]);
        }
      }
    }
    return bestIndex;
  }

  /**
   * Compute the loss associated with an array of bags where small loss is
   * considered "better". We assume that loss is additive for a set of bags.
   * 
   * @param bags
   *            array of bags whose losses will be added up and returned
   * @return loss the sum of the losses for all the bags
   */
  public double getLoss(Bag[] bags) {
    double loss = 0;
    for (int i = 0; i < bags.length; i++) {
      loss += bags[i].getLoss();
    }
    return loss;
  }

}
