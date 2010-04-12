package jboost.visualization;

import org.jfree.data.xy.XYSeries;

public class AdaBoostHelper {

  public static double calculateWeight(double m) {
    return Math.exp(-1 * m);
  }

  public static double calculatePotential(double m) {
    return Math.exp(-1 * m);
  }

  public static XYSeries getPosWeightPlot(double height, double min, double max, double step) {
    XYSeries weight = new XYSeries("pos weights");
    double current = min, w;
    double offset = Math.exp(-min);
    while (current < max) {
      w = calculateWeight(current) / offset * height;
      weight.add(current, w);
      current += step;
    }
    return weight;
  }

  public static XYSeries getNegWeightPlot(double height, double min, double max, double step) {
    XYSeries weight = new XYSeries("neg weights");
    double current = -max, w;
    double offset = Math.exp(max);
    while (current < -min) {
      w = calculateWeight(current) / offset * height;
      weight.add(-current, w);
      current += step;
    }
    return weight;
  }

  public static XYSeries getPosPotentialPlot(double height, double min, double max, double step) {
    XYSeries weight = new XYSeries("pos potentials");
    double current = min, w;
    double offset = Math.exp(-min);
    while (current < max) {
      w = calculatePotential(current) / offset * height * 1.20;
      weight.add(current, w);
      current += step;
    }
    return weight;
  }

  public static XYSeries getNegPotentialPlot(double height, double min, double max, double step) {
    XYSeries weight = new XYSeries("neg potentials");
    double current = -max, w;
    double offset = Math.exp(max);
    while (current < -min) {
      w = calculatePotential(current) / offset * height * 1.20;
      weight.add(-current, w);
      current += step;
    }
    return weight;
  }

}
