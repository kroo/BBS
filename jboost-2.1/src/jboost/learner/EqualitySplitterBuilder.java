package jboost.learner;

import java.util.Arrays;
import java.util.Vector;

import jboost.CandidateSplit;
import jboost.booster.Bag;
import jboost.booster.Booster;
import jboost.examples.Attribute;
import jboost.examples.AttributeDescription;
import jboost.examples.DiscreteAttribute;
import jboost.examples.Example;

/**
 * Finds the best split based on an attribute==value test on a
 * <i>DiscreteAttribute</i>. ISSUES: Requires an intersect function that should
 * be in booster? ISSUES: There should be a mechanism to say "no useful splits"
 * 
 * @author Nigel
 */
public class EqualitySplitterBuilder extends SplitterBuilder {

  /**
   * Finds the best equality split on a discrete attribute using attr==value
   * tests
   */
  public CandidateSplit build() {
    Bag[] attributeValues = new Bag[attr_val.length];
    Bag universe = booster.newBag();
    int j;
    for (j = 0; j < attr_val.length; j++) {
      attributeValues[j] = intersect(examplesMask, attr_val[j]);
      universe.addBag(attributeValues[j]);
    }

    double minLoss = Double.MAX_VALUE;
    int minVal = 0;
    double loss = 0;
    Bag[] tmpBags = new Bag[2];
    tmpBags[1] = booster.newBag();

    for (j = 0; j < attributeValues.length; j++) {
      tmpBags[1].copyBag(universe);
      tmpBags[0] = attributeValues[j];
      tmpBags[1].subtractBag(tmpBags[0]);
      if ((loss = booster.getLoss(tmpBags)) < minLoss) {
        minLoss = loss;
        minVal = j;
      }
    }
    tmpBags[1].copyBag(universe);
    tmpBags[0] = attributeValues[minVal];
    tmpBags[1].subtractBag(tmpBags[0]);

    Splitter s = new EqualitySplitter(attributeIndex, minVal, 2, desc[0]);
    // if(Monitor.logLevel>3) Monitor.log(s);
    return (new CandidateSplit(this, s, tmpBags, minLoss));
  }

  /**
   * @param splitter
   * @return split
   */
  public CandidateSplit build(Splitter s) {

    EqualitySplitter splitter = (EqualitySplitter) s;

    Bag[] bags = new Bag[2];
    Bag[] attributeValues = new Bag[attr_val.length];
    Bag universe = booster.newBag();
    int j;
    double minLoss = Double.MAX_VALUE;

    for (j = 0; j < attr_val.length; j++) {
      attributeValues[j] = intersect(examplesMask, attr_val[j]);
      universe.addBag(attributeValues[j]);
    }

    bags[1] = booster.newBag();
    bags[1].copyBag(universe);
    bags[0] = attributeValues[splitter.getIndex()];
    bags[1].subtractBag(bags[0]);
    minLoss = booster.getLoss(bags);

    return (new CandidateSplit(this, splitter, bags, minLoss));
  }

  /**
   * Find the best attribute value to use and return the list of bags that split
   * along that value
   * 
   * @return list of bags
   */
  private Bag[] findBestAttributeValues() {
    Bag[] attributeValues = new Bag[attr_val.length];
    Bag universe = booster.newBag();
    int j;
    for (j = 0; j < attr_val.length; j++) {
      attributeValues[j] = intersect(examplesMask, attr_val[j]);
      universe.addBag(attributeValues[j]);
    }

    double minLoss = Double.MAX_VALUE;
    int minVal = 0;
    double loss = 0;
    Bag[] tmpBags = new Bag[2];
    tmpBags[1] = booster.newBag();

    for (j = 0; j < attributeValues.length; j++) {
      tmpBags[1].copyBag(universe);
      tmpBags[0] = attributeValues[j];
      tmpBags[1].subtractBag(tmpBags[0]);
      if ((loss = booster.getLoss(tmpBags)) < minLoss) {
        minLoss = loss;
        minVal = j;
      }
    }
    tmpBags[1].copyBag(universe);
    tmpBags[0] = attributeValues[minVal];
    tmpBags[1].subtractBag(tmpBags[0]);
    return tmpBags;
  }

  /**
   * construct a new SplitterBuilder based on this one and some subset of the
   * data.
   * 
   * @param an
   *            array holding the exampleMask for the subset
   * @param the
   *            no of elements in the subset.
   */
  public SplitterBuilder spawn(boolean[] em, int count) {
    return new EqualitySplitterBuilder(booster, em, count, attr_val, attributeIndex, desc);
  }

