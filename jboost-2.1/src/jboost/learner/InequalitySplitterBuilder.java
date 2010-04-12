package jboost.learner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jboost.CandidateSplit;
import jboost.NotSupportedException;
import jboost.booster.Bag;
import jboost.booster.Booster;
import jboost.examples.Attribute;
import jboost.examples.AttributeDescription;
import jboost.examples.Example;
import jboost.examples.IntegerAttribute;
import jboost.examples.Label;
import jboost.examples.RealAttribute;

/**
 * a builder of splitters that partition data according to whether or not a
 * particular numerical attribute is larger than some value
 * 
 * @author Yoav Freund
 * @version $Header:
 *          /cvsroot/jboost/jboost/src/jboost/learner/InequalitySplitterBuilder.java,v
 *          1.4 2008/11/17 22:31:13 aarvey Exp $
 */
class InequalitySplitterBuilder extends SplitterBuilder {

  /** Default constructor */
  InequalitySplitterBuilder() {
    this.attributeIndex = -1;
    m_type = SplitterType.INEQUALITY_SPLITTER;
  }

  /**
   * Constructor for a non-root non-sortedListOwner InequalitySplitterBuilder
   * gets as parameters the set of pointers to its parent's data structures
   */
  InequalitySplitterBuilder(int attributeIndex, double[] indexedValues, int[] sortedIndices, boolean[] potentialSplits, boolean[] examplesMask,
                            int noOfElements, Booster booster, AttributeDescription[] ad) {

    this.attributeIndex = attributeIndex;
    this.sortedListOwner = false;
    this.indexedValues = indexedValues;
    this.sortedIndices = sortedIndices;
    this.potentialSplits = potentialSplits;
    super.examplesMask = examplesMask;
    super.noOfElements = noOfElements;
    super.booster = booster;
    isFinalized = true;
    isRoot = false;
    desc = ad;
    m_type = SplitterType.INEQUALITY_SPLITTER;
  }

  /** describe as a string for debugging printout */
  public String toString() {
    String s = "InequalitySplitterBuilder for attribute " + attributeIndex + "\n\tindex\tvalue\tpotentialSplit\n";
    if (sortedIndices == null) s += "sortedIndices is empty";
    else for (int i = 0; i < sortedIndices.length; i++) {
      int index = sortedIndices[i];
      s += i + "\t" + index + "\t" + indexedValues[index] + "\t" + potentialSplits[i] + "\n";
    }
    return s;
  }

  /**
   * The builder = weak learner
   * 
   * @throws NotSupportedException
   * @return split
   */
  public CandidateSplit build() throws NotSupportedException {
    int splitIndex; // the index of the best split point
    // in sortedIndices. The split is between
    // the indexed element and the one before it.
    Bag[] bag = new Bag[] { booster.newBag(), booster.newBag() };
    int[] localIndices;
    boolean[] localSplits;
    if (sortedListOwner) { // data is ready from construction
      localIndices = sortedIndices;
      localSplits = potentialSplits;
    }
    else { // generate data for findBestSplit on the fly
      int i = 0; // index in the temporary arrays
      boolean split = false;
      for (int j = 0; j < sortedIndices.length; j++)
        if (examplesMask[sortedIndices[j]]) i++;
      localIndices = new int[i];
      localSplits = new boolean[i];
      i = 0;
      for (int j = 0; j < sortedIndices.length; j++) {
        split = split | potentialSplits[j]; // detect potential splits
        if (examplesMask[sortedIndices[j]]) {
          localIndices[i] = sortedIndices[j];
          localSplits[i] = split;
          split = false;
          i++;
        }
      }
    }

    splitIndex = booster.findBestSplit(bag[0], bag[1], localIndices, localSplits);
    double threshold = (splitIndex == 0 ? -Double.MAX_VALUE : 0.5 * (indexedValues[localIndices[splitIndex]] + indexedValues[localIndices[splitIndex - 1]]));

    Splitter s = new InequalitySplitter(attributeIndex, threshold, desc[0]);
    return new CandidateSplit(this, s, bag, booster.getLoss(bag));
  }

