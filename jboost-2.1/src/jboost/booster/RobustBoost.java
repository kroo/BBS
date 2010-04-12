package jboost.booster;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import jboost.controller.Configuration;
import jboost.examples.Label;

/**
 * Java implemantation of RobustBoost.
 * 
 * @author Sunsern Cheamanunkul
 */
public class RobustBoost extends AbstractBooster {

  /** permanent storage for m_labels */
  protected short[] m_labels;
  /** permanent storage for m_margins */
  protected double[] m_margins;
  /** permanent storage for example m_weights */
  protected double[] m_weights;
  /** permanent storage for example's old m_weights */
  protected double[] m_oldWeights;
  /** Records the potentials. Similar to m_margins and m_weights. */
  protected double[] m_potentials;
  /** sampling weights for the examples */
  protected double[] m_sampleWeights;
  /** total weight of all examples */
  protected double m_totalWeight;
  /** number of examples in training set */
  protected int m_numExamples;

  /** current RobustBoost time [0,1] */
  protected double m_t;
  /** epsilon, fraction of allowed errors */
  protected double m_epsilon;
  /** conf.rated flag */
  protected boolean m_conf_rated;
  /** sigma_f for each class */
  protected double[] m_sigma_f;
  /** theta (goal margin) for each class */
  protected double[] m_theta;
  /** rho for each class */
  protected double[] m_rho;
  /** mistake cost for each class */
  protected double[] m_cost;

  /** most recently used ds */
  protected double m_last_ds;
  /** most recently used dt */
  protected double m_last_dt;

  /** RobustBoost time at last iteration */
  protected double m_old_t;

  /** minimum epsilon that works */
  protected static final double MIN_EPSILON = 1E-6;
  /** maximum epsilon that works */
  protected static final double MAX_EPSILON = 0.999;

  /** amount of change in potential allowed near the end */
  protected double m_potentialSlack = 1e-7;

  /** grid search parameter */
  protected static final double DS_MIN = 0;
  protected static final double DS_MAX = 10;
  protected static final double DS_STEP = 0.1;
  protected static final double DT_MIN = 0.0001;
  protected static final double DT_STEP = 0.1;

  /** temporary location for storing the examples as they are read in */
  protected List<TmpData> m_tmpList;

  /**
   * default constructor conf_rated = true epsilon = 0.1 theta = { 0.0 , 0.0 }
   * sigma_f = { 0.1 , 0.1 } cost = { 1, 1 }
   */
  public RobustBoost() {

    this(true, 0.1, new double[] { 0, 0 }, new double[] { 0.1, 0.1 }, new double[] { 1, 1 });
    init(new Configuration());

  }

  public RobustBoost(boolean conf_rated, double epsilon, double[] theta, double[] sigma_f, double[] cost) {

    m_tmpList = new ArrayList<TmpData>();
    m_numExamples = 0;

    m_epsilon = Math.max(epsilon, MIN_EPSILON);
    m_epsilon = Math.min(m_epsilon, MAX_EPSILON);

    m_theta = theta;
    m_sigma_f = sigma_f;

    m_cost = cost;

    m_conf_rated = conf_rated;

    m_rho = new double[] { calculateRho(m_sigma_f[0], m_epsilon, m_theta[0], m_cost[0]), calculateRho(m_sigma_f[1], m_epsilon, m_theta[1], m_cost[1]) };

    m_t = 0.0;
    m_old_t = 0.0;

    m_last_ds = 0;
    m_last_dt = 0;

  }

  /**
   * @see jboost.booster.Booster#init(jboost.controller.Configuration)
   */
  public void init(Configuration config) {

    m_epsilon = Math.max(config.getDouble("rb_epsilon", m_epsilon), MIN_EPSILON);
    m_epsilon = Math.min(m_epsilon, MAX_EPSILON);

    double theta = config.getDouble("rb_theta", m_theta[0]);
    double sigma_f = config.getDouble("rb_sigma_f", m_sigma_f[0]);

    m_theta[0] = theta;
    m_theta[1] = theta;

    m_theta[0] = config.getDouble("rb_theta_0", m_theta[0]);
    m_theta[1] = config.getDouble("rb_theta_1", m_theta[1]);

    m_sigma_f[0] = sigma_f;
    m_sigma_f[1] = sigma_f;

    m_sigma_f[0] = config.getDouble("rb_sigma_f_0", m_sigma_f[0]);
    m_sigma_f[1] = config.getDouble("rb_sigma_f_1", m_sigma_f[1]);

    m_cost[0] = config.getDouble("rb_cost_0", m_cost[0]);
    m_cost[1] = config.getDouble("rb_cost_1", m_cost[1]);

    m_conf_rated = config.getBool("rb_conf_rated", m_conf_rated);

    m_t = config.getDouble("rb_t", m_t);

    m_rho = new double[] { calculateRho(m_sigma_f[0], m_epsilon, m_theta[0], m_cost[0]), calculateRho(m_sigma_f[1], m_epsilon, m_theta[1], m_cost[1]) };

    m_potentialSlack = config.getDouble("rb_potentialSlack", m_potentialSlack);

  }

  /**
   * Add an example to the data set of this booster
   * 
   * @param index
   * @param label
   * @param weight
   *            Sample weight
   */
  public void addExample(int index, Label label, double weight) {
    addExample(index, label, weight, 0.0);
  }

