package jboost.learner;

import java.util.Arrays;

import jboost.CandidateSplit;
import jboost.NotSupportedException;
import jboost.booster.AbstractBooster;
import jboost.booster.Bag;
import jboost.booster.Booster;
import jboost.examples.Attribute;
import jboost.examples.AttributeDescription;
import jboost.examples.Example;
import jboost.examples.SetAttribute;
import jboost.examples.WordTable;

/**
 * This is where the splits that are based on <i>SetAttribute</i>s are searched
 * for.
 * 
 * @author Yoram Singer
 * @version $Header:
 *          /cvsroot/jboost/jboost/src/jboost/learner/SetSplitterBuilder.java,v
 *          1.1.1.1 2007/05/16 04:06:02 aarvey Exp $
 */
class SetSplitterBuilder extends SplitterBuilder {

  /**
   * Constructs an empty Root SetSplitterBuilder
   * 
   * @param index -
   *            the index of the relevant attribute
   * @param booster -
   *            the booster
   */
  public static SplitterBuilder newSplitterBuilder(int index, AbstractBooster booster, AttributeDescription[] ad) {
    return new SetSplitterBuilder(index, booster, true, ad);
  }

  public static SplitterBuilder newSplitterBuilder(int i, Booster b, boolean a, AttributeDescription[] ad) {
    return new SetSplitterBuilder(i, b, a, ad);
  }

  /**
   * default constructor XXX: is this an acceptable initial state? Do any
   * clients use this ctor?
   */
  public SetSplitterBuilder() {
    init(-1, null, false, null, null, -1, null, false, true);
  }

  /**
   * The constructor for the root splitter-builder
   * 
   * @param index -
   *            the index of the relevant attribute
   * @param booster -
   *            the booster that is to be used by this builder
   * @param abstain -
   *            optional parameter telling the root to abstain
   */
  protected SetSplitterBuilder(int index, Booster booster, boolean abstain, AttributeDescription[] ad) {
    init(index, booster, abstain, ad, null, -1, new SparseMatrix(), true, false);
  }

  protected SetSplitterBuilder(int index, Booster booster, AttributeDescription[] ad) {
    init(index, booster, true, ad, null, -1, new SparseMatrix(), true, false);
  }

  /**
   * Basic constructor
   */
  private SetSplitterBuilder(int index, Booster b, boolean abstain, boolean[] em, int noEl, SparseMatrix SM, AttributeDescription[] ad) {
    init(index, b, abstain, ad, em, noEl, SM, false, false);
  }

  /**
   * A private virtual ctor that is used by all the public ctors of this class
   * 
   * @param index
   *            the attribute index of this SplitterBuilder
   * @param booster
   *            the booster used by this SplitterBuilder
   * @param abstain
   *            if true, then this builder can abstain
   * @param attributes
   *            the AttributeDescriptions used by this SplitterBuilder
   * @param masks
   *            a boolean mask of the examples that reach this SplitterBuilder
   * @param count
   *            the number of elements that reach this SplitterBuilder
   * @param tokens
   *            the SparseMatrix used by this SplitterBuilder
   * @param root
   *            if true, then this SplitterBuilder is used by the root of the
   *            tree
   * @param finalized
   *            if true, then this SplitterBuilder has been finalized
   */
  private void init(int index, Booster booster, boolean abstain, AttributeDescription[] attributes, boolean[] masks, int count, SparseMatrix tokens,
                    boolean root, boolean finalized) {

    attributeIndex = index;
    this.booster = booster;
    this.abstain = abstain;
    desc = attributes;
    examplesMask = masks;
    noOfElements = count;
    SM = tokens;
    isRoot = root;
    isFinalized = finalized;
    m_type = SplitterType.SET_SPLITTER;
  }

  /**
   * Construct a new splitter builder basd on the current one and a subset of
   * the data defined through examplesMask (em).
   * 
   * @param em -
   *            an array holding the exampleMask for the subset
   * @param count -
   *            the number of elements in the subset
   */
  public SplitterBuilder spawn(boolean[] em, int count) {

    /**
     * OLD CODE --> atree now intersects examplesMask ** ** boolean[] tMask =
     * new boolean[em.length]; ** int n; ** for (int i = n = 0; i < em.length;
     * i++) { ** tMask[i] = em[i] & examplesMask[i]; ** if (tMask[i]) ** n++; ** } ** **
     * return new ** SetSplitterBuilder(attributeIndex, this.booster,
     * this.abstain, ** tMask, n); **
     */

    return new SetSplitterBuilder(attributeIndex, this.booster, this.abstain, em, count, SM, desc);
  }