  /**
   * Take all the indices that reach this split create two bags, one full and
   * one empty find the index whose value matches the value of the splitter
   * separate remove that index from the full bag and put it in the empty pass
   * the bag and splitter on as a new CandidateSplit
   */
  public CandidateSplit build(Splitter s) throws NotSupportedException {
    InequalitySplitter splitter = (InequalitySplitter) s;
    int splitIndex = -1;
    int count = 0;
    int[] indices;
    if (sortedListOwner) {
      indices = sortedIndices;
      for (int j = 0; j < indices.length; j++) {
        if (indices[j] == splitter.getIndex()) {
          splitIndex = j;
        }
      }
    }
    else {
      for (int j = 0; j < sortedIndices.length; j++) {
        if (examplesMask[sortedIndices[j]]) {
          count++;
        }
      }

      indices = new int[count];
      for (int i = 0, j = 0; j < sortedIndices.length; j++) {
        if (examplesMask[sortedIndices[j]]) {
          indices[i] = sortedIndices[j];
          if (indices[i] == splitter.getIndex()) {
            splitIndex = i;
          }
          i++;
        }
      }
    }

    Bag[] bag = new Bag[] { booster.newBag(), booster.newBag(indices) };
    if (splitIndex >= 0) {
      bag[0].addExample(indices[splitIndex]);
      bag[1].subtractExample(indices[splitIndex]);
    }

    /*
     * if (sortedListOwner) { // data is ready from construction localIndices=
     * sortedIndices; localSplits= potentialSplits; } else { // generate data
     * for findBestSplit on the fly int index= 0; // index in the temporary
     * arrays for (int j= 0; j < sortedIndices.length; j++) { if
     * (examplesMask[sortedIndices[j]]) { index++; } } localIndices= new
     * int[index]; index= 0; for (int j= 0; j < sortedIndices.length; j++) { if
     * (examplesMask[sortedIndices[j]]) { localIndices[index]= sortedIndices[j];
     * index++; } } } for (int i=0; i < localIndices.length; i++) { if
     * (indexedValues[localIndices[i]] >= splitter.getThreshold()) { // make
     * bags based on this split } } splitIndex= booster.findBestSplit(bag[0],
     * bag[1], localIndices, localSplits);
     */
    return new CandidateSplit(this, splitter, bag, booster.getLoss(bag));
  }

  /**
   * construct a new SplitterBuilder based on this one and some subset of the
   * data.
   * 
   * @param em
   *            an array holding the exampleMask for the subset
   * @param count
   *            the no of elements in the subset.
   */
  public SplitterBuilder spawn(boolean[] em, int count) {
    return new InequalitySplitterBuilder(attributeIndex, indexedValues, sortedIndices, potentialSplits, em, count, booster, desc);
  }

  /**
   * Figures out the split of the data for a given splitter. The idea here is to
   * be able to use a splitter without retaining all of the examples.
   * 
   * @param The
   *            splitter on which to base the split
   * @returns The partition of the data or null if the splitter is not
   *          compatible.
   */
  public int[][] split(Splitter sp) {
    if (attributeIndex != sp.getIndex()) return (null);
    double threshold = ((InequalitySplitter) sp).getThreshold();
    int[][] result = new int[2][];
    if (sortedListOwner) {
      int cutIndex; // locate the place where the
      // list should be cut into two
      for (cutIndex = 0; cutIndex < sortedIndices.length; cutIndex++) {
        if (indexedValues[sortedIndices[cutIndex]] > threshold) break;
      }
      result[0] = new int[cutIndex]; // create first list
      for (int j = 0; j < cutIndex; j++)
        result[0][j] = sortedIndices[j];
      result[1] = new int[sortedIndices.length - cutIndex];
      // create second list
      for (int j = cutIndex; j < sortedIndices.length; j++)
        result[1][j - cutIndex] = sortedIndices[j];
    }
    else {
      int[] l = new int[noOfElements];
      // temporary storage for list of elements
      int j = 0;
      int cutIndex = -1;
      int index = 0;
      for (int i = 0; i < sortedIndices.length; i++) {
        index = sortedIndices[i];
        if (examplesMask[index]) {
          if ((cutIndex == -1) && (indexedValues[index] > threshold)) cutIndex = j;
          l[j++] = index;
        }
      }

      if (cutIndex == -1) {
        System.out.println(this);
        result[0] = new int[0];
        int noGreater = j;
        result[1] = new int[noGreater];
        for (j = 0; j < noGreater; j++) {
          result[1][j] = l[j];
        }
      }
      else {
        result[0] = new int[cutIndex]; // create first list
        int noGreater = j;
        result[1] = new int[noGreater - cutIndex]; // create second list
        for (j = 0; j < cutIndex; j++)
          result[0][j] = l[j];
        for (; j < noGreater; j++)
          result[1][j - cutIndex] = l[j];
      }
    }
    return result;
  }

