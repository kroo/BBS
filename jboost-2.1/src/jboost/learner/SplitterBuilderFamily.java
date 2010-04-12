package jboost.learner;

import java.util.Vector;

import jboost.booster.Booster;
import jboost.controller.Configuration;
import jboost.examples.ExampleDescription;

/**
 * Implements a meta-class for SplitterBuilders of a certain general type, for
 * example decision stumps, linear rules etc. This class is responsible for
 * constructing the SplitterBuilders that correspond to that class and can be
 * used on the current ExampleDescription.
 * 
 * @author Nigel Duffy
 */
public abstract class SplitterBuilderFamily {

  /**
   * Default constructor
   */
  public SplitterBuilderFamily() {
  }

  /**
   * Constructs a Vector of SplitterBuilderFamilys Uses
   * StumpSplitterBuilderFamily as the default splitter builders
   */
  public static Vector factory(Configuration config) {
    Vector tmp = new Vector();
    SplitterBuilderFamily sbf = null;

    sbf = new StumpSplitterBuilderFamily(config);
    tmp.add(sbf);

    return (tmp);
  }

  /**
   * Construct a vector of SplitterBuilders corresponding to the members of this
   * family as they may apply to the given set of attributes.
   * 
   * @param An
   *            ExampleDescription.
   * @param An
   *            array of integers listing the AttributeDescrptions for the
   *            attributes to which this family should be applied.
   * @param A
   *            flag determining whether all the specified attributes should be
   *            handled or whether the default policy should be used to
   *            determine which should be handled.
   */
  public abstract Vector build(ExampleDescription exDesc, int[] attr, boolean usePolicy, Configuration config, Booster booster) throws IncompAttException;

  /**
   * Use the default policy to construct a Vector of SplitterBuilders for all
   * attributes.
   */
  public Vector build(ExampleDescription ed, Configuration config, Booster booster) throws IncompAttException {
    int[] attr = new int[ed.getAttributes().length];
    for (int i = 0; i < attr.length; i++) {
      attr[i] = i;
    }
    return (this.build(ed, attr, true, config, booster));
  }
}
