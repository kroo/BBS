package jboost.examples;

import jboost.monitor.Monitor;

/** Holds the value of a multiset attribute, used for example as a label */
public class MultiDiscreteAttribute extends Attribute {

  protected int value; // = single class label or -1 if ill-defined
  protected boolean values[]; // null if single label; else array of

  // boolean classes to which label belongs

  /**
   * constructor
   * 
   * @param value
   *            label's value
   */
  public MultiDiscreteAttribute(int value) {
    this.value = value;
  }

  /**
   * constructor
   * 
   * @param values
   *            array of {0,1} boolean values indicating membership in each
   *            class
   */
  public MultiDiscreteAttribute(boolean[] values) {
    int num_true = 0;
    value = -1;
    for (int i = 0; i < values.length; i++) {
      if (values[i]) {
        value = i;
        num_true++;
      }
    }
    if (num_true != 1) {
      this.values = new boolean[value + 1];
      for (int i = 0; i <= value; i++)
        this.values[i] = values[i];
      value = -1;
    }
  }

  /**
   * returns the single value associated with this label, or -1 if the single
   * label is not well defined (i.e., more or less than one label defined).
   */
  public int getSingleValue() {
    return (value);
  }

  /**
   * returns true if the given class index is associated with this label.
   * 
   * @param l
   *            given class index
   */
  public boolean getMultiValue(int l) {
    if (value >= 0) return (l == value);
    else if (l < 0 || l >= values.length) return false;
    else return values[l];
  }

  public boolean[] getValue() {
    return (values);
  }

  /** Compare to another label */
  public boolean equals(MultiDiscreteAttribute l) {
    if ((value >= 0) || (l.value >= 0)) return (value == l.value);
    else if (values.length != l.values.length) return false;
    else {
      for (int i = 0; i < values.length; i++)
        if (values[i] != l.values[i]) return false;
      return true;
    }
  }

  public String toString() {
    if (values == null) return Integer.toString(value);
    else {
      String s = "[" + (values[0] ? "1" : "0");
      for (int i = 1; i < values.length; i++)
        s += (values[i] ? ",1" : ",0");
      return s + "]";
    }
  }

  public int[] getValues() {
    int[] ret = null;
    if (values == null) {
      ret = new int[1];
      ret[0] = value;
    }
    else {
      ret = new int[values.length];
      int j = 0;
      for (int i = 0; i < values.length; i++)
        if (values[i]) ret[j++] = i;
    }
    return ret;
  }

  public static void main(String[] args) {
    MultiDiscreteAttribute[] a = new MultiDiscreteAttribute[5];
    int i, j;
    a[0] = new MultiDiscreteAttribute(3);
    a[1] = new MultiDiscreteAttribute(new boolean[] { false, false, false, true, false });
    a[2] = new MultiDiscreteAttribute(new boolean[] { false, true, false, true, false });
    a[4] = new MultiDiscreteAttribute(new boolean[] { false, true, false, true });
    a[3] = new MultiDiscreteAttribute(new boolean[] { false });
    for (i = 0; i < a.length; i++) {
      if (Monitor.logLevel > 3) Monitor.log("i = " + i);
      if (Monitor.logLevel > 3) Monitor.log("getSingleValue = " + a[i].getSingleValue());
      for (j = 0; j < 6; j++) {
        if (Monitor.logLevel > 3) Monitor.log("getMultiValue(" + j + ") = " + a[i].getMultiValue(j));
      }
      for (j = 0; j < a.length; j++)
        if (Monitor.logLevel > 3) Monitor.log(i + (a[i].equals(a[j]) ? "=" : "!=") + j);
    }
  }
}