  /**
   * Build a CandidateSplit from a Splitter. Uses the information in the
   * Splitter to select the token to use for splitting the bag of examples that
   * reach this SplitterBuilder
   * 
   * @param s
   *            the Splitter to use. Will be cast to a SetSplitter
   * @return candidate split that can be added to an InstrumentedAlternatingTree
   */
  public CandidateSplit build(Splitter s) throws NotSupportedException {
    double loss;
    Bag b0, b1, tb;
    Bag[] bestBag;
    int[] T;

    SetSplitter split = (SetSplitter) s;

    tb = booster.newBag(); // always contains all
    // examples reaching this node
    b0 = booster.newBag();
    b1 = booster.newBag();

    // Create default Bag if not abstaining when token is not present
    if (!abstain) {
      for (int i = 0; i < examplesMask.length; i++) {
        if (examplesMask[i]) {
          tb.addExample(i);
        }
      }
      bestBag = new Bag[2];
      bestBag[0] = booster.newBag();
      bestBag[1] = booster.newBag();
    }
    else {
      bestBag = new Bag[1];
      bestBag[0] = booster.newBag();
    }

    try {
      T = SM.getColumn(split.getToken());
    }
    catch (Exception e) {
      String error = "build() : " + e.getMessage() + " for attribute " + attributeIndex;
      throw new NotSupportedException("SetSplitterBuilder", error);
    }

    b0.reset();
    for (int j = 0; j < T.length; j++) {
      if (examplesMask[T[j]]) {
        b0.addExample(T[j]);
      }
    }
    if (abstain) {
      loss = booster.getLoss(new Bag[] { b0 });
      bestBag[0].copyBag(b0);
    }
    else {
      b1.copyBag(tb);
      b1.subtractBag(b0);
      loss = booster.getLoss(new Bag[] { b0, b1 });
      bestBag[0].copyBag(b0);
      bestBag[1].copyBag(b1);
    }
    return (new CandidateSplit(this, split, bestBag, loss));
  }

  /**
   * The builder == Weak-Learner This is an inefficient version that checks each
   * token by creating a new set of bags each call to build.
   */
  public CandidateSplit build() throws NotSupportedException {
    double loss, minLoss;
    int i, l, bestTok;
    Bag b0, b1, tb;
    Bag[] bestBag;
    int[] T;

    tb = booster.newBag(); // always contains all
    // examples reaching this node
    b0 = booster.newBag();
    b1 = booster.newBag();

    // Create default Bag if not abstaining when token is not present
    if (!abstain) {
      for (i = 0; i < examplesMask.length; i++) {
        if (examplesMask[i]) {
          tb.addExample(i);
        }
      }
      bestBag = new Bag[2];
      bestBag[0] = booster.newBag();
      bestBag[1] = booster.newBag();
    }
    else {
      bestBag = new Bag[1];
      bestBag[0] = booster.newBag();
    }

    /* Create and accumulate bag and then evaluate loss for each token */
    minLoss = Double.MAX_VALUE;
    bestTok = -1;

    for (i = 0, l = SM.numCols(); i < l; i++) {
      try {
        T = SM.getColumn(i);
      }
      catch (Exception e) {
        String s = "build() : " + e.getMessage() + " for attribute " + attributeIndex;
        throw new NotSupportedException("SetSplitterBuilder", s);
      }

      if (T != null) {
        b0.reset();
        for (int j = 0; j < T.length; j++) {
          if (examplesMask[T[j]]) {
            b0.addExample(T[j]);
          }
        }

        if (abstain) {
          loss = booster.getLoss(new Bag[] { b0 });
        }
        else {
          b1.copyBag(tb);
          b1.subtractBag(b0);
          loss = booster.getLoss(new Bag[] { b0, b1 });
        }

        // if(Monitor.logLevel>3) Monitor.log("Token " + i + " Score " + loss);

        if (loss < minLoss) {
          minLoss = loss;
          bestTok = i;
          if (abstain) bestBag[0].copyBag(b0);
          else {
            bestBag[0].copyBag(b0);
            bestBag[1].copyBag(b1);
          }
        }

      }

    }

    Splitter s = new SetSplitter(attributeIndex, bestTok, abstain, desc[0]);

    return (new CandidateSplit(this, s, bestBag, minLoss));
  }

  /**
   * Figures out the split of the data for a given splitter. The idea here is to
   * be able to use a splitter without retaining all of the examples.
   * 
   * @param -
   *            The splitter on which to base the split
   * @returns - The partition of the data or null if the splitter is not
   *          compatible.
   */
  public int[][] split(Splitter sp) {
    int[] T = null;

    if (attributeIndex != sp.getIndex()) return null;

    int token = ((SetSplitter) sp).getToken();

    try {
      T = SM.getColumn(token);
    }
    catch (Exception e) {
      String s = "SPLIT for attribute " + attributeIndex + " could not be performed: internal structures were not finalized";
      System.err.println(s);
      System.exit(-1);
    }

    int i, j, l;

    /** build the split for the examples in which the token appears * */
    for (i = l = 0; i < T.length; i++)
      if (examplesMask[T[i]]) l++;
    int[] A0 = new int[l];
    for (i = l = 0; i < T.length; i++)
      if (examplesMask[T[i]]) A0[l++] = T[i];

    /** build the split for the examples from which the token is absent * */
    if (abstain) return (new int[][] { A0 });
    else {
      int[] A1 = new int[noOfElements - l];
      for (i = j = l = 0; i < T.length; i++) {
        int ex = T[i];
        for (; j < ex; j++)
          if (examplesMask[j]) A1[l++] = j;
        j++;
      }
      for (; j < examplesMask.length; j++)
        if (examplesMask[j]) A1[l++] = j;

      return (new int[][] { A0, A1 });
    }
  }

