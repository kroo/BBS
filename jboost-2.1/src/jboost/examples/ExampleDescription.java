package jboost.examples;

import java.util.Vector;

/**
 * A description of the fields in a single example
 * 
 * @author Yoav Freund
 * @version $Header:
 *          /cvsroot/jboost/jboost/src/jboost/examples/ExampleDescription.java,v
 *          1.2 2007/10/02 20:34:56 aarvey Exp $
 */
public class ExampleDescription {

  /** An AttributeDescription for the labels */
  private LabelDescription m_label;

  /** An AttributeDescription for the example weights */
  private WeightDescription m_weight;

  /** An AttributeDescription for INDEX */
  private IndexDescription m_index;

  /** a description of each attribute */
  private Vector m_attribute;

  /** the index of the example attribute that contains the label */
  private int m_labelIndex;

  /** the index of the example attribute that contains the weight */
  private int m_weightIndex;

  /** the index of the example attribute that contains INDEX */
  private int m_indexIndex;

  /** Flag for using private sampling weights */
  private boolean m_useSamplingWeights;

  /**
   * Defaul constructor
   */
  public ExampleDescription() {
    m_attribute = new Vector();
    m_label = null;
    m_weight = null;
    m_index = null;
    m_useSamplingWeights = false;
    m_labelIndex = -1;
    m_weightIndex = -1;
    m_indexIndex = -1;
  }

  /**
   * Sets the index description.
   */
  public void setIndex(AttributeDescription ad) {
    m_index = (IndexDescription) ad;
  }

  /**
   * Sets the index description.
   */
  public void setIndex(AttributeDescription ad, int index) {
    m_index = (IndexDescription) ad;
    m_indexIndex = index;
  }

  /**
   * Sets the label description.
   */
  public void setLabel(AttributeDescription ad) {
    m_label = (LabelDescription) ad;
  }

  /**
   * Sets the label description.
   */
  public void setLabel(AttributeDescription ad, int index) {
    m_label = (LabelDescription) ad;
    m_labelIndex = index;
  }

  /**
   * Return the index of the label attribute
   */
  public int getLabelIndex() {
    return m_labelIndex;
  }

  /**
   * Return the index of the weight attribute
   */
  public int getWeightIndex() {
    return m_weightIndex;
  }

  /**
   * Return the index of the INDEX
   */
  public int getIndexIndex() {
    return m_indexIndex;
  }

  /**
   * Assign the weight description
   */
  public void setWeight(AttributeDescription weightDescription) {
    m_weight = (WeightDescription) weightDescription;
  }

  /**
   * Assign the weight description
   */
  public void setWeight(AttributeDescription weightDescription, int index) {
    m_weight = (WeightDescription) weightDescription;
    m_weightIndex = index;
  }

  /**
   * Return the WeightDescription
   */
  public WeightDescription getWeightDescription() {
    return m_weight;
  }

  /**
   * Return the Index Description
   */
  public IndexDescription getIndexDescription() {
    return m_index;
  }

  /**
   * If true, then the examples with this description will contain an attribute
   * that measures the example's sampling probability.
   * 
   * @param flag
   */
  public void setSamplingWeightsFlag(boolean flag) {
    m_useSamplingWeights = flag;
  }

  /**
   * Return the flag value
   * 
   * @param flag
   */
  public boolean getSamplingWeightsFlag() {
    return m_useSamplingWeights;
  }

  /** Add an attribute description. */
  public void addAttribute(AttributeDescription ad) {
    m_attribute.add(ad);
  }

  /**
   * Converts an Example to a string this function is here since the way in
   * which examples are stored needs to use the example description to reproduce
   * something interpretable.
   */
  public String toString(Example e) throws Exception {
    String retval = new String();
    Label l = e.getLabel();
    Instance ins = e.getInstance();
    retval += m_label.toString(l);
    int i = 0;
    int s = ins.getSize();
    for (i = 0; i < s; i++) {
      retval += ((AttributeDescription) m_attribute.get(i)).toString(e.getAttribute(i));
    }
    return (retval);
  }

  /**
   * Converts the ExampleDescription into a String the output is in the same
   * format as a spec file.
   */
  public String toString() {
    String retval = new String();
    for (int i = 0; i < m_attribute.size(); i++)
      retval += (AttributeDescription) m_attribute.get(i) + "\n";
    retval += m_label + "\n";
    return (retval);
  }

  public LabelDescription getLabelDescription() {
    return (m_label);
  }

  public AttributeDescription[] getAttributes() {
    AttributeDescription[] retval = new AttributeDescription[m_attribute.size()];
    for (int i = 0; i < retval.length; i++) {
      retval[i] = (AttributeDescription) m_attribute.get(i);
    }
    return (retval);
  }

  /**
   * Return the number of attributes plus the Label and the example weight, if
   * present
   * 
   * @return count
   */
  public int getNoOfAttributes() {
    return m_attribute.size();
  }

  public AttributeDescription getAttributeDescription(int i) {
    return (AttributeDescription) m_attribute.get(i);
  }

}
