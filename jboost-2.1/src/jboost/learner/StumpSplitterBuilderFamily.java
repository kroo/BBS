package jboost.learner;

import java.util.Vector;

import jboost.booster.Booster;
import jboost.controller.Configuration;
import jboost.examples.AttributeDescription;
import jboost.examples.BooleanAttribute;
import jboost.examples.DiscreteAttribute;
import jboost.examples.ExampleDescription;
import jboost.examples.IntegerAttribute;
import jboost.examples.RealAttribute;
import jboost.examples.SetAttribute;
import jboost.monitor.Monitor;

/**
 * Implements a meta-class for SplitterBuilders of decision stumps. This class
 * is responsible for constructing the SplitterBuilders that correspond to
 * decision stumps and can be used on the current ExampleDescription.
 * 
 * @author Nigel Duffy
 */
public class StumpSplitterBuilderFamily extends SplitterBuilderFamily {

  private boolean textAbstain;
  private static final String prefix = "learner_";

  /**
   * Default constructor
   */
  public StumpSplitterBuilderFamily(Configuration config) {
    textAbstain = config.getBool(prefix + "textAbstain", false);
    if (Monitor.logLevel > 3) {
      Monitor.log("textAbstain = " + textAbstain);
    }
  }

  /**
   * Constructs a vector of single-attribute SplitterBuilders to match the given
   * ExampleDescription.
   */
  public Vector build(ExampleDescription exDesc, int[] attr, boolean usePolicy, Configuration config, Booster booster) throws IncompAttException {
    // attribute class templates
    RealAttribute realAttribute = new RealAttribute(0.0);
    DiscreteAttribute discreteAttribute = new DiscreteAttribute(0);
    SetAttribute setAttribute = new SetAttribute();
    IntegerAttribute integerAttribute = new IntegerAttribute();
    BooleanAttribute booleanAttribute = new BooleanAttribute();

    Vector retval = new Vector();
    AttributeDescription[] attrDesc = exDesc.getAttributes();
    int noOfAtt = attr.length;
    SplitterBuilder tmpSB = null;

    // check each attribute to make sure that we are ignoring attributes when
    // necessary
    for (int i = 0; i < noOfAtt; i++) {
      if (!attrDesc[attr[i]].isIgnored()) {
        Class attributeClass = attrDesc[attr[i]].getAttributeClass();

        if (attributeClass.equals(realAttribute.getClass())) {
          tmpSB = new InequalitySplitterBuilder(attr[i], booster, new AttributeDescription[] { attrDesc[attr[i]] });
        }
        else if (attributeClass.equals(integerAttribute.getClass())) {
          tmpSB = new InequalitySplitterBuilder(attr[i], booster, new AttributeDescription[] { attrDesc[attr[i]] });
        }
        else if (attributeClass.equals(discreteAttribute.getClass())) {
          tmpSB = new EqualitySplitterBuilder(attr[i], booster, new AttributeDescription[] { attrDesc[attr[i]] });
        }
        else if (attributeClass.equals(setAttribute.getClass())) {
          tmpSB = SetSplitterBuilder.newSplitterBuilder(attr[i], booster, textAbstain, new AttributeDescription[] { attrDesc[attr[i]] });
        }
        else {
          throw new IncompAttException("Trying to build SplitterBuilders vector but cannot identify attribute:", attr[i], attributeClass);
        }

        if (Monitor.logLevel > 3) {
          Monitor.log("attribute " + attr[i] + " class is:" + attributeClass);
        }

        retval.add(tmpSB);
      }
    }
    return retval;
  }
}
