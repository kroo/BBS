/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */
package jboost.booster;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import jboost.controller.Configuration;
import jboost.examples.Label;

/**
 * The simplest possible implementation of a booster. confidence-rated adaboost
 * based on equality/inequality of m_labels
 * 
 * @author Yoav Freund
 * @version $Header:
 *          /proj/gene/cvs-repository/jboost/src/jboost/booster/AdaBoost.java,v
 *          1.2 2003/10/01 18:36:15 freund Exp $
 */
public class AdaBoost extends AbstractBooster {

  /** permanent storage for m_labels */
  protected short[] m_labels;
  /** permanent storage for m_margins */
  protected double[] m_margins;
  /** permanent storage for old m_margins */
  protected double[] m_oldMargins; // XXX DJH: this is never used in AdaBoost
  // (e.g. not cleared in clear())
  /** permanent storage for example m_weights */
  protected double[] m_weights;
  /** permanent storage for example's old m_weights */
  protected double[] m_oldWeights;
  /** Records the potentials. Similar to m_margins and m_weights. */
  protected double[] m_potentials;

  /**
   * The predictions from a hypothesis for an iteration. HACK: This should
   * eventually be removed for efficiency concerns.
   */
  protected double[] m_hypPredictions;

  /** */
  protected int[] m_posExamples;
  /** */
  protected int[] m_negExamples;
  /** */
  protected int m_numPosExamples;
  /** */
  protected int m_numNegExamples;

  /** Constant for positive label for cost sensitive stuff */
  protected final short POSITIVE_LABEL = 0;
  /** Constant for negative label for cost sensitive stuff */
  protected final short NEGATIVE_LABEL = 1;
  /** Constant for non cost sensitive stuff */
  protected final short NO_LABEL = -99;

  /** sampling weights for the examples */
  protected double[] m_sampleWeights;

  /** if false, then assume all sample weights are 1 */
  // protected boolean m_useSampleWeights;
  protected double m_totalWeight; // total weight of all examples
  protected int m_numExamples = 0; // number of examples in training set

  protected double m_smooth;
  protected double m_epsilon; // the hedge term for the
  // calculation of the prediction

  /** temporary location for storing the examples as they are read in */
  protected List<TmpData> m_tmpList;

  /**
   * default constructor
   */
  public AdaBoost() {
    this(0.0);
  }

  /**
   * Constructor which takes a smoothing factor
   * 
   * @param smooth
   *            "smoothing" factor
   */
  AdaBoost(double smooth) {
    m_tmpList = new ArrayList<TmpData>();
    m_numExamples = 0;
    m_smooth = smooth;
    init(new Configuration());
  }

  /**
   * @see jboost.booster.Booster#init(jboost.controller.Configuration)
   */
  public void init(Configuration config) {
    m_smooth = config.getDouble(PREFIX + "smooth", 0.5);
  }

  /**
   * Add an example to the data set of this booster
   * 
   * @param index
   * @param label
   * @param weight
   */
  public void addExample(int index, Label label, double weight) {
    addExample(index, label, weight, 0);
  }

  /**
   * Add an example to the data set of this booster
   * 
   * @param index
   * @param label
   * @param weight
   * @param margin
   */
  public void addExample(int index, Label label, double weight, double margin) {
    int l = label.getSingleValue();
    String failed = null;
    if (l == 1 || l == 0) {
      if (index == m_numExamples) {
        m_numExamples++;
        m_tmpList.add(new TmpData(index, (short) l, weight, margin));
        if (l == POSITIVE_LABEL) m_numPosExamples++;
      }
      else {
        // XXX DJH: determine class name at runtime
        failed = getClass().getName() + ".addExample received index " + index + ", when it expected index " + m_numExamples;
      }
    }
    else {
      // XXX DJH: determine class name at runtime
      failed = getClass().getName() + ".addExample expected a label which is either 0 or 1. It received " + l;
    }

    if (failed != null) {
      throw new IllegalArgumentException(failed);
    }
  }

  /**
   * Add an example to the dataset Default the weight for this example to 1 If
   * this method is used, then this booster will assume that all the sample
   * weights are 1
   * 
   * @param index
   * @param label
   */
  public void addExample(int index, Label label) {
    addExample(index, label, 1);
  }

