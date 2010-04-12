package jboost.visualization;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import org.jfree.data.xy.XYIntervalSeries;
import org.jfree.data.xy.XYSeries;

/**
 * @author yoavfreund A data structure that stores a set of scores and labels
 *         for examples. Used to store the data for {@HistogramFrame}.
 */
public class DataSet {

  private String[] iterList;

  private int iteration = 0;

  private String outputFilename = "ExamplesDumpFile.txt";

  /**
   * data is a list of ArrayLists, each list element corresponds to a (boosting)
   * iteration, Each List entry consists of a sorted list of DataElements,
   * sorted according to their score.
   */
  private ArrayList<ArrayList<DataElement>> data;
  private XYIntervalSeries[] fluctBinSeries;
  private XYIntervalSeries[] fluctRangeSeries;

  // private double minScore,maxScore;
  private double[] minScores, maxScores;
  private double[] minRanges, maxRanges;

  private int total_pos, total_neg;
  private int neg_label = -1, pos_label = +1;

  public DataSet(String[] iterList) {
    this.iterList = iterList;
    data = new ArrayList<ArrayList<DataElement>>();
    minScores = new double[iterList.length];
    maxScores = new double[iterList.length];

    minRanges = new double[iterList.length];
    maxRanges = new double[iterList.length];

    for (int i = 0; i < iterList.length; i++) {
      data.add(new ArrayList<DataElement>());
    }
    initFluctSeries(iterList.length);
  }

  private void initFluctSeries(int iterations) {
    fluctBinSeries = new XYIntervalSeries[iterations];
    fluctRangeSeries = new XYIntervalSeries[iterations];
    for (int i = 0; i < iterations; i++) {
      fluctBinSeries[i] = new XYIntervalSeries("Bins");
      fluctRangeSeries[i] = new XYIntervalSeries("Ranges");
    }

  }

  public DataSet(int size, int iterations) {

    data = new ArrayList<ArrayList<DataElement>>();
    iterList = new String[iterations];

    minScores = new double[iterations];
    maxScores = new double[iterations];

    minRanges = new double[iterations];
    maxRanges = new double[iterations];

    initFluctSeries(iterations);

    Random generator = new Random(12345678L);

    for (int iter = 0; iter < iterations; iter++) {
      for (int j = 0; j < iter + 1; j++) {
        addFluctItems(iter, j * 1.0, j * 1.0, j * 1.5 + 1.1, -j * 1.0, -(j + 1.0), -j * 1.0);
      }
      iterList[iter] = "iteration " + iter;
      data.add(new ArrayList<DataElement>());
      for (int i = 0; i < 2 * size; i++) {
        int label = (i < size) ? -1 : 1;
        double value = Math.floor(generator.nextGaussian() + (iter * label) / 4.0 + 5);
        this.addDataElement(new DataElement(value, i, label), iter);
      }
    }
    this.preProcessDataset();
  }

  public void addDataElement(DataElement e, int iteration) {
    ArrayList<DataElement> d = data.get(iteration);
    d.add(e);
  }

  public void addFluctItems(int iterNo, double binMin, double binMax, double rangeMin, double rangeMax, double yMin, double yMax) {
    fluctBinSeries[iterNo].add(binMin, binMin, binMax, yMin, yMin, (yMax + yMin) / 2.0);
    fluctRangeSeries[iterNo].add(rangeMin, rangeMin, rangeMax, yMin, yMin, yMax);
  }

  public XYIntervalSeries getFluctBins() {
    return fluctBinSeries[iteration];
  }

  public XYIntervalSeries getFluctRanges() {
    return fluctRangeSeries[iteration];
  }