  /**
   * The constructor for the root splitterbuilder
   * 
   * @param index
   *            the index of the relevant attribute
   * @param booster
   *            the booster that is to be used by this builder
   */
  InequalitySplitterBuilder(int index, Booster booster, AttributeDescription[] ad) {
    isRoot = true;
    isFinalized = false;
    attributeIndex = index;
    this.booster = booster;
    tempList = new ArrayList();
    largestIndex = -1;
    desc = ad;
    m_type = SplitterType.INEQUALITY_SPLITTER;
  }

  /**
   * Add a single example to the internal data structure
   * 
   * @param index
   *            the index of the example in the dataset
   * @param example
   *            the example
   */
  public void addExample(int index, Example example) throws IncompAttException {
    if (!isRoot || isFinalized) throw new RuntimeException("Attempt to addExample() to non-root or finalized SplitterBuilder");
    RealAttribute a = null;
    IntegerAttribute b = null;
    Attribute t = example.getAttribute(attributeIndex);
    Label l = example.getLabel();
    // check that attribute is of the correct class
    try {
      if (t instanceof RealAttribute) {
        a = (RealAttribute) t;
        if (a.isDefined() && (l != null)) {
          IVL ivl = new IVL(index, a.getValue(), l);
          tempList.add(ivl);
        }
      }
      else if (t instanceof IntegerAttribute) {
        b = (IntegerAttribute) t;
        if (b.isDefined() && (l != null)) {
          IVL ivl = new IVL(index, b.getValue(), l);
          tempList.add(ivl);
        }
      }
      else {
        throw new IncompAttException(index, attributeIndex, "IntegerAttribute or RealAttribute", t.getClass());
      }
      // check if attribute and label are defined
    }
    catch (ClassCastException e) {
      throw new IncompAttException(index, attributeIndex, "RealAttribute", t.getClass());
    }
    largestIndex = (largestIndex > index) ? largestIndex : index;
    // remember the largest index seen
    // if(Monitor.logLevel>3) Monitor.log("In RootInequalitySplitterBuilder:
    // a="+a+" t="+t
    // +" example="+example);
    // if(Monitor.logLevel>3) Monitor.log("at end of
    // addExample("+index+")\n"+tempList);
  }

  /** Initializes attributeValues and sortedIndices */
  public void finalizeData() {
    if (!isRoot || isFinalized) throw new RuntimeException("Attempt to finalizeData() to non-root or finalized SplitterBuilder");
    // if(Monitor.logLevel>3) Monitor.log(tempList);
    Collections.sort(tempList);
    // if(Monitor.logLevel>3) Monitor.log(tempList);
    sortedListOwner = true;
    indexedValues = new double[largestIndex + 1];
    int size = tempList.size();
    sortedIndices = new int[size];
    potentialSplits = new boolean[size];
    if (size > 0) {
      IVL element = (IVL) tempList.get(0);
      double prevValue = element.value;
      Label prevLabel = element.label;
      boolean labelChange = false;
      boolean valueChange = false;
      int firstItemNewValue = 0;
      for (int i = 0; i < size; i++) {
        IVL iv = (IVL) tempList.get(i);
        indexedValues[iv.index] = iv.value;
        sortedIndices[i] = iv.index;
        // a potential split can occur when the value chages and
        // the m_labels associated with the previous and the current value
        // are different
        potentialSplits[i] = false; // default is false
        labelChange |= !iv.label.equals(prevLabel);
        valueChange |= (iv.value != prevValue);
        // if(Monitor.logLevel>3) Monitor.log("start "+i+
        // "\tprevLabel="+prevLabel+
        // "\tiv.label="+iv.label+
        // "\tlabelChange="+labelChange+
        // "\tprevValue="+prevValue+
        // "\tiv.value="+iv.value+
        // "\tvalueChange="+valueChange);
        if (valueChange) {
          if (labelChange) { // label has changed since last split point
            potentialSplits[i] = true;
            labelChange = false;
          }
          else { // label has not changed, but might change
            // before next value change, in which case
            // we want to come back here and mark it as
            // a potential split point
            firstItemNewValue = i;
          }
          valueChange = false;
        }
        else {
          if (labelChange) { // label changed without the value changing -
            // mark previous potential split point,
            // keep labelChange on for the next value change
            potentialSplits[firstItemNewValue] = true;
          }
        }
        prevValue = iv.value;
        prevLabel = iv.label;
        // if(Monitor.logLevel>3) Monitor.log("end "+i+
        // "\tlabelChange="+labelChange+
        // "\tvalueChange="+valueChange+
        // "\tfirstItemNewValue="+firstItemNewValue);
      }
    }
    tempList.clear(); // free the memory
    isFinalized = true;
  }

