package jboost.atree;

import java.io.Serializable;
import java.util.Vector;

import jboost.booster.Prediction;
import jboost.examples.Instance;
import jboost.learner.IncompAttException;

/**
 * Holds a contribution to the prediction sum, links to the child
 * {@link SplitterNode}s,
 * 
 * @author Nigel Duffy
 * @version $Header:
 *          /cvsroot/jboost/jboost/src/jboost/atree/PredictorNode.java,v 1.3
 *          2008/04/07 17:09:06 dhsu Exp $
 */
class PredictorNode implements Serializable {

  /** the prediction value associated with this node. */
  protected Prediction prediction;

  /**
   * A textual identifier, has the format <parentSplitterNodeID>:index The id of
   * the root predictor node is "R".
   */
  protected String id;

  /**
   * An index signifying the iteration in which this node was added to the tree.
   */
  protected int index;

  /**
   * The parent splitter node (or null if root)
   */
  SplitterNode parent;

  /**
   * The branch index (as a value returned by Splitter) of the parent split that
   * leads to this predictor node.
   */
  int branchIndex;

  /** constructor */
  public PredictorNode(Prediction p, String ID, int ind, Vector sp, SplitterNode parent, int branchIndex) throws Error {
    prediction = p;
    id = ID;
    index = ind;
    this.parent = parent;
    this.branchIndex = branchIndex;
    if (sp == null) splitterNodes = new Vector();
    else splitterNodes = sp;
  }

  /** calculate the prediction of the subtree starting at this node. */
  protected Prediction predict(Instance instance) throws IncompAttException {
    Prediction retval = (Prediction) prediction.clone();
    Prediction tmp = null;
    if (splitterNodes == null) return (retval);
    for (int i = 0; i < splitterNodes.size(); i++) {
      tmp = ((SplitterNode) splitterNodes.elementAt(i)).predict(instance);
      if (tmp != null) retval.add(tmp);
    }
    return (retval);
  }

  /**
   * Calculate the prediction of the subtree starting at this node in order of
   * iteration. This is important for normalized predictors such as NormalBoost.
   * This function can only be called on the root node.
   * 
   * @author Aaron Arvey
   */

  protected Prediction orderPredict(Instance instance, int numIterations) throws IncompAttException, RuntimeException {
    // Are we the root node?
    if (parent != null || id != "R") {
      throw new RuntimeException("Cannot perform ordered prediction on a node other then the root");
    }

    Prediction retval = (Prediction) prediction.clone();
    Prediction tmp = null;
    for (int i = 0; i < numIterations; i++) {
      PredictorNode p = findPrediction(instance, i, this);
      if (p == null) { // we could not get to this iteration, so we continue to
        // the next iteration
        continue;
        // throw new Exception("Cannot find prediction for iteration " + i);
      }
      retval.add(p.prediction);
    }

    /*
     * if (numIterations > 3 && numIterations < 5) { System.out.println("Doing
     * ordered prediction"); } if (numIterations > 3 && numIterations < 5) { try {
     * Thread.currentThread().sleep(9999); } catch (Exception e) { // do nothing } }
     */

    return retval;
  }

  private PredictorNode findPrediction(Instance instance, int iter, PredictorNode pn) {
    if (pn.splitterNodes == null && pn.index != iter) return null;
    if (pn.splitterNodes == null && pn.index == iter) return pn;

    // Search for the SplitterNode/PredictorNode of interest
    for (int i = 0; i < pn.splitterNodes.size(); i++) {
      if (((SplitterNode) pn.splitterNodes.elementAt(i)).getIndex() == iter) {
        return ((SplitterNode) pn.splitterNodes.elementAt(i)).predictNode(instance);
      }
    }

    // We couldn't find the node of interest, so continue with search
    PredictorNode tmp = null;
    for (int i = 0; i < pn.splitterNodes.size(); i++) {
      tmp = ((SplitterNode) pn.splitterNodes.elementAt(i)).predictNode(instance);

      if (tmp != null) tmp = findPrediction(instance, iter, tmp);

      if (tmp == null) {
        // The node is not down there or this instance does
        // not fulfill the predicate. Search down the other
        // paths
      }
      else {
        return tmp;
      }
    }

    return null;
  }

  /** Generate a textual explanation of the prediction */
  public String explain(Instance instance) throws IncompAttException {
    // describe own contribution
    String s = "\tP=" + prediction.shortText() + "\n";
    // describe contributions of child SplitterNodes
    if (splitterNodes == null) return s;
    for (int i = 0; i < splitterNodes.size(); i++)
      s += ((SplitterNode) splitterNodes.elementAt(i)).explain(instance);
    return s;
  }

  public void addSplitterNode(SplitterNode sn) {
    splitterNodes.add(sn);
  }

  /** output self in human-readable format. */
  public String toString() {
    String s = new String();
    s += index + "\t[" + id + "] prediction = ";
    s += prediction + "\n";
    if (splitterNodes == null) return (s);
    for (int i = 0; i < splitterNodes.size(); i++) {
      s += (SplitterNode) splitterNodes.get(i);
    }
    return (s);
  }

  /** Converts this predictor node to Java. */
  public String toJava(String fname) {
    int i = 0;
    String retval = "\tprivate Prediction " + fname + "(Instance ins){\n";
    retval += "\t\tPrediction retval=null;\n";
    SplitterNode sn = null;
    if (splitterNodes.size() > 0) retval += "\t\tretval=" + fname + "_" + i + "(ins);\n";
    for (i = 1; i < splitterNodes.size(); i++)
      retval += "\t\tretval.add(" + fname + "_" + i + "(ins));\n";
    retval += "\t\treturn(retval);\n";
    retval += "\t}\n\n";
    for (i = 0; i < splitterNodes.size(); i++) {
      sn = (SplitterNode) splitterNodes.get(i);
      retval += sn.toJava(fname + "_" + i);
    }
    return (retval);
  }

  /** Add a prediction to its prediction value */
  public void addToPrediction(Prediction p) {
    prediction.add(p);
  }

  /**
   * The splitter nodes that are the children of this node. In predicting, the
   * algorithm follows all the children, and sums their predictions
   */
  protected Vector splitterNodes;

  public Vector getSplitterNodes() {
    return splitterNodes;
  }

  /**
   * Returns the number of child splitternodes.
   */
  int getSplitterNodeNo() {
    return (splitterNodes.size());
  }

  /**
   * Return the ID of this PredictorNode
   * 
   * @return id of this node
   */
  public String getID() {
    return id;
  }

  /**
   * Return the index of this PredictorNode
   * 
   * @return index of this node
   */
  public int getIndex() {
    return index;
  }

}