  public void preProcessDataset() {
    int size = data.size();
    // System.out.printf("data.size = %d%n", size);
    for (int i = 0; i < size; i++) {

      ArrayList<DataElement> d = data.get(i);
      addScoresList(d, i);

      // d should already be sorted

      int bottom = 1 * (d.size() - 1) / 100;
      int top = 99 * (d.size() - 1) / 100;

      minRanges[i] = d.get(bottom).value;
      maxRanges[i] = d.get(top).value;

      maxRanges[i] = Math.max(Math.abs(minRanges[i]), Math.abs(maxRanges[i]));
      minRanges[i] = -maxRanges[i];

      if (minRanges[i] > -1 || maxRanges[i] < 1) {
        minRanges[i] = -1;
        maxRanges[i] = 1;
      }

      if (i > 0 && minRanges[i] > minRanges[i - 1]) minRanges[i] = minRanges[i - 1];
      if (i > 0 && maxRanges[i] < maxRanges[i - 1]) maxRanges[i] = maxRanges[i - 1];

      double min = d.get(0).value;
      double max = d.get(d.size() - 1).value;

      minScores[i] = min;
      maxScores[i] = max;

      maxScores[i] = Math.max(Math.abs(maxScores[i]), Math.abs(minScores[i]));
      minScores[i] = -maxScores[i];

      if (minScores[i] > -1 || maxScores[i] < 1) {
        minScores[i] = -1;
        maxScores[i] = 1;
      }

      if (i > 0 && minScores[i] > minScores[i - 1]) minScores[i] = minScores[i - 1];
      if (i > 0 && maxScores[i] < maxScores[i - 1]) maxScores[i] = maxScores[i - 1];

      // System.out.printf("finished pre-processing %d%n",i);
    }
  }

  /**
   * addScoresList processes a list of scores, computes the true-positive-rate
   * (TPR) and false-positive-rate (FPR) for each element in the list and adds
   * the list to the data
   * 
   * @param scores
   *            void
   */
  public void addScoresList(ArrayList<DataElement> scores, int index) {

    Collections.sort(scores);
    Object[] a = (Object[]) scores.toArray();

    // System.out.printf("index=%d, a.length=%d%n",index,a.length);

    int neg_count = 0;
    int pos_count = 0;

    if (index == 0) {

      for (int i = 0; i < a.length; i++) {
        DataElement e = ((DataElement) a[i]);
        if (e.label == neg_label) neg_count++;
        else if (e.label == pos_label) pos_count++;
      }

      total_neg = neg_count;
      total_pos = pos_count;

      // System.out.printf("total_neg=%d, total_pos=%d%n",total_neg,total_pos);

    }

    neg_count = 0;
    pos_count = 0;
    for (int i = a.length - 1; i >= 0; i--) {
      DataElement e = ((DataElement) a[i]);

      if (e.label == neg_label) neg_count++;
      else if (e.label == pos_label) pos_count++;

      e.truePositives = pos_count;
      e.falsePositives = neg_count;

      // System.out.println(e);
    }
  }

  public double getMin(int iter) {
    return minScores[iter];
  }

  public double getMax(int iter) {
    return maxScores[iter];
  }

  public double getMinRange(int iter) {
    return minRanges[iter];
  }

  public double getMaxRange(int iter) {
    return maxRanges[iter];
  }

  public double[] computeHistogram(int label, int bins) {

    double[] h = new double[bins];

    double step = (maxScores[iteration] - minScores[iteration]) / bins;
    double s = minScores[iteration] + step;
    double prev = total_pos;
    if (label != pos_label) prev = total_neg;
    ArrayList<DataElement> iterData = data.get(iteration);
    for (int i = 0; i < bins; i++) {
      DataElement e = iterData.get(binarySearch(iterData, s));

      // System.out.printf("label=%d, i= %d, s=%f, prev=%f, e=",label,i,s,prev);
      // System.out.println(e);

      // exclude its own label
      double tp = (e.label == pos_label) ? e.truePositives - 1 : e.truePositives;
      double fp = (e.label == neg_label) ? e.falsePositives - 1 : e.falsePositives;

      if (label == pos_label) {
        h[i] = prev - tp;
        prev = tp;
      }
      else {
        h[i] = prev - fp;
        prev = fp;
      }

      s = s + step;
      // System.out.println(prev);
    }
    return h;
  }

