package jboost.atree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

import jboost.CandidateSplit;
import jboost.ComplexLearner;
import jboost.NotSupportedException;
import jboost.Predictor;
import jboost.WritablePredictor;
import jboost.booster.Bag;
import jboost.booster.Booster;
import jboost.booster.Prediction;
import jboost.booster.RobustBinaryPrediction;
import jboost.booster.RobustBoost;
import jboost.controller.Configuration;
import jboost.controller.ConfigurationException;
import jboost.examples.Example;
import jboost.learner.Splitter;
import jboost.learner.SplitterBuilder;
import jboost.monitor.Monitor;
import jboost.util.ExecutorSinglet;

/**
 * This data structure uses Splitters from the learner package to find the best
 * alternating decision tree from the m_examples. This class can easily output
 * an AlternatingTree once it has learned from all the m_examples.
 */

@SuppressWarnings("unchecked")
public class InstrumentedAlternatingTree extends ComplexLearner {

  /**
   * A list that holds each {@link PredictorNode} in the tree, ordered by the
   * order that the learning process added them in. The first node is the root
   * node.
   */
  private ArrayList m_predictors;

  /**
   * A list that holds all the {@link Splitter} nodes in the tree, ordered by
   * the order that the learning process added them in. XXX: this is not true.
   * what is this list for?
   */
  private ArrayList m_splitters;

  /** The time index at which nodes are added */
  private int m_index;

  /**
   * The list of each {@link SplitterBuilder} used by this tree, stored as a
   * list of
   * 
   * @{link PredictorNodeSB} nodes.
   */
  private ArrayList m_splitterBuilders;

  /**
   * The example mask corresponding to the m_examples that reach each node Each
   * example that has a true value for its mask is an example that reaches that
   * node
   */
  private ArrayList m_masks;

  /** A list of indices of examples to be used by this tree */
  private int[] m_examples;

  /** The booster to be used for learning this alternating tree */
  private Booster m_booster;

  /** The tree type used by this alternating tree */
  private AtreeType m_treeType;

  /**
   * This flag is used to all the tree to emulate BoosTexter functionality
   */
  private boolean m_emulateBoosTexter;

  /**
   * Constructor which allows the controller to specify some SplitterBuilders.
   * 
   * @param sb
   *            The splitter builders for this tree
   * @param b
   *            The m_booster to be used.
   * @param ex
   *            The example indices.
   * @param config
   *            The configuration information.
   */

  public InstrumentedAlternatingTree(Vector sb, Booster b, int[] ex, Configuration config) {

    init(sb, b, ex, config);
    // create root node
    createRoot();
  }

  public InstrumentedAlternatingTree(AlternatingTree tree, Vector splitterbuilders, Booster booster, int[] examples, Configuration config)
  throws InstrumentException,
  NotSupportedException {

    init(splitterbuilders, booster, examples, config);
    createRoot(tree.getRoot());
    instrumentAlternatingTree(tree);
  }

