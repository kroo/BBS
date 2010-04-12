/** EqualitySplitter
 *  Splits the data based on testing a discrete attribute
 *  for equality with some value
 *
 * @author Nigel Duffy
 */
package jboost.learner;

import jboost.examples.Attribute;
import jboost.examples.AttributeDescription;
import jboost.examples.DiscreteAttribute;
import jboost.examples.Instance;

/**
 * Splits examples according to whether a {@link jboost.examples.RealAttribute}
 * is equal to a fixed value. If it is equal, returns 0 or -1 if unknown or 1 if
 * not equal and split is binary.
 */
class EqualitySplitter extends Splitter {

  private int value;

  /** Does this need to be public? */
  public int getValue() {
    return (value);
  }

  public EqualitySplitter(int ind, int v, int deg, AttributeDescription attr) {
    index = ind;
    value = v;
    if (deg > 2 || deg < 1) throw (new RuntimeException("Equality Splitter defined with inappropriate degree:" + deg));
    degree = deg;
    desc = attr;
    m_type = SplitterType.EQUALITY_SPLITTER;
  }

  /**
   * Return the partition in which an instance belongs. returning -1 signifies
   * "partition unknown". If the attribute is equal to the desired value, then
   * return 0. If the split is binary and the attribute is not equal, then
   * return 1. Inherited from Splitter
   */
  public int eval(Instance instance) throws IncompAttException {
    Attribute t = instance.getAttribute(index);
    if (t == null) {
      return (-1);
    }
    try {
      DiscreteAttribute a = (DiscreteAttribute) t; // try downcasting
      if (a.getValue() == value) return (0);
      else if (degree == 2) return (1);
      else throw (new RuntimeException("Equality Split with degree greater than 2"));
    }
    catch (ClassCastException e) {
      throw new IncompAttException("InequalitySplitter Error:", index, t.getClass());
    }
  }

  /** Produce a copy */
  public Object clone() {
    EqualitySplitter retval = new EqualitySplitter(index, value, degree, desc);
    return (retval);
  }

  /** Returns a string describing this splitter */
  public String toString() {
    String retval = new String("EqualitySplit: " + degree);
    try {
      retval += " " + desc.getAttributeName() + " = " + desc.getAttributeValue(value);
    }
    catch (Exception e) {
      retval = null;
    }
    return (retval);
  }

  /** Produces a java implementation of this splitter. */
  public String toC(String fname) {
    return (null);
  }

  /** Produces a java implementation of this splitter. */
  public String toJava(String fname) {
    String retval = "\tprivate int " + fname + "(Instance ins){\n";
    retval += "\t\tif(((DiscreteAttribute)ins.getAttribute(" + index + ")).getValue()==" + value + ") return(0);\n";
    retval += "\t\telse return(1);\n";
    retval += "\t}\n\n";
    return (retval);
  }

  public boolean equals(Splitter sp) {
    EqualitySplitter isp;
    try {
      isp = (EqualitySplitter) sp;
    }
    catch (ClassCastException e) {
      return (false);
    }
    if (index == isp.index && value == isp.value) return (true);
    else return (false);
  }

  /** Get a summary. */
  public Summary getSummary() {
    Summary retval = new Summary();
    retval.index = index;
    retval.type = Summary.EQUALITY;
    retval.val = new Integer(value);
    return (retval);
  }
}
