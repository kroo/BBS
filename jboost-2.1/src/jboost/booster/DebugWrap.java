package jboost.booster;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import jboost.controller.Configuration;

/**
 * NOTE: THIS CLASS IS NOT READY TO BE USED WITH YabaBoost or BrownBoost!!! IT
 * CAN BE USED WITH AdaBoost or LogLossBoost. This wrapper is to be used for
 * debugging purposes. Numerous debugging checks are made to ensure correctness: *
 * All elements of a bag have up to date m_weights at the time that the bag is
 * used for computing loss or making predictions. * Items are not refreshed at
 * an inappropriate time. * Bags are disjoint (or subets) when necessary. *
 * Parameters are propoer. * If subtracting an element from a bag, we ensure
 * that the element is in the bag to start.
 * 
 * @author Rob Schapire (rewritten by Aaron Arvey)
 */

class DebugWrap extends AbstractBooster {

  /** The booster */
  private AbstractBooster booster;

  /** number of examples */
  private int num_examples = 0;

  /** True after booster finalized */
  private boolean finalized = false;

  /** Current time (number of rounds) */
  private int time = 0;

  /** Keeps track of when each example was last modified */
  private int modified_time[];

  /** Number of times booster has been cleared */
  private int ctime = 0;

  /**
   * Constructor. Takes in the booster to wrap.
   */
  DebugWrap(AbstractBooster booster) {
    this.booster = booster;
  }

  /**
   * Return all variables in the wrapper and then prints out the booster
   * 
   * @return debugging information
   */
  public String toString() {
    String s = "DebugWrap.  num_examples = " + num_examples + " finalized = " + finalized + " time = " + time + " ctime = " + ctime + "\nmodified times:\n";
    for (int i = 0; i < num_examples; i++)
      s += i + "\t" + modified_time[i] + "\n";
    s += "\nUnderlying booster:\n" + booster;
    return s;
  }

  /**
   * Checks: 1) Proper index order, 2) The booster has not been finalized, 3)
   * Weight is not negative
   */
  public void addExample(int index, jboost.examples.Label label, double weight, double margin) {
    if (index != num_examples) throw new RuntimeException("AbstractBooster received examples " + "out of order");
    if (finalized) throw new RuntimeException("AbstractBooster.addExample called " + "after booster finalized");
    if (weight < 0) throw new RuntimeException("AbstractBooster.addExample called " + "with negative weight");

    booster.addExample(index, label, weight, margin);
    num_examples++;
  }

  public void addExample(int index, jboost.examples.Label label, double weight) {
    addExample(index, label, weight, 0);
  }

  /**
   * Calls addExample(index,label,weight)
   */
  public void addExample(int index, jboost.examples.Label label) {
    addExample(index, label, 1.0);
  }

  /**
   * Creates the modified time array. Finalizes booster.
   */
  public void finalizeData() {
    modified_time = new int[num_examples];
    finalized = true;
    booster.finalizeData();
  }

  /**
   * Clears all variables in wrapper and in booster.
   */
  public void clear() {
    booster.clear();
    num_examples = 0;
    finalized = false;
    time = 0;
    modified_time = null;
    ctime++;
  }

  /**
   * Returns new PBag
   */
  public Bag newBag() {
    return new PBag();
  }

  /**
   * Checks for normalized margin. Calculates weight
   */
  public double calculateWeight(double margin) {

    return booster.calculateWeight(margin);
  }

  public double[][] getWeights() {
    return new double[1][1];
  }

  public double getTotalWeight() {
    return booster.getTotalWeight();
  }

  public double[][] getPotentials() {
    return new double[1][1];
  }

  /**
   * Check that there are as many predictions (for bags) as there are
   * partitions. Check that the partition doesn't have the same example in both
   * partitions. After checks, update booster.
   */
  public void update(Prediction[] preds, int[][] index) {
    time++;
    if (preds.length != index.length) throw new RuntimeException("booster.update received " + "arguments of different lengths");

    HashSet h = new HashSet();
    for (int i = 0; i < index.length; i++)
      for (int j = 0; j < index[i].length; j++) {
        if (h.contains(new Integer(index[i][j]))) {
          String s = "index[" + i + "][" + j + "]=" + index[i][j] + " appeared twice in argument " + "to booster.update\nprinting index:\n";
          for (i = 0; i < index.length; i++) {
            for (j = 0; j < index[i].length; j++)
              s += " " + index[i][j];
            s += "\n";
          }

          throw new RuntimeException(s);
        }
        h.add(new Integer(index[i][j]));
        modified_time[index[i][j]] = time;
      }

    booster.update(preds, index);
  }

  /**
   * Check if array of bags overlap one another
   */
  private static boolean overlappingBags(Bag[] b) {
    HashSet h = new HashSet();

    for (int j = 0; j < b.length; j++)
      for (Iterator i = ((PBag) b[j]).map.keySet().iterator(); i.hasNext();) {
        Integer x = (Integer) i.next();
        if (h.contains(x)) return true;
        h.add(x);
      }
    return false;
  }

  /**
   *
   */
  public Prediction[] getPredictions(Bag[] b, int[][] partition) {
    return getPredictions(b);
  }

  public Prediction[] getPredictions(Bag[] b) {
    if (overlappingBags(b)) throw new RuntimeException("bags should not overlap in " + "booster.getPredictions");

    Bag[] ubag = new Bag[b.length];
    for (int i = 0; i < b.length; i++) {
      ubag[i] = ((PBag) b[i]).bag;
      // check up to date
      if (!((PBag) b[i]).isCurrent()) throw new RuntimeException("bag " + b[i] + " not up to date " + "in booster.getPredictions");
    }
    return booster.getPredictions(ubag);
  }

