package jboost.booster;

import jboost.controller.Configuration;

/**
 * AdaBoost-like implementation for the Log-Loss. The only differnece is weight
 * update.
 * 
 * @author Yoram Singer
 * @version $Header:
 *          /proj/gene/cvs-repository/jboost/src/jboost/booster/LogLossBoost.java,v
 *          1.2 2003/10/01 18:36:15 freund Exp $
 */
public class LogLossBoost extends AdaBoost {

  /**
   * Default constructors call AdaBoost constructors Uses a value of 0.0 for the
   * smoothing term
   */
  public LogLossBoost() {
    super(0.0);
  }

  /**
   * Constructor that initializes the smoothing term
   * 
   * @param smooth
   *            the value to use for the smoothing term
   */
  LogLossBoost(double smooth) {
    super(smooth);
    init(new Configuration());
  }

  /**
   * AdaBoost's finalizeData is overridden since the initial weight initialized
   * differently (q_{0,i} = 1/2)
   */
  public void finalizeData() {
    finalizeData(0.5);
  }

  /**
   * Return the theoretical bound on the training error.
   */
  public double getBound() {
    final double oneOverLog2 = 1.44269504088896340737;
    double Z = 0.;

    for (int i = 0; i < m_margins.length; i++)
      Z += Math.log(1 + Math.exp(-m_margins[i]));

    return oneOverLog2 * Z / m_margins.length;
  }

  public String getParamString() {
    String ret = String.format("None (LogLossBoost)");
    return ret;
  }

  /**
   * The LogLoss example weight is set to 1/(1 + e^(margin))
   */
  public double calculateWeight(double margin) {
    return 1 / (1 + Math.exp(margin));
  }

  /**
   * Update the examples m_margins and m_weights using the logistic loss
   * computation
   * 
   * @param predictions
   *            the value of the prediction for each example
   * @param exampleIndex
   *            the indexes of each example to update
   */
  public void update(Prediction[] predictions, int[][] exampleIndex) {
    // exchange pointers of new and old m_weights
    for (int i = 0; i < m_weights.length; i++) {
      m_oldWeights[i] = m_weights[i];
    }

    // update m_weights and m_margins
    for (int i = 0; i < exampleIndex.length; i++) {
      double p = predictions[i].getClassScores()[1];
      double[] value = new double[] { -p, p };
      int[] indexes = exampleIndex[i];
      for (int j = 0; j < indexes.length; j++) {
        int example = indexes[j];
        m_margins[example] += value[m_labels[example]];
        m_weights[example] = calculateWeight(m_margins[example]);
      }
    }
  }
}
