package jboost.examples;

import java.util.Arrays;

import jboost.monitor.Monitor;

/**
 * An attribute that represents a (small) subset of a large discrete set. This
 * attribute is used to represent the existance of a set of words in a
 * natural-language document such as Email.
 * 
 * @author Yoram Singer
 */
public class SetAttribute extends Attribute {

  /** a testing main */
  public static void main(String[] argv) {
    try {
      if (Monitor.logLevel > 3) Monitor.log("starting to test SetAttribute");
      int[] s = { 5, 2, 3, 7 };
      SetAttribute a = new SetAttribute(s);
      if (Monitor.logLevel > 3) Monitor.log(a);
      if (Monitor.logLevel > 3) Monitor.log("The element 2 is in the set:  " + a.contains(2));
      if (Monitor.logLevel > 3) Monitor.log("The element 4 is in the set:  " + a.contains(4));

      if (Monitor.logLevel > 3) Monitor.log("testing exception throwing");
      s[0] = 7;
      try {
        a = new SetAttribute(s);
      }
      catch (RepeatedElementException e) {
        if (Monitor.logLevel > 3) Monitor.log(e.getMessage());
      }
      if (Monitor.logLevel > 3) Monitor.log("finished testing SetAttribute");
    }
    catch (Exception e) {
      if (Monitor.logLevel > 3) Monitor.log(e.getMessage());
      e.printStackTrace();
    }
  }

  /** an array with all of the elements in sorted order */
  private int[] set;
  /**
   * a pointer to the textDescription object related to this set (null if not
   * based on text)
   */
  private TextDescription textDescription;

  /** Default Constructor */
  public SetAttribute() {
    defined = false;
  }

  /**
   * Constructor that gets as input an array of token numbers (supposed to be
   * without repetitions)
   */
  public SetAttribute(int[] s) throws RepeatedElementException {
    if (s == null) {
      defined = false;
      set = null;
    }
    else {
      set = s; // initialize array
      Arrays.sort(set); // sort the values
      for (int i = 0; i < s.length - 1; i++) {
        if (set[i] == set[i + 1]) // check for repeated values
        throw new RepeatedElementException("the value " + set[i] + " appears more than once");
      }
      defined = true;
    }
    textDescription = null;
  }

  /**
   * a constructor that also stores a pointer to the related textDescription
   * object
   */
  public SetAttribute(int[] s, TextDescription textDescription) throws RepeatedElementException {
    this(s);
    this.textDescription = textDescription;
  }

  public String toString() {
    String s = "";
    if (set == null) s += "null set";
    else if (set.length == 0) s += "set with zero elements";
    else {
      if (textDescription != null) {
        s += textDescription.toString(this);
      }
      else {
        s = "Set(" + (defined ? "defined" : "undef") + ") Sorted elements:\n" + set[0];

        for (int i = 1; i < set.length; i++) {
          if ((i % 10) == 0) s += "\n"; // print 10 element per line
          s += ", " + set[i];
        }
      }
    }
    return (s + "\n");
  }

  /** check if a particular value is in the set */
  public boolean contains(int val) {
    return (defined && (Arrays.binarySearch(set, val) >= 0));
  }

  /** return the list of values in the set */
  public int[] getList() {
    return set;
  }

  /** returns a copy of the list of values in the set */
  public int[] cloneList() {
    int[] s = new int[set.length];
    for (int i = 0; i < set.length; i++)
      s[i] = set[i];
    return (s);
  }

}
