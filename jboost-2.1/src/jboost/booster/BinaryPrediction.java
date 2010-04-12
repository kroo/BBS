package jboost.booster;

import jboost.examples.Label;

/**
 * This is a prediction for a binary label, which consists of a single real
 * valued number whose sign is the prediction and whose magnitude is the
 * prediction confidence
 */
class BinaryPrediction extends Prediction {

  protected double prediction;

  public BinaryPrediction(double p) {
    prediction = p;
  }

  public BinaryPrediction() {
    prediction = 0.0;
  }

  public Object clone() {
    Object a = new BinaryPrediction(prediction);
    return a;
  }

  public Prediction add(Prediction p) {
    prediction += ((BinaryPrediction) p).prediction;
    return this;
  }

  public Prediction scale(double w) {
    prediction *= w;
    return this;
  }

  public Prediction add(double w, Prediction p) {
    prediction += w * ((BinaryPrediction) p).prediction;
    return this;
  }

  public double[] getClassScores() {
    double[] a = { -prediction, prediction };
    return a;
  }

  /**
   * computes margin as y * prediction, where y = -1 if label = 0, y = +1 if
   * label = 1.
   */
  public double[] getMargins(Label label) {
    int l = label.getSingleValue();
    if (l > 1 || l < 0) {
      throw new IllegalArgumentException("Adaboost.getMargin should get a label which " + "is either 0 or 1. Instead it got " + l);
    }
    return new double[] { (l == 1 ? prediction : -prediction) };
  }

  public boolean equals(Prediction other) {
    BinaryPrediction bp = (BinaryPrediction) other;
    return (prediction == bp.prediction);
  }

  /**
   * computes margin as absolute value of prediction.
   */
  /*
   * public double getMargin() { return (prediction < 0.0 ? -prediction :
   * prediction); }
   */

  public Label getBestClass() {
    return new Label(prediction > 0 ? 1 : 0);
  }

  public String toString() {
    return "BinaryPrediction. p(1)= " + prediction;
  }

  public String shortText() {
    return Double.toString(prediction);
  }

  public String cPreamble() {
    return "typedef double Prediction_t;\n" + "#define reset_pred()  {p = 0.0;}\n" + "#define add_pred(X) {p += (X);}\n" + "#define finalize_pred()"
           + "  ((r) ? (r[1] = p , r[0] = -p) : -p)\n";
  }

  public String javaPreamble() {
    return "" + "  static private double p;\n" + "  static private void reset_pred() { p = 0.0; }\n" + "  static private void add_pred(double x) { p += x; }\n"
           + "  static private double[] finalize_pred() {\n" + "    return new double[] {-p, p};\n" + "  }\n";
  }

  public double[] toCodeArray() {
    return new double[] { prediction };
  }

}