  /**
   * Add an example to the data set of this booster
   * 
   * @param index
   * @param label
   * @param weight
   *            Sample weight
   * @param margin
   */
  public void addExample(int index, Label label, double weight, double margin) {
    int l = label.getSingleValue();
    String failed = null;
    if (l == 1 || l == 0) {
      if (index == m_numExamples) {
        m_numExamples++;
        m_tmpList.add(new TmpData(index, (short) l, weight, margin));
      }
      else {
        failed = getClass().getName() + ".addExample received index " + index + ", when it expected index " + m_numExamples;
      }
    }
    else {
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
    addExample(index, label, 1.0);
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
    m_totalWeight = 0;
    m_t = 0;
    m_last_ds = 0;
    m_last_dt = 0;
  }

  /**
   * Computes m_margins, m_labels, m_weights, m_sampleWeights and m_totalWeights
   */
  public void finalizeData() {
    m_margins = new double[m_numExamples];
    m_weights = new double[m_numExamples];
    m_oldWeights = new double[m_numExamples];
    m_potentials = new double[m_numExamples];
    m_labels = new short[m_numExamples];
    m_sampleWeights = new double[m_numExamples];
    m_totalWeight = 0.0;

    for (int i = 0; i < m_tmpList.size(); i++) {
      TmpData a = (TmpData) m_tmpList.get(i);
      int index = a.getIndex();
      m_margins[index] = a.getMargin();
      m_labels[index] = a.getLabel();
      m_weights[index] = m_oldWeights[index] = calculateWeight(index, m_margins[index], m_t);
      m_potentials[index] = calculatePotential(index, m_margins[index], m_t);
      m_sampleWeights[index] = a.getWeight();
      m_totalWeight += m_weights[index] * a.getWeight();
    }

    m_tmpList.clear(); // free the memory
  }

  /**
   * Return the theoretical bound on the training error.
   */
  public double getTheoryBound() {
    double sum_potential = 0.0;
    for (int i = 0; i < m_numExamples; i++) {
      sum_potential += m_potentials[i];
    }
    return sum_potential / m_numExamples;
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
   * Returns the boosting weights of the training examples.
   */
  public double[][] getWeights() {
    double[][] r = new double[m_numExamples][1];
    for (int i = 0; i < m_numExamples; i++)
      r[i][0] = m_weights[i];
    return r;
  }

  /**
   * Returns the potential values of the training examples.
   */
  public double[][] getPotentials() {
    double[][] r = new double[m_numExamples][1];
    for (int i = 0; i < m_numExamples; i++)
      r[i][0] = m_potentials[i];
    return r;
  }

  /**
   * Returns the number of training examples.
   */
  public int getNumExamples() {
    return m_numExamples;
  }

  /**
   * @return total weight of examples
   */
  public double getTotalWeight() {
    return m_totalWeight;
  }

  /**
   * @return current value of rho
   */
  public double[] getRho() {
    return m_rho;
  }

  /**
   * @return current value of sigma_f
   */
  public double[] getSigmaF() {
    return m_sigma_f;
  }

  /**
   * @return current value of theta
   */
  public double[] getTheta() {
    return m_theta;
  }

  /**
   * @return current time in RobustBoost training process
   */
  public double getCurrentTime() {
    return m_t;
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
    String ret = String.format("None (" + getClass().getName() + ")");
    return ret;
  }

  /** output contents of this booster as a human-readable string */
  public String toString() {
    String s = getClass().getName() + ". No of examples = " + m_numExamples;
    s += "\nindex\tmargin\tweight\told weight\tsample weight\tlabel\n";
    NumberFormat f = new DecimalFormat("0.00");
    for (int i = 0; i < m_numExamples; i++) {
      s +=
        "  " + i + " \t " + f.format(m_margins[i]) + " \t " + f.format(m_weights[i]) + " \t " + f.format(m_oldWeights[i]) + " \t"
        + f.format(m_sampleWeights[i]) + "\t\t" + m_labels[i] + "\n";
    }
    return s;
  }

  public Bag newBag(int[] list) {
    return new RobustBinaryBag(list);
  }

  public Bag newBag() {
    return new RobustBinaryBag();
  }

  public Bag newBag(Bag bag) {
    return new RobustBinaryBag((RobustBinaryBag) bag);
  }

  /*
   * Returns the predictions associated with a list of bags representing a
   * partition of the data.
   */
  public Prediction[] getPredictions(Bag[] b) {

    throw new RuntimeException("RobustBoost.getPrediction(Bag[] b) is called. " + "This should never happen.");

  }

  /**
   * Returns the predictions associated with a list of bags representing a
   * partition of the data.
   */
  public Prediction[] getPredictions(Bag[] bags, int[][] exampleIndex) {
    Prediction[] basePredictions = new Prediction[bags.length];
    for (int i = 0; i < bags.length; i++) {
      basePredictions[i] = ((RobustBinaryBag) bags[i]).calcPrediction();
    }
    return getPredictions(bags, exampleIndex, basePredictions);
  }

  /**
   * Returns the predictions associated with a list of bags representing a
   * partition of the data. For RobustBoost, this returns predictions from weak
   * rules. Their values will get adjusted in update().
   */
  public Prediction[] getPredictions(Bag[] bags, int[][] exampleIndex, Prediction[] basePredictions) {

    final double EPS = 1E-7;

    Prediction[] predictions = new RobustBinaryPrediction[bags.length];

    for (int i = 0; i < bags.length; i++) {

      // bp = prediction from base classifier where its magnitude
      // represent its confidence
      double bp = basePredictions[i].getClassScores()[1];

      // if bp (confidence of weak learner) is too small or bag is weightless,
      // don't make any prediction
      if (Math.abs(bp) < EPS || bags[i].isWeightless()) {
        predictions[i] = new RobustBinaryPrediction(0.0);
      }
      else {

        // if this is not conf rated, we make base predictions into {-1,1}
        if (!m_conf_rated) {
          bp = (bp < 0 ? -1 : 1);
        }
       
        predictions[i] = new RobustBinaryPrediction(bp);

        // save init_ds for NewtonSolver
        ((RobustBinaryPrediction) predictions[i]).init_ds = Math.abs(((RobustBinaryBag) bags[i]).getAdaBoostAlpha(0.5));

      }
    }

    return predictions;

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

    final double EPS = 1E-7;

    // save old m_weights
    for (int i = 0; i < m_weights.length; i++)
      m_oldWeights[i] = m_weights[i];

    m_old_t = m_t;

    // for each prediction
    for (int i = 0; i < predictions.length; i++) {

      if (!(predictions[i] instanceof RobustBinaryPrediction)) {
        throw new RuntimeException("RobustBoost.update() only works with RobustBinaryPrediction");
      }

      RobustBinaryPrediction rbp = (RobustBinaryPrediction) predictions[i];

      // if already finished, set the rest of the predictions to empty.
      if (isFinished()) {
        rbp.dt = 0;
        rbp.prediction = 0;
        continue;
      }

      double[] value = rbp.getClassScores();

      // if this is a zero prediction or the set is empty, we skip
      // this weak hypothesis
      if (Math.abs(value[1]) < EPS) {
        continue;
      }
      
      // create a mask indicating if an example is in exampleIndex[i]
      boolean[] mask = new boolean[m_numExamples];
      for (int j = 0; j < exampleIndex[i].length; j++)
        mask[exampleIndex[i][j]] = true;

      boolean foundSolution = false;
      double ds = Double.NaN;
      double dt = Double.NaN;

      // create a solver
      NewtonSolver ns = new NewtonSolver(this, mask, value);
      HeuristicSolver hs = new HeuristicSolver(this, mask, value);

//    if (ns.canFinishNow()) {

//    foundSolution = true;
//    ds = 0;
//    dt = Math.abs(1 - m_t);

//    }
//    else {
      // create a set of starting points for NS
      double init_ds, init_dt;
      double[][] initial_points = new double[3][];

      // #1. go as far in the future as possible
      init_dt = 1 - m_t;
      init_ds = Math.sqrt(init_dt);
      initial_points[0] = new double[] { init_ds, init_dt };

      // #2. alpha in adaboost
      init_ds = rbp.init_ds;
      init_dt = init_ds * init_ds;
      initial_points[1] = new double[] { init_ds, init_dt };

      // #3. most recently used
      init_ds = m_last_ds;
      init_dt = m_last_dt;
      initial_points[2] = new double[] { init_ds, init_dt };

      for (int k = 0; k < initial_points.length; k++) {
        ns.solve(initial_points[k][0], initial_points[k][1]);
        if (ns.isSucceeded()) {
          ds = ns.getDs();
          dt = ns.getDt();
          foundSolution = true;
          break;
        }
      }

      // if failed, try heuristic
      if (!foundSolution) {
        hs.solve();
        ds = hs.getDs();
        dt = hs.getDt();
        foundSolution = hs.isSucceeded();
      }

      // if there is a valid solution
      if (foundSolution && 
          !Double.isNaN(ds) && 
          !Double.isNaN(dt) && 
          dt >= 0) {

        m_last_ds = ds;
        m_last_dt = dt;

        // update m_t
        m_t += dt;

        // update prediction
        rbp.prediction = rbp.prediction * ds;
        rbp.dt = dt;

        value[0] *= ds;
        value[1] *= ds;

        // update m_margins
        double exp_negative_dt = Math.exp(-dt);

        for (int j = 0; j < m_numExamples; j++) {
          if (mask[j]) m_margins[j] = m_margins[j] * exp_negative_dt + value[m_labels[j]];
          else m_margins[j] = m_margins[j] * exp_negative_dt;
          // if (j==0) System.out.println("m_margins[0]=" + m_margins[0]);
        }
      }

      // no solutions found
      else {

        System.out.println("WARNING: Solvers have failed. If time is still increasing, please ignore this warning.");

        m_last_ds = 0;
        m_last_dt = 0;

        rbp.prediction = 0;
        rbp.dt = 0;

      }

      // System.out.println("T = " + m_t);
    }

    // update m_weights and m_potentials
    m_totalWeight = 0;
    for (int j = 0; j < m_numExamples; j++) {
      m_weights[j] = calculateWeight(j, m_margins[j], m_t);
      m_potentials[j] = calculatePotential(j, m_margins[j], m_t);
      m_totalWeight += m_weights[j] * m_sampleWeights[j];
    }

  }

  /**
   * Stop when the time is really close to 1.0
   * 
   * @return
   */
  public boolean isFinished() {
    return (1 - m_t < 0.001);
  }

  public static double calculateSigmaSquare(double sigma_f, double t) {
    if (t > 1) return sigma_f * sigma_f;
    else return (sigma_f * sigma_f + 1.0) * Math.exp(2.0 * (1.0 - t)) - 1.0;
  }

  public static double calculateSigma(double sigma_f, double t) {
    if (t > 1) return sigma_f;
    else return Math.sqrt(calculateSigmaSquare(sigma_f, t));
  }

  public static double calculateMu(double rho, double theta, double t) {
    if (t > 1) return theta;
    else {
      double rho_2 = 2 * rho;
      return (theta - rho_2) * Math.exp(1.0 - t) + rho_2;
    }
  }

  /**
   * evaluate potential function at margin m and time t based on rho, theta,
   * sigma_f
   * 
   * @param conf_rated
   * @param rho
   * @param theta
   * @param sigma_f
   * @param m
   * @param t
   * @return Phi(m,t)
   */
  public static double calculatePotential(boolean conf_rated, double rho, double theta, double sigma_f, double cost, double m, double t) {

    if (conf_rated) {
      return cost * Math.min(1.0, 1.0 - erf((m - calculateMu(rho, theta, t)) / calculateSigma(sigma_f, t)));
    }
    else {
      return cost * 0.5 * (1.0 - erf((m - calculateMu(rho, theta, t)) / calculateSigma(sigma_f, t)));
    }

  }

  public double calculatePotential(int exampleIndex, double m, double t) {

    // get rho, theta, sigma_f based on label
    double rho = m_rho[m_labels[exampleIndex]];
    double theta = m_theta[m_labels[exampleIndex]];
    double sigma_f = m_sigma_f[m_labels[exampleIndex]];
    double cost = m_cost[m_labels[exampleIndex]];

    return calculatePotential(m_conf_rated, rho, theta, sigma_f, cost, m, t);
  }

  /**
   * evaluate weight function at margin m and time t based on rho, theta,
   * sigma_f
   * 
   * @param conf_rated
   * @param rho
   * @param theta
   * @param sigma_f
   * @param m
   * @param t
   * @return w(m,t)
   */
  public static double calculateWeight(boolean conf_rated, double rho, double theta, double sigma_f, double cost, double m, double t) {

    double mu_t = calculateMu(rho, theta, t);
    double sigma_t_sq = calculateSigmaSquare(sigma_f, t);

    if (conf_rated) {
      if (m > mu_t) return cost * Math.exp(-((m - mu_t) * (m - mu_t)) / (sigma_t_sq));
      else return 0.0;
    }
    else {
      return cost * Math.exp(-((m - mu_t) * (m - mu_t)) / (sigma_t_sq));
    }

  }

  public double calculateWeight(int exampleIndex, double m, double t) {

    // get rho, theta, sigma_f based on label
    double rho = m_rho[m_labels[exampleIndex]];
    double theta = m_theta[m_labels[exampleIndex]];
    double sigma_f = m_sigma_f[m_labels[exampleIndex]];
    double cost = m_cost[m_labels[exampleIndex]];

    return calculateWeight(m_conf_rated, rho, theta, sigma_f, cost, m, t);

  }

  public double calculateWeight(Label label, double m, double t) {

    // get rho, theta, sigma_f based on label
    double rho = m_rho[label.getSingleValue()];
    double theta = m_theta[label.getSingleValue()];
    double sigma_f = m_sigma_f[label.getSingleValue()];
    double cost = m_cost[label.getSingleValue()];

    return calculateWeight(m_conf_rated, rho, theta, sigma_f, cost, m, t);

  }

  public double calculateWeight(double margin) {
    throw new RuntimeException("calculateWeight(double margin) should never be called.");
  }

  public double getEffectiveNumExamples() {
    double wi = 0, wiSq = 0;
    for (int i = 0; i < m_numExamples; i++) {
      wi += m_weights[i];
      wiSq += m_weights[i] * m_weights[i];
    }
    if (Math.abs(wiSq) < 1e-12) return 0;
    else return wi * wi / wiSq;
  }

  public int getNumExamplesHigherThan(double threshold) {
    int num = 0;
    for (int i = 0; i < m_numExamples; i++) {
      if (m_weights[i] > threshold) num++;
    }
    return num;
  }

  /**
   * compute rho
   * 
   * @return rho
   */
  private double calculateRho(double sigma_f, double epsilon, double theta, double cost) {

    if (!m_conf_rated) epsilon = epsilon * 2;

    epsilon /= cost;

    double f1 = Math.sqrt(Math.exp(2.0) * ((sigma_f * sigma_f) + 1.0) - 1.0);
    double f2 = erfinv(1.0 - epsilon);
    double numer = (f1 * f2) + Math.E * theta;
    double denom = 2.0 * (Math.E - 1.0);
    return numer / denom;

  }

  public String getParameters() {
    String ret = "rb_t = " + m_t + "\n";
    ret += "rb_epsilon = " + m_epsilon + "\n";
    ret += "rb_theta_0 = " + m_theta[0] + "\n";
    ret += "rb_theta_1 = " + m_theta[1] + "\n";
    ret += "rb_sigma_f_0 = " + m_sigma_f[0] + "\n";
    ret += "rb_sigma_f_1 = " + m_sigma_f[1] + "\n";
    ret += "rb_cost_0 = " + m_cost[0] + "\n";
    ret += "rb_cost_1 = " + m_cost[1] + "\n";
    ret += "rb_conf_rated = " + m_conf_rated + "\n";
    ret += "rb_potentialSlack = " + m_potentialSlack;
    return ret;
  }

  /**
   * Already checked against Matlab. When |z| > 1.0, we approximate using the
   * Chebyshev fitting formula from
   * http://www.cs.princeton.edu/introcs/21function/ErrorFunction.java.html When
   * |z| <= 1.0, we use Taylor expansion at z=0 to approximate. ref:
   * http://en.wikipedia.org/wiki/Error_function
   * 
   * @param z
   * @return erf(z)
   */
  public static double erf(double z) {

    if (Math.abs(z) > 1.0) {

      // fractional error in math formula less than 1.2 * 10 ^ -7.
      // although subject to catastrophic cancellation when z in very close to 0
      // from Chebyshev fitting formula for erf(z) from Numerical Recipes, 6.2

      double t = 1.0 / (1.0 + 0.5 * Math.abs(z));

      // use Horner's method
      double ans =
        1.0
        - t
        * Math
        .exp(-z
             * z
             - 1.26551223
             + t
             * (1.00002368 + t
                 * (0.37409196 + t
                     * (0.09678418 + t
                         * (-0.18628806 + t
                             * (0.27886807 + t
                                 * (-1.13520398 + t
                                     * (1.48851587 + t
                                         * (-0.82215223 + t * (0.17087277))))))))));
      if (z >= 0) return ans;
      else return -ans;

    }
    else {

      // taylor expansion
      double EPS = 1E-7;
      double[] k = new double[] { 1, -3, 10, -42, 216, -1320, 9360, -75600, 685440, -6894720, 76204800, -918086400 };

      double t = 1.0, z_sq = z * z, ans = 0.0;

      for (int i = 0; i < k.length; i++) {
        ans += t / k[i];
        t *= z_sq;
      }

      ans = ans * 2.0 * z / Math.sqrt(Math.PI);

      if (Math.abs(ans) < EPS) return 0.0;
      return ans;

    }
  }

  /**
   * Already checked against Matlab. This is good enough.
   * 
   * @param z
   * @return erfinv(z)
   */
  public static double erfinv(double z) {

    double EPS = 1E-7;

    if (z >= 1.0 - EPS) return Double.POSITIVE_INFINITY;
    if (z <= -1.0 + EPS) return Double.NEGATIVE_INFINITY;

    double t = Math.abs(z);

    double a = 0.147;
    double pi = Math.PI;
    double t1 = (2 / (pi * a));
    double t2 = Math.log(1 - t * t);
    double t3 = Math.sqrt((t1 + t2 / 2) * (t1 + t2 / 2) - t2 / a);

    if (z < 0) return -1 * Math.sqrt(-t1 - t2 / 2 + t3);
    else return Math.sqrt(-t1 - t2 / 2 + t3);
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
     * @param margin
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
     * @return m_margin
     */
    protected double getMargin() {
      return m_margin;
    }
  }

  /**
   * Bag for Robustboost. Same as BinaryBag with calcPrediction rewritten
   * 
   * @author Sunsern Cheamanunkul
   */
  public class RobustBinaryBag extends Bag {

    /** default constructor */
    protected RobustBinaryBag() {
      m_w = new double[2];
      reset();
    }

    /** constructor that copies an existing bag */
    protected RobustBinaryBag(RobustBinaryBag bag) {
      m_w = new double[2];
      m_w[0] = bag.m_w[0];
      m_w[1] = bag.m_w[1];
    }

    /** a constructor that initializes a bag the given list of examples */
    protected RobustBinaryBag(int[] list) {
      m_w = new double[2];
      reset();
      this.addExampleList(list);
    }

    public String toString() {
      String s = "RobustBinaryBag.\t w0=" + m_w[0] + "\t w1=" + m_w[1] + "\n";
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
      m_w[m_labels[index]] += (m_weights[index] * m_sampleWeights[index]);
    }

    /**
     * Subtracts one example index from the bag.
     */
    public void subtractExample(int i) {
      if ((m_w[m_labels[i]] -= (m_weights[i] * m_sampleWeights[i])) < 0.0) m_w[m_labels[i]] = 0.0;
    }

    /**
     * Adds the given bag to this one. It is assumed that the two bags are
     * disjoint and the same type.
     */
    public void addBag(Bag b) {
      m_w[0] += ((RobustBinaryBag) b).m_w[0];
      m_w[1] += ((RobustBinaryBag) b).m_w[1];
    }

    /**
     * Subtracts the given bag from this one. It is assumed that the bag being
     * subtracted is a subset of the other one, and that the two bags are the
     * same type.
     */
    public void subtractBag(Bag b) {
      if ((m_w[0] -= ((RobustBinaryBag) b).m_w[0]) < 0.0) m_w[0] = 0.0;
      if ((m_w[1] -= ((RobustBinaryBag) b).m_w[1]) < 0.0) m_w[1] = 0.0;
    }

    /**
     * Copies a given bag of the same type into this one.
     */
    public void copyBag(Bag b) {
      m_w[0] = ((RobustBinaryBag) b).m_w[0];
      m_w[1] = ((RobustBinaryBag) b).m_w[1];
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
     * compute the optimal binary prediction associated with this bag this might
     * need to be rewritten
     */
    public BinaryPrediction calcPrediction() {
      double EPS = 1E-7;
      double totalWeight = m_w[0] + m_w[1];
      if (Double.isNaN(totalWeight) || Math.abs(totalWeight) < EPS || Math.abs(m_w[0] - m_w[1]) < EPS) {
        return new BinaryPrediction(0.0);
      }
      else {
	// {1.0,-1.0}
	double pred = (m_w[1] > 0) ? 1.0 : -1.0; 
        return new BinaryPrediction(pred);
      }
    }

    public double getAdaBoostAlpha(double smooth) {
      double EPS = 1E-7;
      double totalWeight = m_w[0] + m_w[1];
      if (Double.isNaN(totalWeight) || Math.abs(totalWeight) < EPS || Math.abs(m_w[0] - m_w[1]) < EPS) {
        return 0;
      }
      else {
        return 0.5 * Math.log((m_w[1] + smooth) / (m_w[0] + smooth));
      }
    }

    /**
     * Compare a bag to this bag and output true if they are equal
     * 
     * @param other
     *            bag to compare to this bag
     * @return result true if this bag has the same values as the other bag
     */
    public boolean equals(RobustBinaryBag other) {
      return (m_w[0] == other.m_w[0]) && (m_w[1] == other.m_w[1]);
    }

  }

  /** end of class RobustBinaryBag */

  /**
   * This class is used to solve for ds and dt
   */
  public static class NewtonSolver {

    final static double RHS_EPS = 1E-7;
    final static double DET_EPS = 1E-7;
    final static int MAX_ITER = 30;

    protected StringBuffer log;

    protected boolean[] mask;
    protected double[] value;
    protected double t;
    protected double ds, dt;
    protected RobustBoost rb;

    protected boolean succeeded;

    final double SQRTPI = Math.sqrt(Math.PI);

    public NewtonSolver(RobustBoost rb, boolean[] mask, double[] value) {

      this.rb = rb;
      this.mask = mask;
      this.value = value;

      this.t = rb.m_t;

      succeeded = false;

      this.ds = Double.NaN;
      this.dt = Double.NaN;

      log = new StringBuffer();
    }

    /**
     * Suppose X is the output of calculateFandJ X[0..1] = F[0..1] X[2..5] =
     * J[0..3]
     * 
     * @param ds
     * @param dt
     * @return
     */

    private double[] calculateFAndJ(double ds, double dt) {

      double[] output = new double[6];
      int l;
      double new_t, new_margin, new_weight, step, temp;
      double[] new_mu_t, new_sigma_t, new_sigma_t_sq;
      double new_A, new_A_ds, new_A_dt, exp_neg_dt;

      new_t = t + dt;
      exp_neg_dt = Math.exp(-dt);

      new_mu_t = new double[] { RobustBoost.calculateMu(rb.m_rho[0], rb.m_theta[0], new_t), RobustBoost.calculateMu(rb.m_rho[1], rb.m_theta[1], new_t) };
      new_sigma_t_sq = new double[] { RobustBoost.calculateSigmaSquare(rb.m_sigma_f[0], new_t), RobustBoost.calculateSigmaSquare(rb.m_sigma_f[1], new_t) };

      new_sigma_t = new double[] { Math.sqrt(new_sigma_t_sq[0]), Math.sqrt(new_sigma_t_sq[1]) };

      for (int i = 0; i < rb.m_numExamples; i++) {

        l = rb.m_labels[i];
        step = (mask[i] ? value[l] : 0);

        new_margin = rb.m_margins[i] * exp_neg_dt + step * ds;
        new_weight = rb.calculateWeight(i, new_margin, new_t);

        new_A = (new_margin - new_mu_t[l]) / new_sigma_t[l];
        new_A_ds = step / new_sigma_t[l];
        new_A_dt = -(rb.m_margins[i] * exp_neg_dt - new_mu_t[l] + 2 * rb.m_rho[l]) / new_sigma_t[l] + (new_A) * (new_sigma_t_sq[l] + 1) / new_sigma_t_sq[l];

        temp = step * new_weight;
        output[0] += temp;
        output[1] += rb.calculatePotential(i, rb.m_margins[i], t) - rb.calculatePotential(i, new_margin, new_t);

        temp = temp * new_A;
        output[2] += temp * new_A_ds;
        output[3] += temp * new_A_dt;
        output[4] += new_weight * new_A_ds;
        output[5] += new_weight * new_A_dt;

      }

      output[2] *= -2.0;
      output[3] *= -2.0;
      output[4] *= 2 / SQRTPI;
      output[5] *= 2 / SQRTPI;

      return output;
    }

    /**
     * output is [a b c d] where a = d f1 / ds b = d f1 / dt c = d f2 / ds d = d
     * f2 / dt
     * 
     * @return
     */
    public double[] calculateJ(double ds, double dt) {

      double[] output = new double[] { 0, 0, 0, 0 };
      double new_t, new_margin, new_weight;
      double[] new_mu_t, new_sigma_t, new_sigma_t_sq;
      double new_A, new_A_ds, new_A_dt;
      double step, exp_neg_dt, temp;
      int l;

      new_t = t + dt;
      exp_neg_dt = Math.exp(-dt);

      new_mu_t = new double[] { RobustBoost.calculateMu(rb.m_rho[0], rb.m_theta[0], new_t), RobustBoost.calculateMu(rb.m_rho[1], rb.m_theta[1], new_t) };
      new_sigma_t_sq = new double[] { RobustBoost.calculateSigmaSquare(rb.m_sigma_f[0], new_t), RobustBoost.calculateSigmaSquare(rb.m_sigma_f[1], new_t) };

      new_sigma_t = new double[] { Math.sqrt(new_sigma_t_sq[0]), Math.sqrt(new_sigma_t_sq[1]) };

      for (int i = 0; i < rb.m_numExamples; i++) {

        l = rb.m_labels[i];
        step = (mask[i] ? value[l] : 0);

        new_margin = rb.m_margins[i] * exp_neg_dt + step * ds;
        new_weight = rb.calculateWeight(i, new_margin, new_t);

        new_A = (new_margin - new_mu_t[l]) / new_sigma_t[l];
        new_A_ds = step / new_sigma_t[l];
        new_A_dt = -(rb.m_margins[i] * exp_neg_dt - new_mu_t[l] + 2 * rb.m_rho[l]) / new_sigma_t[l] + (new_A) * (new_sigma_t_sq[l] + 1) / new_sigma_t_sq[l];

        temp = step * new_weight * new_A;
        output[0] += temp * new_A_ds;
        output[1] += temp * new_A_dt;
        output[2] += new_weight * new_A_ds;
        output[3] += new_weight * new_A_dt;
      }

      output[0] *= -2.0;
      output[1] *= -2.0;
      output[2] *= 2 / SQRTPI;
      output[3] *= 2 / SQRTPI;

      return output;
    }

    /**
     * output is [a b] where a = f1(ds,dt) b = f2(ds,dt)
     * 
     * @return
     */
    public double[] calculateF(double ds, double dt) {

      double[] output = new double[] { 0, 0 };
      double new_t, new_margin, new_weight;
      double step;

      new_t = t + dt;

      for (int i = 0; i < rb.m_numExamples; i++) {

        step = (mask[i] ? value[rb.m_labels[i]] : 0);

        new_margin = rb.m_margins[i] * Math.exp(-dt) + step * ds;
        new_weight = rb.calculateWeight(i, new_margin, new_t);

        output[0] += step * new_weight;
        output[1] += rb.calculatePotential(i, rb.m_margins[i], t) - 
        rb.calculatePotential(i, new_margin, new_t);

      }

      return output;
    }

    public void solve(double init_ds, double init_dt) {

      succeeded = false;

      ds = init_ds;
      dt = init_dt;

      double[] F;
      double[] J;
      double dds, ddt, det;

      for (int i = 0; i < MAX_ITER; i++) {

        // log.append("iter: " + i + ", ds: " + ds + ", dt: " + dt + "\n");

        // calculate F and J
        double[] FJ = calculateFAndJ(ds, dt);
        F = new double[] { FJ[0], FJ[1] };
        J = new double[] { FJ[2], FJ[3], FJ[4], FJ[5] };

        // solve for dds, ddt
        F[0] = -F[0];
        F[1] = -F[1];

        // Found a solution
        if (Math.abs(F[0]) < RHS_EPS && Math.abs(F[1]) < RHS_EPS && 
	    dt > 0 && dt+t-RHS_EPS < 1.0) {

          // log.append("Found a solution in " + i + " iterations!\n");
          // log.append("> ds = " + ds + "\n");
          // log.append("> dt = " + dt + "\n");
          // log.append("> F[0] =" + F[0] + "\n");
          // log.append("> F[1] =" + F[1] + "\n");

          succeeded = true;
          break;
        }

        // check determinant
        det = J[0] * J[3] - J[1] * J[2];
        if (Math.abs(det) < DET_EPS) {
          // log.append("The Jacobian is a singular matrix!\n");
          // log.append("det(J) = " + det + "\n");
          // log.append("J[0] = " + J[0] + "\n");
          // log.append("J[1] = " + J[1] + "\n");
          // log.append("J[2] = " + J[2] + "\n");
          // log.append("J[3] = " + J[3] + "\n");
          // log.append("F[0] = " + F[0] + "\n");
          // log.append("F[1] = " + F[1] + "\n");

          // if this solution is ok
          if (Math.abs(F[0]) < RHS_EPS && Math.abs(F[1]) < RHS_EPS && 
	      dt > 0 && dt+t-RHS_EPS < 1.0) {
            // log.append("Found a solution in " + i + " iterations!\n");
            // log.append("> ds = " + ds + "\n");
            // log.append("> dt = " + dt + "\n");
            // log.append("> F[0] =" + F[0] + "\n");
            // log.append("> F[1] =" + F[1] + "\n");
            succeeded = true;
          }

          break;
        }
        else {

          dds = (J[3] * F[0] - J[1] * F[1]) / det;
          ddt = (J[0] * F[1] - J[2] * F[0]) / det;

          // log.append("det(J) = " + det + "\n");
          // log.append("J[0] = " + J[0] + "\n");
          // log.append("J[1] = " + J[1] + "\n");
          // log.append("J[2] = " + J[2] + "\n");
          // log.append("J[3] = " + J[3] + "\n");
          //
          // log.append("F[0] = " + F[0] + "\n");
          // log.append("F[1] = " + F[1] + "\n");
          //
          // log.append("dds = " + dds + "\n");
          // log.append("ddt = " + ddt + "\n");

        }

        // update ds and dt
        ds += dds;
        dt += ddt;
      }

      if (!succeeded) {
        log.append("NewtonSolver failed!\n");
        ds = Double.NaN;
        dt = Double.NaN;
      }
      else {
        log.append("NewtonSolver completed successfully!\n");
      }
    }

    public boolean isSucceeded() {
      return succeeded;
    }

    public double getDs() {
      return ds;
    }

    public double getDt() {
      return dt;
    }

    public String getLog() {
      return log.toString();
    }

//  public boolean canFinishNow() {
//  double[] F = calculateF(0, 1 - t);
//  double avgPotChange = -F[1] / m_numExamples;
//  if (avgPotChange <= Math.abs(m_potentialSlack)) {

//  if (avgPotChange > 0) {

//  System.out.println("WARNING: RobustBoost is terminating with some increase in the");
//  System.out.println(" average potential. Use a smaller -rb_potentialSlack if you think");
//  System.out.println(" this is a mistake. [Avg Potential Increased by: " + avgPotChange + "]");
//  }

//  return true;
//  }
//  else return false;
//  }

  }

  /**
   * This class is used to solve for ds and dt using a heuristic
   */
  public class HeuristicSolver {

    final static double EPS = 1E-7;
    final static int MAX_ITER = 30;

    protected StringBuffer log;

    protected RobustBoost rb;
    protected boolean[] mask;
    protected double[] value;
    protected double t;
    protected double ds;
    protected double dt;

    protected boolean succeeded;

    final double SQRTPI = Math.sqrt(Math.PI);

    public HeuristicSolver(RobustBoost rb, boolean[] mask, double[] value) {

      this.rb = rb;
      this.mask = mask;
      this.value = value;

      this.t = rb.m_t;

      succeeded = false;

      this.ds = Double.NaN;
      this.dt = Double.NaN;

      log = new StringBuffer();
    }

    /**
     * output is [a b] where a = f1(ds,dt) b = f2(ds,dt)
     * 
     * @return
     */
    public double[] calculateF(double ds, double dt) {

      double[] output = new double[] { 0, 0 };
      double new_t, new_margin, new_weight;
      double step;

      new_t = t + dt;

      for (int i = 0; i < rb.m_numExamples; i++) {

        step = (mask[i] ? value[rb.m_labels[i]] : 0);

        new_margin = rb.m_margins[i] * Math.exp(-dt) + step * ds;
        new_weight = rb.calculateWeight(i, new_margin, new_t);

        output[0] += step * new_weight;
        output[1] += rb.calculatePotential(i, new_margin, new_t) -
        rb.calculatePotential(i, rb.m_margins[i], t); 

      }

      return output;
    }

    public void solve() {

      succeeded = false;

      double[] F,F2;

      ////  DEBUG  ////
      if (false && t > .07) {
        try {
          //BufferedWriter bw1 = new BufferedWriter(new FileWriter("RB_Letter_f0_bad_4.txt"));
          BufferedWriter bw2 = new BufferedWriter(new FileWriter("RB_Letter_f1_bad_4.txt"));

          for (ds=0;ds<50;ds+=0.1) {
            for (dt=0;dt<1.0;dt+=0.01) {
              F = calculateF(ds,dt);
              if (dt==0) {
                //bw1.append("" + F[0]);
                bw2.append("" + F[1]);
              }
              else {
                //bw1.append("," + F[0]);
                bw2.append("," + F[1]);
              }
            }
            //bw1.append("\n");
            bw2.append("\n");
          }

          //bw1.close();
          bw2.close();


        }
        catch (IOException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
        System.exit(-1);
      }
      //////////////

      ds = 0;
      dt = 0;
      F  = calculateF(ds,dt);

      double dds,dF;
      double ddt = 1.0;
      
      while (Math.abs(F[0]) > EPS || Math.abs(F[1]) > EPS) {

        // update ds using F[0]/F'[0]
        F2 = calculateF(ds+EPS,dt);
        dF = (F2[0]-F[0])/EPS;
        if (Math.abs(dF) < EPS) {
          dF = (dF<0)?-1:1;
        }   
        dds = F[0]/dF;
        // don't jump too far
        if (Math.abs(dds) > 1.0) {
          //System.out.println("> dds = " + dds);
          //System.out.println("> F[0] = " + F[0]);
          //System.out.println("> F2[0] = " + F2[0]);
          dds = (dds<0)?-1:1;
        }
        ds -= dds;

        // lower-bound and upper-bound for dt
        double dt_L = dt;      
        double dt_R = 1.0 - t; 
        double old_t = dt;
        
        // Binary search for dt
        while (Math.abs(dt_L-dt_R) > EPS*EPS) {
          dt = (dt_L + dt_R)/2;
          F  = calculateF(ds,dt);
          if (F[1] > 0) dt_R = dt;
          else dt_L = dt;
          // if F[1] is good enough, break
          if (Math.abs(F[1]) < EPS) break;
        }
        ddt = dt-old_t;
        
//        System.out.println(">>>   ds = " + ds);
//        System.out.println(">>>   dt = " + dt);
//        System.out.println(">>> F[0] = " + F[0]);
//        System.out.println(">>> F[1] = " + F[1]);
        
        if (Math.abs(ds) > 1e5) {
          System.out.println("ds too large. report this issue to sunsern");
        }

        F  = calculateF(ds,dt);

        // we can finish now if ...
        if (dt > 1-t-EPS && F[1] < EPS) break;
        if (Math.abs(ddt) < EPS) break;
      }

      F = calculateF(ds,dt);

      if (Math.abs(F[0]) < EPS && Math.abs(F[1]) < EPS) {
        succeeded = true;
//        System.out.println("GOOD: normal case");
//        System.out.println("> dt = " + dt);
//        System.out.println("> ds = " + ds);
//        System.out.println("> F[0] = " + F[0]);
//        System.out.println("> F[1] = " + F[1]);
      }
      else if (dt > 1-t-EPS && F[1] < EPS) {
        succeeded = true;
//	  System.out.println("too easy.try decrese epsilon or increase theta.");
//        System.out.println("GOOD: border case");
//        System.out.println("> dt = " + dt);
//        System.out.println("> ds = " + ds);
//        System.out.println("> F[0] = " + F[0]);
//        System.out.println("> F[1] = " + F[1]);
      }
      else {

	if (Math.abs(F[1]) < EPS && Math.abs(ddt) < EPS) {
          succeeded = true;
	  // System.out.println("too hard. try increse epsilon or decrease theta.");
        }
        else {
          succeeded = false;
          System.out.println("This should never happen!");
          System.out.println("> dt = " + dt);
          System.out.println("> ds = " + ds);
          System.out.println("> F[0] = " + F[0]);
          System.out.println("> F[1] = " + F[1]);
        }
      }

      if (!succeeded) {
        ds = Double.NaN;
        dt = Double.NaN;
        log.append("HeuristicSolver failed!\n");
      }
      else {
        log.append("HeuristicSolver completed successfully!\n");
      }

    }

    public boolean isSucceeded() {
      return succeeded;
    }

    public double getDs() {
      return ds;
    }

    public double getDt() {
      return dt;
    }

    public String getLog() {
      return log.toString();
    }

  }


}