  /**
   * Figures out the split of the data for a given splitter. In other words
   * determines which examples make it from THIS splitterBuilder to each side of
   * the split. The idea here is to be able to use a splitter without retaining
   * all of the examples.
   * 
   * @param The
   *            splitter on which to base the split
   * @returns The partition of the data or null of the splitter is not
   *          compatible.
   */
  public int[][] split(Splitter sp) {
    if (attributeIndex != sp.getIndex()) return (null);
    int v = ((EqualitySplitter) sp).getValue();
    int d = ((EqualitySplitter) sp).getDegree();
    int[][] retval = new int[d][];
    int tmp = attr_val[v].length;
    int i = 0;
    int count0 = 0;
    int count1 = 0;

    for (int j = 0; j < attr_val[v].length; j++) {
      if (examplesMask[attr_val[v][j]]) {
        count0++;
      }
    }
    retval[0] = new int[count0];
    count0 = 0;
    for (int j = 0; j < attr_val[v].length; j++) {
      if (examplesMask[attr_val[v][j]]) {
        retval[0][count0] = attr_val[v][j];
        count0++;
      }
    }
    if (d == 2) {
      for (i = 0; i < attr_val.length; i++) {
        if (i != v) {
          for (int j = 0; j < attr_val[i].length; j++) {
            if (examplesMask[attr_val[i][j]]) count1++;
          }
        }
      }
      retval[1] = new int[count1];
      count1 = 0;
      for (i = 0; i < attr_val.length; i++) {
        if (i != v && d == 2) {
          for (int j = 0; j < attr_val[i].length; j++) {
            if (examplesMask[attr_val[i][j]]) {
              retval[1][count1] = attr_val[i][j];
              count1++;
            }
          }
        }
      }
    }
    return (retval);
  }

  /** Constructor */
  public EqualitySplitterBuilder(Booster b, boolean[] em, int noEl, int[][] a, int attributeIndex, AttributeDescription[] attr) {
    booster = b;
    examplesMask = em;
    noOfElements = noEl;
    attr_val = a;
    isRoot = false;
    isFinalized = true;
    this.attributeIndex = attributeIndex;
    desc = attr;
    m_type = SplitterType.EQUALITY_SPLITTER;
  }

  /** Default Constructor */
  public EqualitySplitterBuilder() {
    booster = null;
    examplesMask = null;
    noOfElements = -1;
    attr_val = null;
    isRoot = false;
    isFinalized = true;
    desc = null;
    m_type = SplitterType.EQUALITY_SPLITTER;
  }

  /** describe as a string for dubugging printout */
  public String toString() {
    boolean first = true;
    String s = "EqualitySplitterBuilder for attribute " + attributeIndex + "\n";
    if (isRoot) {
      s += "Is Root:\nvalue\tExample\n";
      if (attr_val == null) s += "table is null\n";
      else {
        for (int i = 0; i < attr_val.length; i++) {
          s += i + "\t";
          s += attr_val[i][0];
          for (int j = 1; j < attr_val[i].length; j++) {
            s += "," + attr_val[i][j];
          }
          s += "\n";
        }
      }
    }
    s += "ExamplesMask:\n";
    if (examplesMask == null) s += "is empty\n";
    else {
      for (int i = 0; i < examplesMask.length; i++) {
        if (examplesMask[i]) s += "1";
        else s += "0";
      }
    }
    return s;
  }

  /**
   * The constructor for the root splitterbuilder
   * 
   * @param index
   *            the index of the relevant attribute
   * @param booster
   *            the booster that is to be used by this builder
   */
  public EqualitySplitterBuilder(int index, Booster booster, AttributeDescription[] ad) {
    desc = ad;
    isRoot = true;
    isFinalized = false;
    try {
      maxNoVals = ad[0].getNoOfValues();
    }
    catch (IncompAttException e) {
      throw new RuntimeException("Incomatiable Attribute error in EqualitySplitterBuilder" + " index=" + index + "\n" + e.getMessage());
    }
    if (maxNoVals < 0) maxNoVals = 0;
    attributeIndex = index;
    this.booster = booster;
    attr_val = null;
    maxIndex = -1;
    noOfElements = 0;
    valueVec = new Vector();
    m_type = SplitterType.EQUALITY_SPLITTER;
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
    if (!isRoot || isFinalized) throw new RuntimeException("Trying to addExample() to non-root or finalized SplitterBuilder");
    DiscreteAttribute a = null;
    int exVal = 0;
    Attribute t = example.getAttribute(attributeIndex);
    // check that attribute is of the correct class
    try {
      a = (DiscreteAttribute) t; // try downcasting
    }
    catch (ClassCastException e) {
      throw new IncompAttException(index, attributeIndex, "DiscreteAttribute", a.getClass());
    }
    if (index > maxIndex) {
      for (int i = maxIndex + 1; i <= index; i++)
        valueVec.add(new Integer(-1));
      maxIndex = index;
    }
    if (a.isDefined()) {
      exVal = a.getValue();
      if (exVal > maxNoVals - 1) maxNoVals = exVal + 1;
      valueVec.set(index, new Integer(exVal));
    }
  }