  /** reset the booster */
  public void clear() {
    m_labels = null;
    m_margins = null;
    m_potentials = null;
    m_weights = null;
    m_oldWeights = null;
    m_sampleWeights = null;
    m_tmpList.clear();
    m_numExamples = 0;
  }

  protected void finalizeData(double defaultWeight) {
    m_margins = new double[m_numExamples];
    m_oldMargins = new double[m_numExamples];
    m_weights = new double[m_numExamples];
    m_oldWeights = new double[m_numExamples];
    m_potentials = new double[m_numExamples];
    m_labels = new short[m_numExamples];
    m_sampleWeights = new double[m_numExamples];
    m_epsilon = m_smooth / m_numExamples;
    m_posExamples = new int[m_numPosExamples];
    m_numNegExamples = m_numExamples - m_numPosExamples;
    m_negExamples = new int[m_numNegExamples];

    int m_posIndex = 0, m_negIndex = 0;
    for (int i = 0; i < m_tmpList.size(); i++) {
      TmpData a = (TmpData) m_tmpList.get(i);
      int index = a.getIndex();
      m_margins[index] = a.getMargin();
      m_weights[index] = m_oldWeights[index] = calculateWeight(m_margins[index]);
      m_labels[index] = a.getLabel();
      if (a.getLabel() == POSITIVE_LABEL) m_posExamples[m_posIndex++] = index;
      else if (a.getLabel() == NEGATIVE_LABEL) m_negExamples[m_negIndex++] = index;
      else {
        // XXX DJH: determine class name at runtime
        System.err.println("Label of example is unknown to " + this.getClass().getName());
        System.exit(2);
      }

      m_sampleWeights[index] = a.getWeight();
      m_totalWeight += defaultWeight * a.getWeight();

    }
    m_tmpList.clear(); // free the memory
  }

  public void finalizeData() {
    finalizeData(1.0);
  }

  /**
   * Return the theoretical bound on the training error.
   */
  public double getTheoryBound() {
    return m_totalWeight / m_numExamples;
  }

  /**
   * Returns the margin values of the training examples.
   */
  public double[][] getMargins() {
    double[][] r = new double[m_numExamples][1];
    for (int i = 0; i < m_numExamples; i++)
      r[i][0] = m_margins[i];
    return r;
  }

  /**
   *
   */
  public double[][] getWeights() {
    double[][] r = new double[m_numExamples][1];
    for (int i = 0; i < m_numExamples; i++)
      r[i][0] = m_weights[i];
    return r;
  }

  /**
   *
   */
  public double[][] getPotentials() {
    double[][] r = new double[m_numExamples][1];
    for (int i = 0; i < m_numExamples; i++)
      r[i][0] = m_potentials[i];
    return r;
  }

  /**
   *
   */
  public int getNumExamples() {
    return m_numExamples;
  }

  /**
   *
   */
  public double getTotalWeight() {
    return m_totalWeight;
  }

  /**
   * Returns a string with all the weights, margins, etc
   */
  public String getExampleData() {
    StringBuffer ret = new StringBuffer("");
    ret.append(getParamString());
    for (int i = 0; i < m_margins.length; i++) {
      ret.append(String.format("[%d];[%.4f];[%.4f];[%.4f];\n", m_labels[i], m_margins[i], m_weights[i], m_potentials[i]));
    }
    return ret.toString();
  }

  public String getParamString() {
    // XXX DJH: determine class name at runtime
    String ret = String.format("None (" + getClass().getName() + ")");
    return ret;
  }

  /** output AdaBoost contents as a human-readable string */
  public String toString() {
    // XXX DJH: determine class name at runtime
    String s = getClass().getName() + ". No of examples = " + m_numExamples + ", m_epsilon = " + m_epsilon;
    s += "\nindex\tmargin\tweight\told weight\tlabel\n";
    NumberFormat f = new DecimalFormat("0.00");
    for (int i = 0; i < m_numExamples; i++) {
      s +=
          "  " + i + " \t " + f.format(m_margins[i]) + " \t " + f.format(m_weights[i]) + " \t " + f.format(m_oldWeights[i]) + " \t"
              + f.format(m_sampleWeights[i]) + "\t\t" + m_labels[i] + "\n";
    }
    return s;
  }

