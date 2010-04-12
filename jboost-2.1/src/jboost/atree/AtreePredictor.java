package jboost.atree;

import jboost.Predictor;
import jboost.booster.Bag;
import jboost.booster.Booster;
import jboost.booster.Prediction;
import jboost.examples.Instance;
import jboost.learner.IncompAttException;
import jboost.learner.Splitter;

/**
 * The base predictor that is returned by
 * InstrumentedAlternatingTree.getLastBasePredictor().
 */
class AtreePredictor implements Predictor {

  private Prediction pred[]; // predictions associated with this split
  private boolean isConstant; // true if rule was added at the root
  private Splitter splitter; // the splitter that was added
  private PredictorNode pNode; // the predictor node at which it was added
  private Prediction zeroPred; // a zero prediction

  /** the constructor for a constant predictor */
  AtreePredictor(Prediction[] pred) {
    this.pred = pred;
    isConstant = true;
  }

  /** the constructor for a non-constant predictor */
  AtreePredictor(Splitter s, PredictorNode p, Prediction[] pred, Booster b) {
    splitter = s;

    if (p == null) {
      System.err.println("Predictor node given to constructor is null");
      System.err.println(s);
    }
    pNode = p;
    this.pred = pred;
    isConstant = false;
    zeroPred = b.getPredictions(new Bag[] { b.newBag() }, new int[0][])[0];
  }

  /**
   * Check to see if we get to this node. If we reach this node, return the
   * prediction for it.
   */
  public Prediction predict(Instance x) throws IncompAttException {
    // if this is the root, we have no parent
    if (pNode == null) {
      return predict(x, 0);
    }
    else {
      return predict(x, pNode.index);
    }
  }

  /**
   * Check to see if we get to this node and this node is iteration iter. If we
   * reach this node, return the prediction for it.
   */
  public Prediction predict(Instance x, int iter) throws IncompAttException {
    if (isConstant) return pred[0];

    if (pNode.index != iter) return zeroPred;

    // If we don't reach this node, then return zero
    for (PredictorNode p = pNode; p.parent != null; p = p.parent.parent) {
      if (p.parent.splitter.eval(x) != p.branchIndex) return zeroPred;
    }
    int v = splitter.eval(x);
    return (v < 0 ? zeroPred : pred[v]);
  }

}