  public void finalizeData() {
    if (!isRoot || isFinalized) throw new RuntimeException("Trying to finalizeData() to non-root or finalized SplitterBuilder");
    int i, j;
    int s;

    int value = 0;
    noOfElements = maxIndex + 1;
    examplesMask = new boolean[noOfElements];
    Arrays.fill(examplesMask, true);
    Vector[] attrVec = new Vector[maxNoVals];
    for (i = 0; i < maxNoVals; i++)
      attrVec[i] = new Vector();
    for (i = 0; i < maxIndex + 1; i++) {
      value = ((Integer) valueVec.get(i)).intValue();
      if (value >= 0) {
        attrVec[value].add(new Integer(i));
      }
    }
    attr_val = new int[maxNoVals][];
    for (i = 0; i < maxNoVals; i++) {
      s = attrVec[i].size();
      attr_val[i] = new int[s];
      for (j = 0; j < s; j++) {
        attr_val[i][j] = ((Integer) attrVec[i].elementAt(j)).intValue();
      }
      attrVec[i].clear();
    }
    isFinalized = true;
    tmpAttrVals = null;
    exampleMaskVec = null;
    valueVec = null;
  }

  // ----------------------------- Protected Members
  // ---------------------------------------//

  /** The index of the attribute on which this bulder works */
  protected int attributeIndex;

  /**
   * A list of examples with each attribute=value, perhaps there is a better
   * data structure than an array of ints. But BitSet doesn't seem right as each
   * one would be very sparse. One copy of this is generated by the root
   * splitterBuilder and is pointed to by all of its descendents.
   */
  protected int[][] attr_val;

  // ------------------------------ Private Members
  // ---------------------------------------//

  /**
   * a temporary storage for the values and indices of the attributes. This
   * storage is freed up when the builder is finalized.
   */
  private Vector tmpAttrVals;
  private int maxNoVals;
  private Vector exampleMaskVec;
  private Vector valueVec;

  /** The maximum index reached on reading the data */
  private int maxIndex;

  /**
   * It would be nice if there was a generic intersect function that returned a
   * bag, but where to put it.
   */
  private Bag intersect(boolean[] mask, int[] examples) {
    Bag b = booster.newBag();
    for (int i = 0; i < examples.length; i++)
      if (mask[examples[i]]) b.addExample(examples[i]);
    return (b);
  }

  // ----------------------------- Test Stuff
  // --------------------------------------------//

  /** A main for testing this class */
  /*
   * static public void main(String[] argv) { try{ DataStream ds=new
   * jboost_DataStream(false,"test (one,two,three)\n label (one,two)\n");
   * ExampleDescription ed=ds.getExampleDescription(); AbstractBooster
   * boos=AbstractBooster.getInstance(); AttributeDescription[] ad=new
   * AttributeDescription[1]; ad[0]=ed.getAttributeDescription(0);
   * EqualitySplitterBuilder sb = new EqualitySplitterBuilder(0,boos,ad); int[]
   * trainLabels= { 0, 0, 1, 0, 0, 1, 0, 0, 1, 1, 1, 0}; int[]
   * trainValues={0,2,2,2,1,2,0,1,0,0,2,1}; int[] testLabels= { 0, 0, 1, 0, 0,
   * 1, 0, 0, 1, 1, 1, 0}; int[] testValues={0,2,2,2,1,2,0,1,0,0,2,1}; Example
   * x; Attribute[] attArray = new Attribute[1]; Label l; if(Monitor.logLevel>3)
   * Monitor.log("Input: \t index \t value \t label"); for(int i=0; i<trainLabels.length;
   * i++) { l=new Label(trainLabels[i]); attArray[0]=new
   * DiscreteAttribute(trainValues[i]); x=new Example(attArray,l);
   * if(Monitor.logLevel>3) Monitor.log(" \t "+i+"\t "+trainValues[i]+"\t
   * "+trainLabels[i]); try{ sb.addExample(i,x); boos.addExample(i,l); }
   * catch(IncompAttException e) { if(Monitor.logLevel>3)
   * Monitor.log(e.getMessage()); } } sb.finalizeData(); boos.finalizeData();
   * if(Monitor.logLevel>3) Monitor.log(sb); CandidateSplit bC=sb.build();
   * if(Monitor.logLevel>3) Monitor.log(bC); boolean[] tmpMask=new boolean[12];
   * Arrays.fill(tmpMask,false); tmpMask[0]=true; tmpMask[9]=true;
   * tmpMask[1]=true; tmpMask[4]=true; tmpMask[2]=true; tmpMask[10]=true;
   * SplitterBuilder esb=sb.spawn(tmpMask,1); if(Monitor.logLevel>3)
   * Monitor.log(esb); bC=esb.build(); if(Monitor.logLevel>3) Monitor.log(bC); }
   * catch(Exception e) { if(Monitor.logLevel>3) Monitor.log(e.getMessage());
   * e.printStackTrace(); } }
   */
}