  public Bag newBag(int[] list) {
    return new BinaryBag(list);
  }

  public Bag newBag() {
    return new BinaryBag();
  }

  public Bag newBag(Bag bag) {
    return new BinaryBag((BinaryBag) bag);
  }

  /**
   * Returns the prediction associated with a bag representing a subset of the
   * data.
   */
  protected Prediction getPrediction(Bag b) {
    return ((BinaryBag) b).calcPrediction();
  }

  /*
   * Returns the predictions associated with a list of bags representing a
   * partition of the data.
   */
  public Prediction[] getPredictions(Bag[] b) {
    Prediction[] p = new BinaryPrediction[b.length];
    for (int i = 0; i < b.length; i++) {
      p[i] = ((BinaryBag) b[i]).calcPrediction();
    }
    return p;
  }

  /**
   * @param z -
   *            any double
   * @return If z is negative return -1 else return 1
   */
  public static double sign(double z) {
    if (Double.compare(z, 0.0) == 0) {
      return 1.0;
    }
    else if (Double.compare(z, -0.0) == 0) {
      return -1.0;
    }

    if (z > 0) {
      return 1.0;
    }
    else {
      return -1.0;
    }
  }

  /**
   * Get the "step" of the hypothesis on an example. The step is defined as $y_j *
   * h_i(x_j)$ for example j and hypothesis i.
   * 
   * @param simple_label -
   *            The label of the example as given by m_labels
   * @param hyp_pred -
   *            The hypothesized value of the example
   * @return +1 if label matches hyp, -1 if label doesn't match hyp, 0 if no hyp
   */
  // XXX DJH: changed from 'public' to 'protected'
  protected double getStep(short simple_label, double hyp_pred) {
    double step = getLabel(simple_label) * hyp_pred;
    double EPS = 0.000001;
    if (Math.abs(step) < EPS) return 0.0;
    return sign(step);
  }

  // XXX DJH: changed from 'public' to 'protected'
  protected double getLabel(short simple_label) {
    return sign(-simple_label + 0.5);
  }

  protected double getHypErr(Bag[] bags, int[][] exampleIndex) {
    double hyp_err = 0.0;
    double gamma = 0.0;
    double total_weight = 0.0;

    // Keep track of which examples had hypotheses associated with them.
    boolean[] examplesWithHyp = new boolean[m_margins.length];
    m_hypPredictions = new double[m_margins.length];
    for (int i = 0; i < exampleIndex.length; i++) {
      int[] index = exampleIndex[i];
      BinaryBag b = (BinaryBag) bags[i];
      for (int j = 0; j < index.length; j++) {
        int example = index[j];
        m_hypPredictions[example] = b.calcPrediction().getClassScores()[0];
      }
    }

    int numExamplesWithHyps = 0;
    // Get all examples that have a hypothesis associated with them
    for (int i = 0; i < exampleIndex.length; i++) {
      int[] indexes = exampleIndex[i];
      for (int j = 0; j < indexes.length; j++) {
        int example = indexes[j];
        examplesWithHyp[example] = true;
        numExamplesWithHyps += 1;

        double step = getStep(m_labels[example], m_hypPredictions[example]);
        gamma += m_weights[example] * step;
        if (step < 0) // We got it wrong
        hyp_err += 1;
      }
    }

    // Get all examples that have no hypothesis associated with them.
    for (int i = 0; i < m_margins.length; i++) {
      total_weight += m_weights[i];
      if (!examplesWithHyp[i]) {
        m_hypPredictions[i] = 0;
        // System.out.println("m_hypPredictions[" + i + "," + example + "]: " +
        // 0 + " (No hyp for example " + example + ")");
      }
    }

    hyp_err /= numExamplesWithHyps;
    gamma /= (double) total_weight;

    if (numExamplesWithHyps > 0) {
      System.out.println("Num Examples with predictions: " + numExamplesWithHyps + "/" + m_margins.length);
      System.out.println("Gamma: " + gamma);
    }
    return gamma;
  }

