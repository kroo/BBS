package jboost.controller;

import java.util.concurrent.CountDownLatch;

import jboost.CandidateSplit;
import jboost.util.BaseCountWorker;

/**
 * SplitEvaluatorWorker description
 * 
 * @author Peter Kharchenko
 */
public class SplitEvaluatorWorker extends BaseCountWorker {

  CandidateSplit split;
  double[] losses;
  int pos;

  public SplitEvaluatorWorker(CandidateSplit split, double[] losses, int pos, CountDownLatch count) {
    super(count);
    this.split = split;
    this.losses = losses;
    this.pos = pos;
  }

  /**
   * Evaliate a splitter candidate
   */
  protected void doWork() {
    losses[pos] = split.getLoss();
  }
}
