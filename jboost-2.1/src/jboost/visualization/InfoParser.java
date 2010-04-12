package jboost.visualization;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;

public class InfoParser {

  protected String[] testFiles, trainFiles, infoFiles;
  protected String[] iterNoList;
  protected boolean useCV, hasIndex, carryOver;
  protected int maxNumIndices, maxNumIter, numClasses;
  protected int switchLabel = -1;

  protected boolean isRobustBoost, isAdaBoost, isLogLossBoost, confRated;
  protected double epsilon, theta, sigma_f, rho;
  protected double[] averageTime;
  protected double[] stdTime;

  public InfoParser(String[] testFiles, String[] trainFiles, String[] infoFiles, boolean carryOver) {
    this.testFiles = testFiles;
    this.trainFiles = trainFiles;
    this.infoFiles = infoFiles;
    this.carryOver = carryOver;
  }

  protected void getHasIndexAndNumClasses() throws IOException {

    BufferedReader br;
    String aline;

    if (testFiles.length > 1) useCV = true;
    else useCV = false;

    post("Cross validation = " + useCV);

    // find out the number of classes
    br = new BufferedReader(new FileReader(testFiles[0]));
    aline = br.readLine();
    while (aline != null) {
      String[] tokens = aline.split(":");
      String firstToken = tokens[0];
      if (!firstToken.startsWith("iter")) {

        if (tokens.length < 6) hasIndex = false;
        else hasIndex = true;

        String secondLastToken = tokens[tokens.length - 2].trim();
        String[] labels = secondLastToken.split(",");
        if (labels.length < 2) numClasses = 2;
        else numClasses = labels.length;
        break;
      }
      aline = br.readLine();
    }
    br.close();

    post("Indexed Info = " + hasIndex);
    post("Number of classes = " + numClasses);

    if (numClasses > 2) {
      post("This score visualizer does not support multi-class");
      System.exit(-1);
    }

  }

  protected void getMaxNumIter() throws IOException {

    BufferedReader br;
    String aline;

    // find out the maximum iteration
    ArrayList<String> maxIterList = null;
    for (int i = 0; i < infoFiles.length; i++) {
      ArrayList<String> currentIterList = new ArrayList<String>();
      br = new BufferedReader(new FileReader(infoFiles[i]));
      aline = br.readLine();
      while (aline != null) {
        if (aline.endsWith("#")) {
          String[] tokens = aline.split("\t");
          String firstToken = tokens[0];
          // parse iteration number
          int iterNo = Integer.parseInt(firstToken);
          currentIterList.add("Iteration " + iterNo);
        }
        aline = br.readLine();
      }
      br.close();
      if (maxIterList == null) {
        maxIterList = currentIterList;
      }
      else {
        // carry over
        if (carryOver) {
          if (currentIterList.size() > maxIterList.size()) {
            maxIterList = currentIterList;
          }
        }
        else {
          if (currentIterList.size() < maxIterList.size()) {
            maxIterList = currentIterList;
          }
        }
      }
    }
    maxNumIter = maxIterList.size();
    iterNoList = maxIterList.toArray(new String[maxNumIter]);

    if (maxNumIter == 0) {
      post("Error: number of iterations = 0");
      post("Please change logging option (-a) of JBoost.");
      throw new RuntimeException();
    }

    post("Maximum number of iterations = " + maxNumIter);
  }

  protected void getMaxNumIndices() throws IOException {

    BufferedReader br;
    String aline;

    // find out the maximum index
    maxNumIndices = -1;
    for (int i = 0; i < testFiles.length; i++) {
      br = new BufferedReader(new FileReader(testFiles[i]));
      aline = br.readLine();
      while (aline != null) {
        String[] tokens = aline.split(":");
        String firstToken = tokens[0];
        if (!firstToken.startsWith("iter")) {
          String secondToken = tokens[1].trim();
          int index;
          if (hasIndex) index = Integer.parseInt(secondToken);
          else index = Integer.parseInt(firstToken.trim());
          if (index > maxNumIndices) maxNumIndices = index;
        }
        aline = br.readLine();
      }
      br.close();
    }
    maxNumIndices++;
    post("Maximum number of indices = " + maxNumIndices);
  }