  /**
   * Returns the predictions associated with a list of bags representing a
   * partition of the data. AdaBoost does not use the partition in exampleIndex.
   */
  public Prediction[] getPredictions(Bag[] bags, int[][] exampleIndex) {
    // Code to see how often the splitting partition predicts the same way on
    // both sides
    /*
     * if (bags.length > 1) { BinaryBag [] bbags = new BinaryBag[2]; bbags[0] =
     * (BinaryBag)bags[0]; bbags[1] = (BinaryBag)bags[1]; for (int i= 0; i <
     * bags.length; i++) { if ( (bbags[0].getWeights()[0] >
     * bbags[0].getWeights()[1] && bbags[1].getWeights()[0] >
     * bbags[1].getWeights()[1]) || (bbags[0].getWeights()[0] <
     * bbags[0].getWeights()[1] && bbags[1].getWeights()[0] <
     * bbags[1].getWeights()[1]) ) System.out.print("Bag i: " + bags[i]); } }
     */
    return getPredictions(bags);
  }

  /**
   * AdaBoost uses e^(-margin) as the weight calculation
   */
  public double calculateWeight(double margin) {
    return Math.exp(-1 * margin);
  }

  /**
   * Update the examples m_margins and m_weights using the exponential update
   * 
   * @param predictions
   *            values for examples
   * @param exampleIndex
   *            the list of examples to update
   */
  public void update(Prediction[] predictions, int[][] exampleIndex) {
    // save old m_weights
    for (int i = 0; i < m_weights.length; i++)
      m_oldWeights[i] = m_weights[i];

    // update m_weights and m_margins
    for (int i = 0; i < exampleIndex.length; i++) {
      double p = predictions[i].getClassScores()[1];
      double[] value = new double[] { -p, p };
      int[] indexes = exampleIndex[i];
      for (int j = 0; j < indexes.length; j++) {
        int example = indexes[j];
        m_margins[example] += value[m_labels[example]];
        m_totalWeight -= m_weights[example] * m_sampleWeights[example];
        m_weights[example] = calculateWeight(m_margins[example]);
        m_totalWeight += m_weights[example] * m_sampleWeights[example];
      }
    }
  }

  /**
   * Defines the state of an example Inner class used to store a list of
   * Examples The list is converted into the internal data structures for the
   * Booster by finalizeData();
   */
  protected static class TmpData {

    int m_index;
    short m_label;
    double m_weight;
    double m_margin;

    /**
     * Ctor for a TmpData object
     * 
     * @param index
     * @param label
     * @param weight
     */
    TmpData(int index, short label, double weight, double margin) {
      m_index = index;
      m_label = label;
      m_weight = weight;
      m_margin = margin;
    }

    /**
     * Get the index for this example
     * 
     * @return m_index
     */
    protected int getIndex() {
      return m_index;
    }

    /**
     * Get the label for this example
     * 
     * @return m_label
     */
    protected short getLabel() {
      return m_label;
    }

    /**
     * Get the weigh for this example
     * 
     * @return m_weight
     */
    protected double getWeight() {
      return m_weight;
    }

    /**
     * Get the margin for this example
     * 
     * @return m_weight
     */
    protected double getMargin() {
      return m_margin;
    }

  }

  /**
   * This is the definition of a bag for AdaBoost. The two m_labels are
   * internally referred to as 0 or 1. The bag maintains the total weight of
   * examples labeled 0 and the total weight of examples labeled 1. This bag
   * uses the weights and labels stored in the booster.
   * 
   * @author Yoav Freund
   */

  class BinaryBag extends Bag {

    /** default constructor */
    protected BinaryBag() {
      m_w = new double[2];
      reset();
    }

    /** constructor that copies an existing bag */
    protected BinaryBag(BinaryBag bag) {
      m_w = new double[2];
      m_w[0] = bag.m_w[0];
      m_w[1] = bag.m_w[1];
    }

    /** a constructor that initializes a bag the given list of axamples */
    protected BinaryBag(int[] list) {
      m_w = new double[2];
      reset();
      this.addExampleList(list);
    }

