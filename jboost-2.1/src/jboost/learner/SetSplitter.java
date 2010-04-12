package jboost.learner;

import java.util.Arrays;

import jboost.examples.Attribute;
import jboost.examples.AttributeDescription;
import jboost.examples.Instance;
import jboost.examples.SetAttribute;

/*
 * Splits examples according to whether the intersection of a
 * <i>SetAttribute</> and a fixed list of tokens is non-empty.
 */
class SetSplitter extends Splitter {

  private int token; // The index of the token on which the split is based
  private boolean abstain; // abstaining flag when token does not appear

  public int getToken() {
    return token;
  }

  public boolean isAbstaining() {
    return abstain;
  }

  public SetSplitter(int i, int t, boolean a, AttributeDescription ad) {
    index = i;
    token = t;
    abstain = a;
    desc = ad;
    m_type = SplitterType.SET_SPLITTER;
  }

  /*
   * Return the partition to which the token belongs. When NOT abstaining:
   * -------------------- -1 - signifies "partition unknown" (e.g. attribute not
   * defined) 0 - siginifies that the token appears in the attribute +1 -
   * siginifies that the token does not appear in the attribute When abstaining:
   * ---------------- -1 - signifies "partition unknown" (e.g. attribute not
   * defined) OR token does not appear 0 - siginifies that the token appears in
   * the attribute
   */
  // CHANGED - name from partition to eval.
  public int eval(Instance instance) throws IncompAttException {

    Attribute t = instance.getAttribute(index);

    if (t == null) {
      return (-1);
    }

    SetAttribute a = (SetAttribute) t; // try downcasting

    return (Arrays.binarySearch(a.getList(), token) >= 0 ? 0 : (abstain ? -1 : 1));
  }

  /* return the number of splits of a setSplitter */
  public int getDegree() {
    if (abstain) return 1;
    else return 2;
  }

  // CHANGED - name from copy() to clone()
  public Object clone() {
    SetSplitter ss = new SetSplitter(index, token, abstain, desc);
    return ss;
  }

  /** Returns a string describing this splitter * */
  public String toString() {
    String s = null;
    try {
      s = new String("Attribute:" + desc.getAttributeName() + " Token:" + desc.getAttributeValue(token) + " Abstain: " + abstain);
    }
    catch (IncompAttException e) {
      s = new String("Incompatible attribute description:" + desc.getClass());
    }
    return s;
  }

  // ADDED

  /** Returns a string containing Java code. */
  public String toJava(String fname) {
    return (null);
  }

  /** Returns a string containing Java code. */
  public String toC(String fname) {
    return (null);
  }

  public boolean equals(Splitter sp) {
    SetSplitter isp;
    try {
      isp = (SetSplitter) sp;
    }
    catch (ClassCastException e) {
      return (false);
    }
    if (index == isp.index && token == isp.token && abstain == isp.abstain) return (true);
    else return (false);
  }

  /** Get a summary. */
  public Summary getSummary() {
    Summary retval = new Summary();
    retval.index = index;
    retval.type = (abstain ? Summary.CONTAINS_ABSTAIN : Summary.CONTAINS_NOABSTAIN);
    retval.val = desc.getAttributeValue(token);
    return (retval);
  }

  // END_ADDED
}