  protected void getBoosterInfo() throws IOException {

    BufferedReader br;
    String aline;

    // get epsilon, theta, sigma_f, average t and its standard deviation

    isRobustBoost = false;
    isAdaBoost = false;
    isLogLossBoost = false;
    confRated = true;
    theta = 0;
    sigma_f = 0.1;
    epsilon = 0.1;

    if (infoFiles != null && infoFiles.length > 0) {

      br = new BufferedReader(new FileReader(infoFiles[0]));
      aline = br.readLine();

      while (aline != null) {

        if (aline.startsWith("booster_type")) {
          String[] tokens = aline.split(" = ");
          if (tokens.length > 1) {
            String booster = tokens[1];
            if (booster.endsWith("RobustBoost")) {
              isRobustBoost = true;
            }
            if (booster.endsWith("AdaBoost")) {
              isAdaBoost = true;
              post("AdaBoost found");
            }
            if (booster.endsWith("LogLossBoost")) {
              isLogLossBoost = true;
              post("LogLossBoost found");
            }
          }
        }
        else if (aline.startsWith("rb_theta")) {
          String[] tokens = aline.split(" = ");
          if (tokens.length > 1) {
            String str = tokens[1];
            theta = Double.parseDouble(str);
          }
        }
        else if (aline.startsWith("rb_sigma_f")) {
          String[] tokens = aline.split(" = ");
          if (tokens.length > 1) {
            String str = tokens[1];
            sigma_f = Double.parseDouble(str);
          }
        }
        else if (aline.startsWith("rb_epsilon")) {
          String[] tokens = aline.split(" = ");
          if (tokens.length > 1) {
            String str = tokens[1];
            epsilon = Double.parseDouble(str);
          }
        }
        else if (aline.startsWith("rb_conf_rated")) {
          String[] tokens = aline.split(" = ");
          if (tokens.length > 1) {
            if (tokens[1].compareTo("false") == 0) {
              confRated = false;
            }
          }
        }
        aline = br.readLine();
      }

      br.close();
    }

    if (isRobustBoost) {

      post("RobustBoost detected");
      post("theta = " + theta);
      post("eps = " + epsilon);
      post("sigma_f = " + sigma_f);
      post("conf_rated = " + confRated);

      double[][] time = new double[maxNumIter][infoFiles.length];
      for (int i = 0; i < infoFiles.length; i++) {
        post(">Procesing " + infoFiles[i]);
        br = new BufferedReader(new FileReader(infoFiles[i]));
        int iterIdx = 0;
        aline = br.readLine();
        while (aline != null) {
          if (aline.endsWith("#")) {
            String[] tokens = aline.split("\t");
            String t = tokens[4];
            time[iterIdx++][i] = Double.parseDouble(t);
            if (iterIdx == maxNumIter) break;
          }
          aline = br.readLine();
        }
        br.close();

        // carry over
        while (iterIdx > 0 && iterIdx < maxNumIter) {
          time[iterIdx][i] = time[iterIdx - 1][i];
          iterIdx++;
        }

      }

      averageTime = new double[maxNumIter];
      stdTime = new double[maxNumIter];

      NumberFormat f = new DecimalFormat("0.000");
      for (int i = 0; i < maxNumIter; i++) {
        averageTime[i] = getMean(time[i]);
        stdTime[i] = getStd(time[i], averageTime[i]);
        // post("time("+i+")=" + averageTime[i] + "+/-" + stdTime[i]);

        // update iterNoList
        iterNoList[i] = iterNoList[i] + " [T=" + f.format(averageTime[i]) + "+/-" + f.format(stdTime[i]) + "]";
      }

    }
  }

