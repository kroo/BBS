package jboost.examples;

import java.io.Serializable;
import java.util.List;

import jboost.NotSupportedException;
import jboost.controller.Configuration;
import jboost.learner.IncompAttException;
import jboost.tokenizer.BadAttException;
import jboost.tokenizer.DataStream;

/**
 * the common description for all types of attributes
 * 
 * @author Yoav Freund (rewritten 8/14/00 by Nigel Duffy)
 * @version $Header:
 *          /cvsroot/jboost/jboost/src/jboost/examples/AttributeDescription.java,v
 *          1.3 2007/10/23 22:44:54 aarvey Exp $
 */

public abstract class AttributeDescription implements Serializable {

  /**
   * Factory method. Generates an AttributeDescription according to the string
   * given in the parameter <tt>type</tt>
   * 
   * @param name:
   *            A string ID for the attribute
   * @param type:
   *            one of the strings: labels/number/string/finite/text
   * @param options:
   *            an options parameter, *EXPLAIN*
   * @param okValues:
   *            allowed values parameter *EXPLAIN*
   */

  public static AttributeDescription build(String name, String type, Configuration options, List okValues) throws ClassNotFoundException {
    AttributeDescription retval = null;

    if (name.equals(DataStream.LABELS_ATTR)) retval = new LabelDescription(name, options, okValues);
    else if (name.equals(DataStream.WEIGHT_ATTR)) retval = new WeightDescription(name, options);
    else if (name.equals(DataStream.INDEX_ATTR)) retval = new IndexDescription(name, options);
    else if (type.equals("number")) retval = new NumberDescription(name, options);
    else if (type.equals("string")) retval = new StringDescription(name, options);
    else if (type.equals("finite")) retval = new FiniteDescription(name, options, okValues);
    else if (type.equals("text")) retval = new TextDescription(name, options);

    retval.type = type;

    return (retval);
  }

  /** identifier of the attribute */
  protected String attributeName;
  /**
   * The class (subclass of Attribute) of the attribute:
   * Discrete,MultiDiscrete,Set
   */
  protected Class attributeClass;
  /** The type of the attribute */
  protected String type;

  /** Options */
  boolean caseSignificant;
  boolean crucial;
  boolean existence;
  boolean ignoreAttribute;
  boolean order;
  boolean punctuationSignificant;

  public String getType() {
    return type;
  }

  public String getAttributeName() {
    return attributeName;
  }

  public Class getAttributeClass() {
    return attributeClass;
  }

  public String getValueName() throws IncompAttException {
    throw (new IncompAttException("Invalid attribute type for setAttributes()"));
  }

  public abstract Attribute str2Att(String string) throws BadAttException;

  /**
   * Deal with the attribute values - these are all appropriate only for the
   * StringDescription subclass
   */
  public void setAttributeValues(String[] values) throws IncompAttException {
    throw (new IncompAttException("Invalid attribute type for setAttributes()"));
  }

  public int getNoOfValues() throws IncompAttException {
    throw (new IncompAttException("Invalid attribute type for getNoOfValues()"));
  }

  public String getAttributeValue(int i) throws IncompAttException {
    throw (new IncompAttException("Invalid attribute type for getAttributeValue()"));
  }

  /**
   * Check the ignoreAttribute flag
   * 
   * @return true if this Attribute is ignored
   */
  public boolean isIgnored() {
    return ignoreAttribute;
  }

  public String toString() {
    String retval = new String(attributeName);
    retval += " " + attributeClass.getName();
    retval += " crucial: " + crucial;
    retval += " ignoreAttribute: " + ignoreAttribute;
    retval += " existence: " + existence;
    return (retval);
  }

  public String toString(Attribute attr) throws Exception {
    throw (new NotSupportedException("toString(Attribute)", "AttributeDescription"));
  }
}