  private void init(Vector splitterbuilders, Booster booster, int[] examples, Configuration config) {
    // Use the number of boosting iterations as the default
    // size for the internal lists used by this tree
    int listSize = config.getInt("numRounds", 200);

    // initialize the data structures used by the tree
    m_predictors = new ArrayList(listSize);
    m_splitters = new ArrayList(listSize);
    m_splitterBuilders = new ArrayList(listSize);
    m_masks = new ArrayList(listSize);
    SplitterBuilder[] initialSplitterBuilders = new SplitterBuilder[splitterbuilders.size()];
    splitterbuilders.toArray(initialSplitterBuilders);
    PredictorNodeSB pnSB = new PredictorNodeSB(0, initialSplitterBuilders);
    m_splitterBuilders.add(pnSB);

    m_booster = booster;
    m_examples = examples;
    m_index = 1;

    try {
      // set configuraiton options
      setAddType(config);
    }
    catch (ConfigurationException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    m_emulateBoosTexter = config.getBool("BoosTexter", false);

    // create initial example mask and place it in the masks list
    boolean[] exampleMask = new boolean[m_examples.length];
    Arrays.fill(exampleMask, true);
    m_masks.add(exampleMask);

  }

  /**
   * Create the root node of this tree. Use the booster to create a Bag of the
   * examples specified when this tree was constructed. Create the initial set
   * of predictions by taking the Bag and passing it back through the booster.
   * The bag used contains the relative weights of the labels in the training
   * set. Update the booster with those predictions and create the initial
   * prediction node.
   */
  private void createRoot() {
    Bag[] initialWeights = new Bag[1];
    Prediction[] tmpPred = null;
    initialWeights[0] = m_booster.newBag(m_examples);
    int[][] tmpEx = new int[1][];
    tmpEx[0] = m_examples;

    // To make it behave like boost texter.
    if (m_emulateBoosTexter) {
      if (Monitor.logLevel > 3) {
        Monitor.log("This has been modified to behave like boostexter: " + "Instrumented ATree Constructor.");
      }
      initialWeights[0].reset();
      tmpPred = m_booster.getPredictions(initialWeights, tmpEx);
      m_booster.update(tmpPred, tmpEx);
      PredictorNode predictorNode = new PredictorNode(tmpPred[0], "R", 0, null, null, 0);
      m_predictors.add(predictorNode);
      return;
    }

    tmpPred = m_booster.getPredictions(initialWeights, tmpEx);
    m_booster.update(tmpPred, tmpEx);

    PredictorNode predictorNode = new PredictorNode(tmpPred[0], "R", 0, null, null, 0);
    m_predictors.add(predictorNode);
  }

  private void createRoot(PredictorNode root) {
    PredictorNode predictorNode = new PredictorNode(root.prediction, "R", 0, null, null, 0);

    m_predictors.add(predictorNode);
  }

  /**
   * Suggest a list of Candidate Splitters
   * 
   * @return
   * @throws NotSupportedException
   */
  public Vector getCandidates() throws NotSupportedException {
    // set initial capacity
    Vector retval = new Vector(m_splitterBuilders.size());
    Bag tmpBag = null;

    if (!m_emulateBoosTexter) {
      // Add a candidate that would just adjust the
      // prediction at the root.
      tmpBag = m_booster.newBag(m_examples);
      double loss = m_booster.getLoss(new Bag[] { tmpBag });
      retval.add(new AtreeCandidateSplit(loss));
    }

    // add all other splitters
    retval.addAll(buildSplitters());

    return retval;
  }

  /**
   * Generate candidates from m_splitterBuilders
   * 
   * @return a vector of candidate splitters
   * @throws NotSupportedException
   */
  private Vector buildSplitters() throws NotSupportedException {
    Executor pe = ExecutorSinglet.getExecutor();
    int childCount;

    // create a synchronization barrier that counts the number
    // of processed splitter builders
    CountDownLatch sbCount = new CountDownLatch(m_splitterBuilders.size());

    Vector splitters = new Vector(m_splitterBuilders.size());
    for (Iterator i = m_splitterBuilders.iterator(); i.hasNext();) {

      // System.out.println("Creating new SplitterBuilderWorker and run it ...
      // ");

      PredictorNodeSB pSB = (PredictorNodeSB) i.next();

      if (m_treeType == AtreeType.ADD_ROOT && pSB.pNode != 0) {
        while (sbCount.getCount() != 0) {
          sbCount.countDown();
        }
        break;
      }

      if (m_treeType == AtreeType.ADD_SINGLES) {
        childCount = ((PredictorNode) m_predictors.get(pSB.pNode)).getSplitterNodeNo();
        if (childCount > 0) {
          sbCount.countDown();
          continue;
        }
      }

      if (m_treeType == AtreeType.ADD_ROOT_OR_SINGLES && pSB.pNode != 0) {
        childCount = ((PredictorNode) m_predictors.get(pSB.pNode)).getSplitterNodeNo();
        if (childCount > 0) {
          sbCount.countDown();
          continue;
        }
      }

      // System.out.println("Create new SplitterBuilderWorker and run it ... ");

      SplitterBuilderWorker sbw = new SplitterBuilderWorker(pSB, splitters, sbCount);

      try {
        pe.execute(sbw);
      }
      catch (RejectedExecutionException ie) {
        System.err.println("exception ocurred while handing off the " + "splitter job to the pool: " + ie.getMessage());
        ie.printStackTrace();
      }
    }

    // System.out.println("Waiting on all threads to finish...");

    // wait on all threads to finish
    try {
      sbCount.await();
    }
    catch (InterruptedException ie) {
      if (sbCount.getCount() != 0) {
        System.err.println("interrupted exception occurred, but the " + "sbCount is " + sbCount.getCount());
      }
    }
    ;

    return splitters;
  }

  /**
   * Update the booster and add the new predictor to the root of the tree
   */
  private void updateRoot() {
    Bag[] bags = new Bag[1];
    int[][] partition = new int[1][];

    partition[0] = m_examples;
    bags[0] = m_booster.newBag(m_examples);
    Prediction[] pred = m_booster.getPredictions(bags, partition);
    m_booster.update(pred, partition);

    ((PredictorNode) m_predictors.get(0)).addToPrediction(pred[0]);
    if (pred == null) {
      System.err.println("Updating root pred is null!");
    }
    lastBasePredictor = new AtreePredictor(pred);
  }

  /**
   * Search the children of the parent node for a matching splitter Return null
   * if no match is found
   * 
   * @param parent
   * @param splitter
   * @return null if no matching splitter is found, otherwise the match
   */
  private SplitterNode findSplitter(PredictorNode parent, Splitter splitter) {
    // Check if this split is already added to this predictor node.
    SplitterNode sameAs = null;
    for (int i = 0; i < parent.getSplitterNodeNo(); i++) {
      sameAs = (SplitterNode) parent.splitterNodes.get(i);
      if (splitter.equals(sameAs.splitter)) {
        return sameAs;
      }
    }
    return null;
  }

  /**
   * @param bags
   * @param parent
   * @param splitter
   * @param parentArray
   * @param predictions
   * @param partition
   */
  private SplitterNode insert(Bag[] bags, PredictorNode parent, Splitter splitter, SplitterBuilder[] parentArray, Prediction[] predictions, int[][] partition) {
    boolean[] examplesMask = null;
    int s = bags.length;
    PredictorNode[] pNode = new PredictorNode[s];
    int[] pInt = new int[s];
    String ID = new String(parent.id);
    ID = ID + "." + parent.getSplitterNodeNo();
    int splitterIndex = m_index++;
    // create new splitter node
    SplitterNode sNode = new SplitterNode(splitter, ID, splitterIndex, pNode, parent);
    // 1) Generate the prediction nodes.
    for (int i = 0; i < s; i++) {
      pNode[i] = new PredictorNode(predictions[i], ID + ":" + i, splitterIndex, null, sNode, i);
      // 1.a) Add the new prediction nodes to the alternating tree list
      pInt[i] = m_predictors.size();
      addPredictorNodeToList(pNode[i]);
      // 1.b) Generate the exampleMasks for the split.
      examplesMask = makeExampleMask(partition[i], m_examples.length);
      m_masks.add(examplesMask);
      // 1.c) Create the splitter builders for this prediction node.
      SplitterBuilder[] childArray = new SplitterBuilder[parentArray.length];
      for (int j = 0; j < parentArray.length; j++) {
        childArray[j] = parentArray[j].spawn(examplesMask, partition[i].length);
      }
      PredictorNodeSB pnSB = new PredictorNodeSB(pInt[i], childArray);
      m_splitterBuilders.add(pnSB);
    }
    // 2) Add new splitter node.
    parent.addSplitterNode(sNode);
    m_splitters.add(pInt);
    return sNode;
  }

  /**
   * add a CandidateSplit
   * 
   * @param the
   *            candidate that has been chosen as a split
   */
  public void addCandidate(CandidateSplit candidate) throws InstrumentException {
    AtreeCandidateSplit acand = null;
    SplitterNode node = null;
    try {
      acand = (AtreeCandidateSplit) candidate;
    }
    catch (ClassCastException e) {
      throw new InstrumentException("Sent non-atree candidate to atree.addCandidate");
    }

    // Check if we should just update the root's prediction.
    if (acand.updateRoot) {
      updateRoot();
    }
    else {
      Bag[] bags = acand.getPartition();
      int[][] partition = acand.getDataSplit();
      Splitter splitter = acand.getSplitter();
      PredictorNode parent = (PredictorNode) m_predictors.get(acand.getPredictorNode());

      Prediction[] predictions = m_booster.getPredictions(bags, partition);
      m_booster.update(predictions, partition);
      if (parent == null) {
        System.err.println("Parent is null!");
      }
      lastBasePredictor = new AtreePredictor(splitter, parent, predictions, m_booster);
      node = findSplitter(parent, splitter);

      // --------- RobustBoost ----------//

      // binary case
      if (m_booster instanceof RobustBoost) {

        for (int i = 0; i < predictions.length; i++) {

          // RobustBoost needs to scale all of the previous
          // hyphothesis by exp(-dt)
          if (predictions[i] instanceof RobustBinaryPrediction) {
            // for every RobustBinaryPrediction added before this one
            // we scale all of them by exp(-dt)
            double dt = ((RobustBinaryPrediction) predictions[i]).getDt();
            double exp_negative_dt = Math.exp(-dt);

            for (int j = 0; j < i; j++) {
              predictions[j].scale(exp_negative_dt);
            }

            // for each prediction before this one
            for (int nodeidx = 0; nodeidx < m_predictors.size(); nodeidx++) {
              PredictorNode cpn = (PredictorNode) m_predictors.get(nodeidx);
              cpn.prediction.scale(exp_negative_dt);
            }

          }
          else {
            throw new RuntimeException("RobustBinaryPrediction is expected. This should never happen!");
          }
        }
      }

      if (node != null && predictions.length > 0) {
        for (int i = 0; i < node.predictorNodes.length; i++) {
          node.predictorNodes[i].addToPrediction(predictions[i]);
        }
      }
      else {
        SplitterBuilder[] parentArray = ((PredictorNodeSB) m_splitterBuilders.get(acand.getPredictorNode())).SB;
        node = insert(bags, parent, splitter, parentArray, predictions, partition);
      }
    }

    /*
     * System.out.println("Adding Candidate: " + candidate);
     * System.out.println("Node being added:" ); System.out.println("" + node );
     * System.out.println("m_predictors:"); for (int i=0; i<m_predictors.size();
     * i++) { System.out.println("i: "+ i + m_predictors.get(i)); }
     * System.out.println("m_predictors.index:"); for (int i=0; i<m_predictors.size();
     * i++) { System.out.println("i: "+ i +
     * ((PredictorNode)(m_predictors.get(i))).getIndex()); }
     * System.out.println("m_splitters:\n"); for (int i=0; i<m_splitters.size();
     * i++) { System.out.println("i: "+i+m_splitters.get(i)); }
     * System.out.println("m_splitterBuilders:\n"); for (int i=0; i<m_splitterBuilders.size();
     * i++) { System.out.println("i: "+i+m_splitterBuilders.get(i)); }
     */
  }

  /**
   * add a CandidateSplit using the Predictions instead of generating them
   * 
   * @param the
   *            candidate that has been chosen as a split
   * @param predictions
   *            from this split
   */
  public void addCandidate(CandidateSplit candidate, Prediction[] predictions) throws InstrumentException {

    AtreeCandidateSplit acand = null;
    try {
      acand = (AtreeCandidateSplit) candidate;
    }
    catch (ClassCastException e) {
      throw new InstrumentException("Sent non-atree candidate to atree.addCandidate");
    }

    // Check if we should just update the root's prediction.
    // Does this ever happen?
    if (acand.updateRoot) {
      updateRoot();
    }
    else {
      Bag[] bags = acand.getPartition();
      int[][] partition = acand.getDataSplit();
      Splitter splitter = acand.getSplitter();
      PredictorNode parent = (PredictorNode) m_predictors.get(acand.getPredictorNode());

      // m_booster.update(predictions, partition);

      if (parent == null) {
        System.err.println("Adding candidate and the parent is null!");
      }
      lastBasePredictor = new AtreePredictor(splitter, parent, predictions, m_booster);
      SplitterNode node = findSplitter(parent, splitter);
      if (node != null) {
        for (int i = 0; i < node.predictorNodes.length; i++) {
          node.predictorNodes[i].addToPrediction(predictions[i]);
        }
      }
      else {
        SplitterBuilder[] parentArray = ((PredictorNodeSB) m_splitterBuilders.get(acand.getPredictorNode())).SB;
        insert(bags, parent, splitter, parentArray, predictions, partition);
      }
    }
  }

  /**
   * Instrument this tree by using the AlternatingTree. This method is like an
   * automated version of the learn() method used by the Controller. Instead of
   * evaluating the set returned by getCandidates(), this tree will be built
   * using the splitters that are already used in the AlternatingTree. Using
   * each splitter, we create the CandidateSplit and add it to the tree.
   * 
   * @param tree
   */
  public void instrumentAlternatingTree(AlternatingTree tree) throws InstrumentException, NotSupportedException {
    // get the nodes for the tree
    // the defaults for these lists should come from the tree
    // TODO:decide if we really need to return the predictors with the splitters
    ArrayList predictors = new ArrayList(25);
    ArrayList splitters = new ArrayList(25);

    tree.getNodes(predictors, splitters);
    CandidateSplit split = null;
    // create each candidate split and add it to this tree
    // by finding adding all the splitters that match the current predictor node
    for (int i = 0; i < m_predictors.size(); i++) {
      PredictorNode prediction = (PredictorNode) m_predictors.get(i);
      String predictionID = prediction.getID();

      for (int j = 0; j < splitters.size(); j++) {
        SplitterNode splitter = (SplitterNode) splitters.get(j);
        // get the splitter ID without the final 2 characters
        String splitterID = splitter.getID().substring(0, splitter.getID().length() - 2);
        // compare this splitter ID to the prediction ID
        // if they match, then add this splitter to the tree with this predictor
        if (splitterID.equals(predictionID)) {
          PredictorNodeSB splitterBuilder = (PredictorNodeSB) m_splitterBuilders.get(i);
          // find the splitter builder with the same type as this splitter
          // build a CandidateSplit and add it to this tree
          for (int k = 0; k < splitterBuilder.SB.length; k++) {
            // check that the Splitter and SplitterBuilder have the same type,
            // and that they have the same AttributeDescription
            // if
            // (splitter.splitter.getType().equals(splitterBuilder.SB[k].getType()))
            // {
            if (splitterBuilder.SB[k].canBuild(splitter.splitter)) {
              split = splitterBuilder.SB[k].build(splitter.splitter);
              // find the predictors that have this splitter as their root
              Prediction[] predictions = new Prediction[splitter.getPredictorNodes().length];
              for (int n = 0; n < predictions.length; n++) {
                predictions[n] = splitter.getPredictorNodes()[n].prediction;
              }

              // add split to tree
              AtreeCandidateSplit added = new AtreeCandidateSplit(i, split);
              addCandidate(added, predictions);
              // remove the split from the list of splitters
              // decrement the index to make sure we do not skip a splitter in
              // the list
              splitters.remove(j--);
              // break out of this loop and move to the next splitter
              break;
            }
          }
        }
      }
    }
  }

  /**
   * Get the combined classifier. Simply returns a pointer to the root predictor
   * nodes, so that the returned tree will share nodes with this Instrumented
   * Tree.
   * 
   * @return An alternating decision tree created by this object
   */
  public WritablePredictor getCombinedPredictor() {
    PredictorNode p = (PredictorNode) m_predictors.get(0);
    AlternatingTree retval = new AlternatingTree(p);
    return (retval);
  }

  /** Produces a string describing this tree. */
  public String toString() {
    return (((PredictorNode) m_predictors.get(0)).toString());
  }

  /**
   * returns the last base predictor that was added using addCandidate.
   */
  public Predictor getLastBasePredictor() {
    return lastBasePredictor;
  }

  public boolean boosterIsFinished() {

    if (m_booster instanceof RobustBoost) {
      RobustBoost b = (RobustBoost) m_booster;
      return b.isFinished();
    }

    double EPS = 1e-50;
    double w = m_booster.getTotalWeight();
    if ((w < EPS) || Double.isNaN(w)) {
      System.out.println("JBoost boosting process is completed.");
      System.out.println("\tThe boosting has finished early.");
      System.out.println("\tThis is a result of too little weight being placed on examples");
      System.out.println("\t(which will cause an underflow error).");
      System.out.println("\tThis is not necessarily a bad thing.  Look at the margin curve to find out more.");
      return true;
    }

    return false;
  }

  // ---------------------------------- Un-Implemented
  // ----------------------------------------//
  public void addExample(Example e) {
  }

  public void finalizeData() {
  }

  public void resetData() {
  }

  // ---------------------------------- Protected Members
  // -------------------------------------//
  /**
   * Add a predictorNode to this list.
   */
  protected void addPredictorNodeToList(PredictorNode pn) {
    m_predictors.add(pn);
  }

  /**
   * Add a splitterNode to this list.
   */
  protected void addSplitterNode(SplitterNode sn) {
    m_splitters.add(sn);
  }

  protected Booster getBooster() {
    return (m_booster);
  }

  protected int getIndex() {
    return (m_index);
  }

  protected void setIndex(int ind) {
    m_index = ind;
  }

  public ArrayList getMasks() {
    return m_masks;
  }

  private void setAddType(Configuration config) throws ConfigurationException {

    String aT = config.getString("ATreeType", "ADD_ALL");

    if (aT.equals("ADD_ALL")) {
      m_treeType = AtreeType.ADD_ALL;
    }
    else if (aT.equals("ADD_ROOT")) {
      m_treeType = AtreeType.ADD_ROOT;
    }
    else if (aT.equals("ADD_SINGLES")) {
      m_treeType = AtreeType.ADD_SINGLES;
    }
    else if (aT.equals("ADD_ROOT_OR_SINGLES")) {
      m_treeType = AtreeType.ADD_ROOT_OR_SINGLES;
    }
    else {
      throw new ConfigurationException("Unknown value: " + aT + " for Atree_AddType");
    }

    if (Monitor.logLevel > 3) Monitor.log("Add Type is " + aT + " " + m_treeType);
  }

  /** the last base predictor added to the tree */
  private Predictor lastBasePredictor = null;

  /** Construct an example mask - this should be in some generic place */
  boolean[] makeExampleMask(int[] exampleList, int s) {
    boolean[] em = new boolean[s];
    Arrays.fill(em, false);
    int length = exampleList.length;
    for (int i = 0; i < length; i++)
      em[exampleList[i]] = true;
    return (em);
  }

}

/** A description of a candidate splitter */
class AtreeCandidateSplit extends CandidateSplit {

  boolean updateRoot;
  private int pNode;

  /** the predictor node in the atree which owns the builder */
  public int getPredictorNode() {
    return (pNode);
  }

  /** Constructor to convert from a {@link CandidateSplit} */
  public AtreeCandidateSplit(int pn, CandidateSplit b) {
    pNode = pn;
    builder = b.getBuilder();
    splitter = b.getSplitter();
    partition = b.getPartition();
    loss = b.getLoss();
    updateRoot = false;
  }

  /** Constructor to specify that only the root prediction should be updated. */
  public AtreeCandidateSplit(double loss) {
    updateRoot = true;
    this.loss = loss;
    pNode = 0;
    builder = null;
    splitter = null;
    partition = null;
  }
}

/**
 * Contains a SplitterBuilder and the number of the PredictorNode to which it
 * belongs.
 */
class PredictorNodeSB {

  public int pNode;
  public SplitterBuilder[] SB;

  public PredictorNodeSB(int p, SplitterBuilder[] sb) {
    SB = sb;
    pNode = p;
  }
}