  // ----------------------------- Protected Members
  // -------------------------------------//
  /** The index of the attribute on which this builder works */
  protected int attributeIndex;
  /**
   * The indices of (a superset of) the examples that reach this node, sorted by
   * attribute value.
   */
  protected int[] sortedIndices;
  /**
   * true if all of the elements in sortedExamples are relevant to this builder
   */
  protected boolean sortedListOwner;
  /**
   * The value of the attribute for all of the examples. The values are kept in
   * their original order, the entries that corresponds to undefined attribute
   * values are not initialized. One copy of this is generated by the root
   * splitterBuilder and is pointed to by all of its descendents
   */
  protected double[] indexedValues;
  /**
   * a flag for each element in sortedIndices, indicating whether or not the
   * att. value of this example is different from the value of the previous
   * (unmasked) example in sortedIndices
   */
  protected boolean[] potentialSplits;

  // ----------------------------- Private Members
  // -------------------------------------//
  /** an internal class defining the elements of {@link tempList} */
  class IVL implements Comparable {

    IVL(int i, double v, Label l) {
      index = i;
      value = v;
      label = l;
    }

    IVL(int i, int v, Label l) {
      index = i;
      value = v;
      label = l;
    }

    public int compareTo(Object o) { // defined so that we can use
      // "Collections.sort"
      double value2 = ((IVL) o).value;
      if (value < value2) {
        return -1;
      }
      else if (value > value2) {
        return 1;
      }
      else {
        return 0;
      }
    }

    public String toString() {
      return ("index=" + index + ", value=" + value + ", label=" + label + "\n");
    }

    int index;
    double value;
    Label label;
  }

  /**
   * a temporary storage for the values and indices of the attributes. This
   * storage is freed up when the builder is finalized.
   */
  private List tempList;
  /** the largest example index seen */
  private int largestIndex;
  // -------------------------------- Test Stuff
  // ---------------------------------------//
  /** A main for testing this class */
  /*
   * static public void main(String[] argv) { try { int[] labels= { 0, 0, 1, 1,
   * 0, 1, 1, 0, 0, 1, 0, 0 }; double[] values= { 1.0, 2.0, 2.0, 2.0, 1.1, 2.1,
   * 5.5, -3.2, 0.0, 53.2, 0.2, 1.1 }; AbstractBooster booster=
   * AbstractBooster.getInstance(); InequalitySplitterBuilder sb= new
   * InequalitySplitterBuilder( 0, booster, new AttributeDescription[] { null
   * }); Example x; Attribute[] attArray= new Attribute[1]; Label l; if
   * (Monitor.logLevel > 3) Monitor.log("Input: \t index \t value \t label");
   * for (int i= 0; i < labels.length; i++) { l= new Label(labels[i]);
   * attArray[0]= new RealAttribute(values[i]); x= new Example(attArray, l); if
   * (Monitor.logLevel > 3) Monitor.log( " \t " + i + "\t " + values[i] + "\t " +
   * labels[i]); try { sb.addExample(i, x); booster.addExample(i, l); } catch
   * (IncompAttException e) { if (Monitor.logLevel > 3)
   * Monitor.log(e.getMessage()); } } booster.finalizeData(); if
   * (Monitor.logLevel > 3) Monitor.log(booster); sb.finalizeData(); if
   * (Monitor.logLevel > 3) Monitor.log("\n\nOutput:"); if (Monitor.logLevel >
   * 3) Monitor.log(sb); if (Monitor.logLevel > 3) Monitor.log("checking
   * building"); CandidateSplit h= sb.build(); if (Monitor.logLevel > 3)
   * Monitor.log( "\n===\n checking detection of incompatible attribute"); sb=
   * new InequalitySplitterBuilder( 0, booster, new AttributeDescription[] {
   * null }); l= new Label(0); attArray[0]= new DiscreteAttribute(0); x= new
   * Example(attArray, l); try { sb.addExample((int) 0, x); } catch
   * (IncompAttException e) { if (Monitor.logLevel > 3)
   * Monitor.log(e.getMessage()); } } catch (Exception e) { if (Monitor.logLevel >
   * 3) Monitor.log(e.getMessage()); e.printStackTrace(); } finally { if
   * (Monitor.logLevel > 3) Monitor.log("finished testing
   * InequalitySplitterBuilder"); } }
   */
}