  protected DataSet createDataSet() throws IOException {

    DataSet dataset;
    ExampleData exData;
    ArrayList<IterationData> iterationData = new ArrayList<IterationData>();

    dataset = new DataSet(iterNoList);
    exData = new ExampleData(maxNumIndices, maxNumIter, trainFiles.length);

    int currentIter;
    String aline;
    BufferedReader br;

    // now get test scores

    post("Reading test scores ...");

    for (int i = 0; i < testFiles.length; i++) {

      post(">Processing " + testFiles[i]);

      currentIter = -1;
      br = new BufferedReader(new FileReader(testFiles[i]));
      aline = br.readLine();
      while (aline != null) {
        String[] tokens = aline.split(":");
        String firstToken = tokens[0];
        if (firstToken.startsWith("iter")) {
          currentIter++;
          iterationData.clear();
          if (currentIter >= maxNumIter) break;
        }
        else {
          String secondLastToken = tokens[tokens.length - 2].trim();
          String thirdLastToken = tokens[tokens.length - 3].trim();
          String secondToken = tokens[1].trim();

          // get rid of '+'
          if (secondLastToken.startsWith("+")) secondLastToken = secondLastToken.substring(1);

          int label = Integer.parseInt(secondLastToken) * switchLabel;
          double score = Double.parseDouble(thirdLastToken) * switchLabel;
          int index;

          if (hasIndex) index = Integer.parseInt(secondToken);
          else index = Integer.parseInt(firstToken.trim());

          DataElement e = new DataElement(score, index, label);
          dataset.addDataElement(e, currentIter);

          exData.addTestScore(index, currentIter, score);
          iterationData.add(new IterationData(score, index, label));
        }
        aline = br.readLine();
      }

      br.close();
      currentIter++;

      // carry over
      while (currentIter > 0 && currentIter < maxNumIter) {
        for (int j = 0; j < iterationData.size(); j++) {
          IterationData id = iterationData.get(j);
          double score = id.score;
          int index = id.index;
          int label = id.label;
          DataElement e = new DataElement(score, index, label);
          dataset.addDataElement(e, currentIter);
          exData.addTestScore(index, currentIter, score);
        }
        currentIter++;
      }
    }

    post("Preprocessing test scores ...");
    dataset.preProcessDataset();

    if (useCV && hasIndex) {
      post("Reading train scores ...");

      for (int i = 0; i < trainFiles.length; i++) {

        post(">Processing " + trainFiles[i]);

        currentIter = -1;
        br = new BufferedReader(new FileReader(trainFiles[i]));
        aline = br.readLine();
        while (aline != null) {
          String[] tokens = aline.split(":");
          String firstToken = tokens[0];
          if (firstToken.startsWith("iter")) {
            currentIter++;
            iterationData.clear();
            if (currentIter >= maxNumIter) break;
          }
          else {
            String scoreToken = tokens[tokens.length - 5].trim();
            String secondToken = tokens[1].trim();

            double score = Double.parseDouble(scoreToken) * switchLabel;

            int index;

            if (hasIndex) index = Integer.parseInt(secondToken);
            else index = Integer.parseInt(firstToken.trim());

            exData.addTrainScore(index, currentIter, score);
            iterationData.add(new IterationData(score, index, 0));
          }
          aline = br.readLine();
        }
        br.close();
        currentIter++;

        // carry over
        while (currentIter > 0 && currentIter < maxNumIter) {
          for (int j = 0; j < iterationData.size(); j++) {
            IterationData id = iterationData.get(j);
            double score = id.score;
            int index = id.index;
            exData.addTrainScore(index, currentIter, score);
          }
          currentIter++;
        }

      }

      post("Calculating Fluctuation ... ");
      dataset.preProcessDataset();

      for (int iter = 0; iter < maxNumIter; iter++) {

        double iterMin = dataset.getMin(iter);
        double iterMax = dataset.getMax(iter);

        int fluctBins = 20;
        double percentage = 0.05;
        int barHeight = 6;
        double binWidth = (iterMax - iterMin) / fluctBins;

        double y = 0;
        int testScoreCount;

        // for each bin
        for (int b = 0; b < fluctBins; b++) {

          ArrayList<Double> trainScores = new ArrayList<Double>();

          double lower = iterMin + b * binWidth;
          double upper = iterMin + (b + 1) * binWidth;
          testScoreCount = 0;

          // for each index
          for (int i = 0; i < maxNumIndices; i++) {

            // get test score
            double score = exData.getTestScore(i, iter);

            // if the test score is in this bin
            if (score != Double.NaN && score >= lower && score <= upper) {
              testScoreCount++;
              // get train scores
              double[] train = exData.getTrainScore(i, iter);
              if (train != null) {
                int num_train = exData.getNumTrainScore(i, iter);
                for (int j = 0; j < num_train; j++)
                  trainScores.add(new Double(train[j]));
              }
            }
          }

          double yMin = y - barHeight;
          double yMax = y;
          y = y - 1.1 * barHeight;

          if (testScoreCount > 10 && trainScores.size() > 10) {
            Collections.sort(trainScores);

            int topIndex = Math.min((int) (trainScores.size() * (1 - percentage)), trainScores.size() - 1);
            int bottomIndex = Math.max((int) (trainScores.size() * percentage), 0);

            double top = (trainScores.get(topIndex)).doubleValue();
            double bottom = (trainScores.get(bottomIndex)).doubleValue();

            dataset.addFluctItems(iter, lower, upper, bottom, top, yMin, yMax);

          }
        }
      }
    }

    post("Cleaning up ...");
    iterationData.clear();
    iterationData = null;
    exData = null;

    return dataset;
  }

  private void post(String s) {
    System.out.println(s);
  }

  private static double getMean(double[] v) {
    double sum = 0;
    for (int i = 0; i < v.length; i++) {
      sum += v[i];
    }
    return sum / v.length;
  }

  private static double getStd(double[] v, double mean) {
    double s = 0;
    for (int i = 0; i < v.length; i++) {
      s += Math.pow(v[i] - mean, 2);
    }
    return Math.sqrt(s / v.length);
  }

  private class ExampleData {

    int maxNumIter;
    int numTrainFolds;
    double[][] testScores;
    double[][][] trainScores;
    int[][] trainScoresIdx;

    public ExampleData(int maxNumIndices, int maxNumIter, int numTrainFolds) {
      this.maxNumIter = maxNumIter;
      this.numTrainFolds = numTrainFolds;
      trainScores = new double[maxNumIter][maxNumIndices][numTrainFolds];
      trainScoresIdx = new int[maxNumIter][maxNumIndices];
      testScores = new double[maxNumIter][maxNumIndices];
    }

    public void addTestScore(int index, int iter, double testScore) {
      testScores[iter][index] = testScore;
    }

    public double getTestScore(int index, int iter) {
      return testScores[iter][index];
    }

    public void addTrainScore(int index, int iter, double trainScore) {
      trainScores[iter][index][trainScoresIdx[iter][index]] = trainScore;
      trainScoresIdx[iter][index]++;
    }

    public int getNumTrainScore(int index, int iter) {
      return trainScoresIdx[iter][index];
    }

    public double[] getTrainScore(int index, int iter) {
      return trainScores[iter][index];
    }

  }

  private class IterationData {

    public double score;
    public int index, label;

    public IterationData(double s, int i, int l) {
      score = s;
      index = i;
      label = l;
    }
  }

}
