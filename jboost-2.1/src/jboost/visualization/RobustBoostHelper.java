package jboost.visualization;

import jboost.booster.RobustBoost;

import org.jfree.data.xy.XYSeries;

public class RobustBoostHelper {

  public static double calculateRho(boolean confRated, double sigma_f, double epsilon, double theta) {

    if (!confRated) epsilon = epsilon * 2;

    double f1 = Math.sqrt(Math.exp(2.0) * ((sigma_f * sigma_f) + 1.0) - 1.0);
    double f2 = RobustBoost.erfinv(1.0 - epsilon);
    double numer = (f1 * f2) + Math.E * theta;
    double denom = 2.0 * (Math.E - 1.0);
    return numer / denom;

  }

  public static double calculateWeight(boolean confRated, double sigma_f, double epsilon, double theta, double rho, double m, double t) {
    return RobustBoost.calculateWeight(confRated, rho, theta, sigma_f, 1.0, m, t);
  }

  public static double calculatePotential(boolean confRated, double sigma_f, double epsilon, double theta, double rho, double m, double t) {
    return RobustBoost.calculatePotential(confRated, rho, theta, sigma_f, 1.0, m, t);
  }

  public static XYSeries getPosWeightPlot(boolean confRated, double sigma_f, double epsilon, double theta, double rho, double t, double height, double min,
                                          double max, double step) {
    XYSeries weight = new XYSeries("pos weights");
    double current = min, w;
    while (current < max) {
      w = calculateWeight(confRated, sigma_f, epsilon, theta, rho, current, t) * height;
      weight.add(current, w);
      current += step;
    }
    return weight;
  }

  public static XYSeries getNegWeightPlot(boolean confRated, double sigma_f, double epsilon, double theta, double rho, double t, double height, double min,
                                          double max, double step) {
    XYSeries weight = new XYSeries("neg weights");
    double current = -max, w;
    while (current < -min) {
      w = calculateWeight(confRated, sigma_f, epsilon, theta, rho, current, t) * height;
      weight.add(-current, w);
      current += step;
    }
    return weight;
  }

  public static XYSeries getPosPotentialPlot(boolean confRated, double sigma_f, double epsilon, double theta, double rho, double t, double height, double min,
                                             double max, double step) {
    XYSeries weight = new XYSeries("pos potentials");
    double current = min, w;
    while (current < max) {
      w = calculatePotential(confRated, sigma_f, epsilon, theta, rho, current, t) * height;
      weight.add(current, w);
      current += step;
    }
    return weight;
  }

  public static XYSeries getNegPotentialPlot(boolean confRated, double sigma_f, double epsilon, double theta, double rho, double t, double height, double min,
                                             double max, double step) {
    XYSeries weight = new XYSeries("neg potentials");
    double current = -max, w;
    while (current < -min) {
      w = calculatePotential(confRated, sigma_f, epsilon, theta, rho, current, t) * height;
      weight.add(-current, w);
      current += step;
    }
    return weight;
  }

}
