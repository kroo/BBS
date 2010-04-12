package jboost.atree;

import java.util.Vector;
import java.util.concurrent.CountDownLatch;

import jboost.CandidateSplit;
import jboost.NotSupportedException;
import jboost.monitor.Monitor;
import jboost.util.BaseCountWorker;

/**
 * SplitterBuilderWorker is a Runnable worker class that performs splitter
 * building.
 * 
 * @author Peter Kharchenko
 */
public class SplitterBuilderWorker extends BaseCountWorker {

  PredictorNodeSB pSB;
  Vector splitters;

  public SplitterBuilderWorker(PredictorNodeSB pSB, Vector splitters, CountDownLatch count) {
    super(count);
    this.pSB = pSB;
    this.splitters = splitters;
  }

  /**
   * Build a splitter using a single splitter buildier
   */
  protected void doWork() {
    CandidateSplit split;

    double trivLoss;
    long start;
    long stop;

    // create bag containing all m_examples reaching this node
    // tmpBag = m_booster.newBag(makeIndices((boolean [])
    // m_masks.get(pSB.pNode)));
    // compute loss for trivial split
    // trivLoss = m_booster.getLoss(new Bag[] {tmpBag});
    // TODO: need to fix so that splits worse than trivial are not
    // added. In the meantime, allow all splits.
    trivLoss = Double.MAX_VALUE;
    start = System.currentTimeMillis();
    int j = 0;
    for (j = 0; j < pSB.SB.length; j++) {
      try {
        split = pSB.SB[j].build();

        // only add candidates with loss better than trivial split
        // TODO: figure out what to do if no splits better
        // than trivial
        if (split != null && split.getLoss() < trivLoss) {
          splitters.add(new AtreeCandidateSplit(pSB.pNode, split));
        }
      }
      catch (NotSupportedException nse) {
        System.err.println(nse.getMessage());
        nse.printStackTrace();
      }
    }
    stop = System.currentTimeMillis();
    if (Monitor.logLevel > 3) {
      Monitor.log("It took an average of " + (stop - start) / (j * 1000.0) + " seconds to build " + j + " splitterbuilders.");
    }
  }
}