  public double getLoss(Bag[] b) {
    // check disjoint
    if (overlappingBags(b)) throw new RuntimeException("bags should not overlap in " + "booster.getLoss");

    Bag[] ubag = new Bag[b.length];
    for (int i = 0; i < b.length; i++) {
      ubag[i] = ((PBag) b[i]).bag;
      // check up to date
      if (!((PBag) b[i]).isCurrent()) throw new RuntimeException("bag " + b[i] + " not up to date " + "in booster.getPredictions");
    }
    return booster.getLoss(ubag);
  }

  public double getTheoryBound() {
    return booster.getTheoryBound();
  }

  public double[][] getMargins() {
    return booster.getMargins();
  }

  /**
   * This is the bag class associated with this booster. Each bag maintains all
   * elements added to it, as well as timing information to be sure bag is kept
   * up to date from round to round of boosting.
   */
  private class PBag extends Bag {

    private Bag bag; // underlying bag
    private int ctime_created;
    private Map map; // contains all examples in bag mapped

    // to last refreshed times

    private PBag() {
      bag = booster.newBag();
      ctime_created = ctime;
      map = new TreeMap();
    }

    // check if all examples in bag are up to date
    private boolean isCurrent() {
      if (ctime_created != ctime) return false;
      for (Iterator i = map.keySet().iterator(); i.hasNext();) {
        Integer x = (Integer) i.next();
        if (((Integer) map.get(x)).intValue() < // last refresh time
        modified_time[x.intValue()]) // last modified time
        return false;
      }
      return true;
    }

    public String toString() {
      String s = "PBag.  ctime_created = " + ctime_created + " Map:\n";
      for (Iterator i = map.keySet().iterator(); i.hasNext();) {
        Integer x = (Integer) i.next();
        s += x + " " + ((Integer) map.get(x)) + "\n";
      }
      s += "Underlying bag:\n" + bag;
      return s;
    }

    public void reset() {
      ctime_created = ctime;
      map.clear();
      bag.reset();
    }

    public boolean isWeightless() {
      return bag.isWeightless();
    }

    private void saveEx(int index, int t) {
      if (map.containsKey(new Integer(index))) throw new RuntimeException("example " + index + " added " + "to bag that already contains " + "it:\n" + this);

      map.put(new Integer(index), new Integer(t));
    }

    private void deleteEx(int index) {
      Integer x = new Integer(index);
      if (!map.containsKey(x)) throw new RuntimeException("tried to delete example " + index + " from bag that " + "doesn't contain it:\n" + this);

      map.remove(x);
    }

    public void addExample(int index) {
      saveEx(index, time);
      bag.addExample(index);
    }

    public void subtractExample(int index) {
      deleteEx(index);
      bag.subtractExample(index);
    }

    public void addExampleList(int[] l) {
      for (int i = 0; i < l.length; i++)
        saveEx(l[i], time);
      bag.addExampleList(l);
    }

    public void subtractExampleList(int[] l) {
      for (int i = 0; i < l.length; i++)
        deleteEx(l[i]);
      bag.subtractExampleList(l);
    }

    public void addBag(Bag b) {
      PBag other = (PBag) b;

      for (Iterator i = other.map.keySet().iterator(); i.hasNext();) {
        Integer x = (Integer) i.next();
        saveEx(x.intValue(), ((Integer) other.map.get(x)).intValue());
      }

      bag.addBag(other.bag);
    }

    public void subtractBag(Bag b) {
      PBag other = (PBag) b;

      if (other.ctime_created != ctime_created) throw new RuntimeException("bag " + b + " does not " + "match creation time of bag " + this
                                                                           + " in bag.subtractBag");

      for (Iterator i = other.map.keySet().iterator(); i.hasNext();) {
        Integer x = (Integer) i.next();
        if (map.containsKey(x) && ((Integer) map.get(x)).intValue() != ((Integer) other.map.get(x)).intValue()) throw new RuntimeException(
                                                                                                                                           "creation times of "
                                                                                                                                               + x
                                                                                                                                               + "do not match in bags "
                                                                                                                                               + b
                                                                                                                                               + " and "
                                                                                                                                               + this
                                                                                                                                               + " in bag.subtractBag");
        deleteEx(x.intValue());
      }

      bag.subtractBag(other.bag);
    }

    public void copyBag(Bag b) {
      PBag other = (PBag) b;
      ctime_created = other.ctime_created;
      map = new TreeMap(other.map);
      bag.copyBag(other.bag);
    }

    private void refreshMap(int index) {
      // check last modified times, mark last refresh times
      if (time != modified_time[index]) {
        throw new RuntimeException("tried to refresh example " + index + " in bag too late.  Bag:\n" + this);
      }
      if (!map.containsKey(new Integer(index))) {
        throw new RuntimeException("tried to refresh example " + index + " in bag that doesn't contain it." + "  Bag:\n" + this);
      }

      map.put(new Integer(index), new Integer(time));
    }

    public void refresh(int index) {
      refreshMap(index);
      bag.refresh(index);
    }

    public void refreshList(int[] l) {
      for (int i = 0; i < l.length; i++)
        refreshMap(l[i]);
      bag.refreshList(l);
    }

    /**
     * Computes the loss for this bag. This loss is only meaningful for additive
     * losses.
     */
    public double getLoss() {
      if (!isCurrent()) throw new RuntimeException("bag " + this + " not up to " + "date in bag.getLoss");
      return bag.getLoss();
    }

  }

  /*
   * (non-Javadoc)
   * 
   * @see jboost.booster.Booster#init(jboost.controller.Configuration)
   */
  public void init(Configuration config) {
    // TODO Auto-generated method stub
  }

}