  public void printScores(String iterName, double lowerScore, double upperScore) {

    PrintStream out = null;
    try {
      // Create file

      FileOutputStream fstream = new FileOutputStream(outputFilename);
      out = new PrintStream(fstream);
    }
    catch (Exception e) {// Catch exception if any
      System.err.println("Error: " + e.getMessage());
    }

    out.printf("Iteration = %s, low=%f,high=%f%n", iterName, lowerScore, upperScore);
    ArrayList<DataElement> iterData = data.get(iteration);
    int i = binarySearch(iterData, lowerScore);
    DataElement e = iterData.get(i);
    if (e.value < lowerScore && i + 1 < iterData.size()) {
      i++;
      e = iterData.get(i);
    }
    while (e.value <= upperScore) {
      out.printf("%f\t%d\t%d%n", e.value, e.label, e.index);
      i++;
      if (i >= iterData.size()) break;
      e = iterData.get(i);
    }

    try {
      out.close();
    }
    catch (Exception e1) {// Catch exception if any
      System.err.println("Error: " + e1.getMessage());
    }

  }

  private int binarySearch(ArrayList<DataElement> list, double s) {
    int l = list.size();
    if (s < list.get(0).value) return 0;
    if (s > list.get(l - 1).value) return list.size() - 1;
    double l2 = Math.floor(Math.log((double) l) / Math.log(2.0));
    int index = 0;
    int step = (int) Math.pow(2, l2);
    DataElement e = list.get(index);
    while (Math.abs(e.value - s) > 1e-7 && step > 0) {
      if (index + step < l) {
        if (list.get(index + step).value <= s) {
          index = index + step;
        }
      }
      e = list.get(index);
      // System.out.printf("s=%f,e.value=%f,index=%d,step=%d%n",s,e.value,index,step);
      step = step / 2;
    }
    return index;
  }

  public XYSeries generateRoC(int neg_label, int pos_label) {
    XYSeries roc = new XYSeries("ROC");

    Object[] a = (Object[]) data.get(iteration).toArray();

    for (int i = a.length - 1; i >= 0; i--) {
      DataElement e = ((DataElement) a[i]);
      roc.add(e.falsePositives / total_neg, e.truePositives / total_pos);
    }
    return roc;
  }

  public double[] getFPTP(double v) {
    ArrayList<DataElement> iterData = data.get(iteration);
    DataElement e = iterData.get(binarySearch(iterData, v));
    double[] answer = { e.falsePositives / total_neg, e.truePositives / total_pos };
    return answer;
  }

  public double getScoreAtTPThreshold(double threshold) {
    Object[] a = (Object[]) data.get(iteration).toArray();

    for (int i = a.length - 1; i >= 0; i--) {
      DataElement e = ((DataElement) a[i]);
      if (e.truePositives / total_pos > threshold) {
        return e.value;
      }
    }

    return Double.NEGATIVE_INFINITY;
  }

  /**
   * @return the iteration
   */
  public int getIteration() {
    return iteration;
  }

  /**
   * @param iteration
   *            the iteration to set
   */
  public void setIteration(int iteration) {
    this.iteration = iteration;
  }

  public void setOutputFilename(String name) {
    outputFilename = name;
  }

  /**
   * @return the iterList
   */
  public String[] getIterList() {
    return iterList;
  }

  public static void main(String[] args) {
    DataSet test = new DataSet(1000, 3);

    test.generateRoC(-1, 1);
    double[] a = test.getFPTP(5.0);
    System.out.printf("%d: %f; %f%n", test.getIteration(), a[0], a[1]);
    test.setIteration(2);
    test.generateRoC(-1, 1);
    a = test.getFPTP(5.0);
    System.out.printf("%d: %f; %f%n", test.getIteration(), a[0], a[1]);

    double[] hist = test.computeHistogram(1, 30);
    for (int i = 0; i < hist.length; i++)
      System.out.printf("%f, ", hist[i]);
    System.out.println();
    hist = test.computeHistogram(-1, 30);
    for (int i = 0; i < hist.length; i++)
      System.out.printf("%f, ", hist[i]);
    System.out.println();

  }
}