  /**
   * Add a single example to the internal data structure.s
   * 
   * @param index -
   *            the index of the example in the dataset
   * @param example -
   *            the example
   */
  public void addExample(int index, Example example) throws IncompAttException {
    // ADDED
    if (!isRoot || isFinalized) throw new RuntimeException("Trying to addExample() to non-root or finalized SplitterBuilder");
    // END_ADDED
    SetAttribute a = null;
    Attribute t = example.getAttribute(attributeIndex);

    // Check running index
    if (index != checkIndex) {
      System.err.println("Examples not in consecutive order (SetSplitterBuilder)");
      System.exit(-1);
    }
    checkIndex++;

    // check that attribute is of the correct class
    try {
      a = (SetAttribute) t; // try downcasting
    }
    catch (ClassCastException e) {
      throw new IncompAttException(index, attributeIndex, "SetAttribute", t.getClass());
    }

    if (a.isDefined()) {
      int A[] = a.getList();
      // SM.printit("Inside: ", A);
      SM.addRow(a.getList());
    }
    else {
      int A[] = new int[0];
      SM.addRow(A);
    }
  }

  public void finalizeData() {
    if (!isRoot || isFinalized) throw new RuntimeException("Trying to finalizeData() to non-root or finalized SplitterBuilder");
    WordTable.globalTable.setFrozen(true); // freeze the global word table

    SM.finalizeMatrix();
    examplesMask = new boolean[SM.numRows()];
    Arrays.fill(examplesMask, true);
    noOfElements = examplesMask.length;
    isFinalized = true;
  }

  /** describe as a string for dubugging printout. */
  public String toString() {
    String s = "SetSplitterBuilder for attribute " + attributeIndex;
    s += SM.toString();
    return s;
  }

  // -------------------------- Private Members
  // -----------------------------------//

  /** The index of the attribute on which this bulder works */
  int attributeIndex;

  /**
   * The sparse matrix of examples x tokens. One copy of this is generated by
   * the root splitter builder and is pointed to by all of its decendents.
   */
  SparseMatrix SM;

  /** Flag determining the prediction value when a token is not present */
  boolean abstain;

  /** running-index for checking that examples are provided sequentially. */
  private int checkIndex = 0;

  // --------------------------- Test Code
  // ----------------------------------------//

  /** A main for testing this class */
  /*
   * static public void main(String[] argv) { try{ int[] labels = {0, 0, 1, 1,
   * 0}; int[][] Tokens = { {1, 3, 7}, {0, 2}, {4, 6}, {1, 4, 5}, {3} };
   * AbstractBooster booster = AbstractBooster.getInstance(); SetSplitterBuilder
   * sb = new SetSplitterBuilder(0, booster, false,new AttributeDescription[]
   * {null}); Example x; Attribute[] attArray = new Attribute[1]; Label l;
   * if(Monitor.logLevel>3) Monitor.log("Input: \t index \t value \t label");
   * for(int i = 0; i < labels.length; i++) { l = new Label(labels[i]);
   * attArray[0] = new SetAttribute(Tokens[i]); // if(Monitor.logLevel>3)
   * Monitor.log(attArray[0]); x = new Example(attArray, l);
   * if(Monitor.logLevel>3) Monitor.log(" \t "+i+"\t "+"\t "+labels[i]); try{
   * sb.addExample(i, x); booster.addExample(i, l); } catch(IncompAttException
   * e) { if(Monitor.logLevel>3) Monitor.log(e.getMessage()); } }
   * if(Monitor.logLevel>3) Monitor.log(""); booster.finalizeData();
   * if(Monitor.logLevel>3) Monitor.log(booster); sb.finalizeData();
   * if(Monitor.logLevel>3) Monitor.log("\n\nOutput:"); if(Monitor.logLevel>3)
   * Monitor.log(sb); if(Monitor.logLevel>3) Monitor.log("checking building");
   * CandidateSplit h = sb.build(); if(Monitor.logLevel>3) Monitor.log( "\n===\n
   * checking detection of incompatible attribute"); sb = new
   * SetSplitterBuilder(0, booster, true,new AttributeDescription[] {null}); l =
   * new Label(0); attArray[0] = new DiscreteAttribute(0); x = new
   * Example(attArray, l); try { sb.addExample((int) 0, x); }
   * catch(IncompAttException e) { if(Monitor.logLevel>3)
   * Monitor.log(e.getMessage()); } } catch(Exception e) {
   * if(Monitor.logLevel>3) Monitor.log(e.getMessage()); e.printStackTrace(); }
   * finally { if(Monitor.logLevel>3) Monitor.log("finished testing
   * InequalitySplitterBuilder"); } }
   */
}
