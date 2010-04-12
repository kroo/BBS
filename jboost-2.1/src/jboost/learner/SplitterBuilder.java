package jboost.learner;

import jboost.CandidateSplit;
import jboost.NotSupportedException;
import jboost.booster.Booster;
import jboost.examples.AttributeDescription;
import jboost.examples.Example;

/**
 * Builds splitter candidates of a specific type for a given set of examples.
 * Generates and holds the information to make the search efficient. If the
 * isRoot flag is set then this SplitterBuilder is capable of constructing the
 * internal data-structures. Data can be added until isFinalized is set.
 * 
 * @author Nigel Duffy
 */
public abstract class SplitterBuilder {

  /**
   * Finds and builds the best BaseClassiffier for the set of examples
   * associated with this SplitterBuilder
   */
  public abstract CandidateSplit build() throws NotSupportedException;

  /**
   * Build a CandidateSplit using the specified Splitter.
   * 
   * @param splitter
   *            to use for this CandidateSplit
   * @return built CandidateSplite
   * @throws NotSupportedException
   */
  public abstract CandidateSplit build(Splitter splitter) throws NotSupportedException;

  /**
   * construct a new SplitterBuilder based on this one and some subset of the
   * data.
   * 
   * @param em
   *            an array holding the exampleMask for the subset
   * @param count
   *            the no of elements in the subset.
   */
  public abstract SplitterBuilder spawn(boolean[] em, int count);

  /**
   * Example implementation { return(new SplitterBuilder(b,em,count)); }
   */

  /** Constructor */
  public SplitterBuilder(Booster b, boolean[] em, int noEl) {
    booster = b;
    examplesMask = em;
    noOfElements = noEl;
  }

  /** Default constructor */
  public SplitterBuilder() {
    booster = null;
    examplesMask = null;
    noOfElements = 0;
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
  public abstract int[][] split(Splitter sp);

  /**
   * Add a single example to the internal data structure only callable for root.
   * 
   * @param index
   *            the index of the example in the dataset
   * @param example
   *            the example
   */
  public abstract void addExample(int index, Example example) throws IncompAttException;

  /** Finish reading the data - should throw an exception if not root. */
  public abstract void finalizeData();

  /**
   * Get the type of this Splitter
   * 
   * @return type
   */
  public SplitterType getType() {
    return m_type;
  }

  /**
   * Check to see if this SplitterBuilder can build the Splitter passed in
   * 
   * @param splitter
   * @return true if this builder can build the splitter
   */
  public boolean canBuild(Splitter splitter) {
    boolean retval = false;
    if (splitter.getType().equals(splitter.getType())) {
      for (int i = 0; i < desc.length; i++) {
        if (splitter.desc.toString().equals(desc[i].toString())) {
          retval = true;
        }
      }
    }
    return retval;
  }

  /** describe as a string for dubugging printout */
  public abstract String toString();

  /** The booster that is used by this builder to evaluate losses */
  protected Booster booster;

  /**
   * A flag for each element in the exampleSet, indicating whether the example
   * is used in this builder.
   */
  protected boolean[] examplesMask;

  /**
   * the number of examples that reach this builder, it should be the number of
   * "true" elements in examplesMask
   */
  protected int noOfElements;

  /** the type of the Splitter that is built by this builder */
  protected SplitterType m_type;

  /* A flag indicating whether this is the root or not. */
  protected boolean isRoot;

  /* A flag indicating whether finalizeData has been called or not. */
  protected boolean isFinalized;

  /* The description of the attributes to which this SplitterBuilder applies. */
  protected AttributeDescription[] desc;
}
