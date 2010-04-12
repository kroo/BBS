package jboost.learner;

import java.io.Serializable;

import jboost.examples.AttributeDescription;
import jboost.examples.ExampleSet;
import jboost.examples.Instance;

/**
 * A splitter recieves an example and returns -1 for "I don't know" or a
 * non-negative integer for a (non-abstaining) classification
 */
public abstract class Splitter implements Cloneable, Serializable {

  /** the type of this splitter */
  protected SplitterType m_type;

  /**
   * Return the type of this splitter. Uses the value of the SplitterType
   * 
   * @return the SplitterType
   */
  public SplitterType getType() {
    return m_type;
  }

  /**
   * Return the partition in which an instance belongs. returning -1 signifies
   * "partition unknown"
   */
  public abstract int eval(Instance instance) throws IncompAttException;

  /**
   * Compute the partition for a list of instances.
   * 
   * @param exampleSet
   *            The set that contains the examples to be partitioned.
   * @param indices
   *            An array of the relevant indices in exampleSet
   * @return A new array of arrays of integers where each array of integers is a
   *         list of the examples in that partition
   */
  public int[][] partition(ExampleSet exampleSet, int[] indices) throws IncompAttException, PartitionException {
    if (degree == -1) throw (new PartitionException("This splitter had degree: " + degree));
    int[][] part = new int[degree][];
    int[] count = new int[degree];
    int length = indices.length;
    int i = 0;
    for (i = 0; i < degree; i++) {
      part[i] = new int[length];
    }
    int j = 0;
    int p = 0;
    for (i = 0; i < length; i++) {
      j = indices[i];
      p = eval(exampleSet.getExample(j).getInstance());
      if (p < 0) continue;
      part[p][count[p]] = j;
      count[p]++;
    }
    int[][] retval = new int[degree][];
    for (i = 0; i < degree; i++) {
      retval[i] = new int[count[i]];
      System.arraycopy(part[i], 0, retval[i], 0, count[i]);
    }
    return (retval);
  }

  /** Create a copy of this splitter */
  public abstract Object clone();

  /** Returns a string describing this splitter */
  public abstract String toString();

  /** Produces a java implementation of this splitter. */
  public abstract String toJava(String fname);

  /** Produces a java implementation of this splitter. */
  public abstract String toC(String fname);

  /**
   * Produces a summary if possible, should be useful for producing efficient
   * code.
   */
  public Summary getSummary() {
    return (null);
  }

  /** Determines whether two splitters calculate the same split. */
  public abstract boolean equals(Splitter sp);

  // --------------------------- Protected Members
  // ---------------------------------//

  /** The attribute index this splitter refers to */
  protected int index;
  protected AttributeDescription desc;

  public int getIndex() {
    return (index);
  }

  /** Holds the degree of the split */
  protected int degree = -1;

  protected int getDegree() {
    return (degree);
  }
}

class PartitionException extends Exception {

  PartitionException(String m) {
    super(m);
  }
}
