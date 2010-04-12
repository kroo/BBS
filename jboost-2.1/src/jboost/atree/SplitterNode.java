package jboost.atree;

import java.io.Serializable;

import jboost.booster.Prediction;
import jboost.examples.Instance;
import jboost.learner.IncompAttException;
import jboost.learner.Splitter;

/**
 * SplitterNode.java This is a splitter node in the alternating tree, it
 * implements a binary predicate branches the tree according to the value of
 * this predicate. Links to child {@link PredictorNode}s,
 * 
 * @author Nigel Duffy
 * @version $Header: /cvsroot/jboost/jboost/src/jboost/atree/SplitterNode.java,v
 *          1.2 2007/10/02 02:28:06 aarvey Exp $
 */
class SplitterNode implements Serializable {

  /**
   * Calculate the prediction of the subtree starting at this node. Depends on
   * the fact that all splits are binary.
   */
  protected Prediction predict(Instance instance) throws IncompAttException {
    if (splitter == null) throw (new RuntimeException("Splitter node: " + id + " has no splitter."));
    int which_branch = splitter.eval(instance);
    if (which_branch < 0) return (null);
    if (predictorNodes == null) throw (new RuntimeException("Splitter node: " + id + " has no predictor nodes."));
    return (predictorNodes[which_branch].predict(instance));
  }

  /**
   * Return the prediction node that comes next for this instance
   */
  protected PredictorNode predictNode(Instance instance) throws IncompAttException {
    if (splitter == null) throw (new RuntimeException("Splitter node: " + id + " has no splitter."));
    int which_branch = splitter.eval(instance);
    if (which_branch < 0) return (null);
    if (predictorNodes == null) throw (new RuntimeException("Splitter node: " + id + " has no predictor nodes."));
    return (predictorNodes[which_branch]);
  }

  /** Generate a textual explanation of the prediction */
  protected String explain(Instance instance) throws IncompAttException {
    if (splitter == null) throw (new RuntimeException("Splitter node: " + id + " has no splitter."));
    String s = new String();
    s += "[" + id + "] " + splitter;
    int which_branch = splitter.eval(instance);
    if (which_branch < 0) return (s + " Abstains\n");
    else if (which_branch == 0) s += " True";
    else if (which_branch == 1) s += " False";
    else if (which_branch > 1) s += " Eval=" + which_branch;

    if (predictorNodes == null) throw (new RuntimeException("Splitter node: " + id + " has no predictor nodes."));
    return (s + predictorNodes[which_branch].explain(instance));
  }

  protected SplitterNode() {
    parent = null;
    splitter = null;
    id = null;
    index = -1;
    predictorNodes = null;
  }

  /** Constructor */
  public SplitterNode(Splitter sp, String ID, int ind, PredictorNode[] p, PredictorNode parent) {
    this.parent = parent;
    splitter = sp;
    id = ID;
    index = ind;
    predictorNodes = p;
  }

  /** output self in human-readable format */
  public String toString() {
    String s = new String();
    s += index + "\t[" + id + "] Splitter = ";
    if (splitter != null) s += splitter + "\n";
    else s += "no splitter\n";
    if (predictorNodes == null) return (s);
    for (int i = 0; i < predictorNodes.length; i++) {
      s += predictorNodes[i];
    }
    return (s);
  }

  /**
   * Write this object into an output stream
   * 
   * @param stream
   *            to write out to
   */
  /*
   * private void writeObject(ObjectOutputStream stream) throws IOException {
   * stream.defaultWriteObject(); stream.writeInt(predictorNodes.length); for
   * (int i=0; i < predictorNodes.length; i++) {
   * stream.writeObject(predictorNodes[i]); } }
   */

  /**
   * Read an object from the input stream
   * 
   * @param stream
   *            to read from
   */
  /*
   * private void readObject(ObjectInputStream stream) throws IOException,
   * ClassNotFoundException { stream.defaultReadObject(); int count=
   * stream.readInt(); predictorNodes= new PredictorNode[count]; for (int i=0; i <
   * count; i++) { predictorNodes[i]= (PredictorNode) stream.readObject();
   * predictorNodes[i].parent= this; } }
   */

  /** Converts this splitter node to Java. */
  public String toJava(String fname) {
    int i = 0;
    String retval = "\tprivate Prediction " + fname + "(Instance ins){\n";
    retval += "\t\tPrediction retval=null;\n";
    retval += "\t\tint sp=" + fname + "_split(ins);\n";
    retval += "\t\tswitch(sp) {\n";
    for (i = 0; i < predictorNodes.length; i++) {
      retval += "\t\t\tcase " + i + ": retval=" + fname + "_" + i + "(ins);\n";
      retval += "\t\t\t\tbreak;\n";
    }
    retval += "\t\t\tdefault: retval=null;\n";
    retval += "\t\t\t\tbreak;\n";
    retval += "\t\t}\n";
    retval += "\t\treturn(retval);\n";
    retval += "\t}\n\n";
    for (i = 0; i < predictorNodes.length; i++)
      retval += predictorNodes[i].toJava(fname + "_" + i);
    retval += splitter.toJava(fname + "_split");
    return (retval);
  }

  /**
   * The predictor nodes that are the children of this splitternode. In
   * predicting, the algorithm follows the node that corresponds to the output
   * of the splitter
   */
  protected PredictorNode[] predictorNodes;

  /**
   * Return the list of PredictorNodes that are children of this SplitterNode
   * 
   * @return list of PredictorNodes
   */
  public PredictorNode[] getPredictorNodes() {
    return predictorNodes;
  }

  /**
   * Return the ID of this SplitterNode
   * 
   * @return id of this node
   */
  public String getID() {
    return id;
  }

  /**
   * Return the index of this SplitterNode
   * 
   * @return index of this node
   */
  public int getIndex() {
    return index;
  }

  /** the predictornode parent of this splitternode */
  PredictorNode parent;

  /** A textual identifier, has the format <parentPredictorNodeID>.index */
  protected String id;

  /**
   * An index signifying the iteration in which this node was added to the tree
   */
  protected int index;
  /** The splitter rule that is associated with this node */
  protected Splitter splitter;

}