    public String toString() {
      String s = "BinaryBag.\t w0=" + m_w[0] + "\t w1=" + m_w[1] + "\n";
      return s;
    }

    /**
     * Resets the bag to empty
     */
    public void reset() {
      m_w[0] = 0.0;
      m_w[1] = 0.0;
    }

    /**
     * Checks if the bag has any weight.
     */
    public boolean isWeightless() {
      double EPS = 0.0000001;
      if (m_w[0] < EPS && m_w[1] < EPS) {
        return true;
      }
      return false;
    }

    /**
     * Adds one example index to the bag. Update the weights in this bag using
     * the weights from the booster The example index is used to find the label
     * and weight for this example
     * 
     * @param index
     *            the example that is being added to this bag. The index refers
     *            to the booster's internal data structures
     */
    public void addExample(int index) {
      m_w[m_labels[index]] += m_weights[index] * m_sampleWeights[index];
    }

    /**
     * Subtracts one example index from the bag.
     */
    public void subtractExample(int i) {
      if ((m_w[m_labels[i]] -= m_weights[i] * m_sampleWeights[i]) < 0.0) m_w[m_labels[i]] = 0.0;
    }

    /**
     * Adds the given bag to this one. It is assumed that the two bags are
     * disjoint and the same type.
     */
    public void addBag(Bag b) {
      m_w[0] += ((BinaryBag) b).m_w[0];
      m_w[1] += ((BinaryBag) b).m_w[1];
    }

    /**
     * Subtracts the given bag from this one. It is assumed that the bag being
     * subtracted is a subset of the other one, and that the two bags are the
     * same type.
     */
    public void subtractBag(Bag b) {
      if ((m_w[0] -= ((BinaryBag) b).m_w[0]) < 0.0) m_w[0] = 0.0;
      if ((m_w[1] -= ((BinaryBag) b).m_w[1]) < 0.0) m_w[1] = 0.0;
    }

    /**
     * Copies a given bag of the same type into this one.
     */
    public void copyBag(Bag b) {
      m_w[0] = ((BinaryBag) b).m_w[0];
      m_w[1] = ((BinaryBag) b).m_w[1];
    }

    /**
     * Updates the weight of a single example contained in this bag. In other
     * words, subtracts its old weight and adds its new weight.
     */
    public void refresh(int i) {
      short label = m_labels[i];
      if ((m_w[label] += m_weights[i] - m_oldWeights[i]) < 0.0) m_w[label] = 0.0;
    }

    /**
     * Computes the loss using the following formula: 2*Sqrt(w_0 * w_1) - w_0 -
     * w_1 Where w_0 and w_1 are the weights of the 0 and 1 labeled examples,
     * respectively If w_0 and w_1 are equal, then the loss will return 0.
     * 
     * @return Z the result of the computation
     */
    public double getLoss() {
      return 2 * Math.sqrt(m_w[0] * m_w[1]) - m_w[0] - m_w[1];
    }

    /**
     * compute the optimal binary prediction associated with this bag
     */
    public BinaryPrediction calcPrediction() {
      double smoothFactor = m_epsilon * m_totalWeight;
      double EPS = 1e-50;
      if (Double.isNaN(smoothFactor) || (Math.abs(m_totalWeight) < EPS) || (Math.abs(smoothFactor) < EPS) || Double.isNaN(m_totalWeight)) {
        return new BinaryPrediction(0.0);
      }

      BinaryPrediction p = new BinaryPrediction(m_w[1] == m_w[0] ? 0.0 : // handle
                                                                          // case
                                                                          // that
                                                                          // w0=w1=0
                                                                0.5 * Math.log((m_w[1] + smoothFactor) / (m_w[0] + smoothFactor)));
      return p;
    }

    /**
     * Compare a bag to this bag and output true if they are equal
     * 
     * @param other
     *            bag to compare to this bag
     * @return result true if this bag has the same values as the other bag
     */
    public boolean equals(BinaryBag other) {
      return (m_w[0] == other.m_w[0]) && (m_w[1] == other.m_w[1]);
    }

  }
  /** end of class BinaryBag */

}
/** end of class AdaBoost */
