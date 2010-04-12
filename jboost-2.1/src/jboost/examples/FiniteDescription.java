package jboost.examples;

import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import jboost.controller.Configuration;
import jboost.tokenizer.BadAttException;
import jboost.tokenizer.StringOp;

/**
 * the description for finite attributes.
 */
class FiniteDescription extends AttributeDescription {

  FiniteDescription(String name, Configuration c, List okValues) throws ClassNotFoundException {
    map = new HashMap();
    attributeName = name;
    attributeClass = Class.forName("jboost.examples.DiscreteAttribute");
    ;
    attributeValues = new Vector();

    crucial = c.getBool("crucial", false);
    ignoreAttribute = c.getBool("ignoreAttribute", false);
    caseSignificant = c.getBool("caseSignificant", false);
    punctuationSignificant = c.getBool("punctuationSignificant", false);
    existence = c.getBool("existence", false);
    order = c.getBool("order", false);

    String tmp = null;

    for (int i = 0; i < okValues.size(); i++) {
      tmp = (String) okValues.get(i);
      if (!punctuationSignificant) tmp = StringOp.removePunctuation(tmp);
      if (!caseSignificant) tmp = tmp.toLowerCase();
      map.put(tmp, new Integer(i));
      attributeValues.add(tmp);
    }
  }

  public String toString() {
    String retval = new String(attributeName);
    retval += " " + attributeClass.getName();
    retval += " ( ";
    String tmp = null;
    if (attributeValues.size() > 0) tmp = getAttributeValue(0);
    retval += tmp;
    for (int i = 1; i < attributeValues.size(); i++) {
      retval += " , ";
      tmp = getAttributeValue(i);
      retval += tmp;
    }
    retval += " ) ";
    retval += " crucial: " + crucial;
    retval += " ignoreAttribute: " + ignoreAttribute;
    retval += " caseSignificant: " + caseSignificant;
    retval += " punctuationSignificant: " + punctuationSignificant;
    retval += " existence: " + existence;
    retval += " order: " + order;
    return (retval);
  }

  /**
   * Reads a finite attribute Note that it is currently based on String but
   * should use StringBuffer
   */
  public Attribute str2Att(String string) throws BadAttException {
    if (string == null) return (new DiscreteAttribute());
    string = string.trim();
    if (string.length() == 0) return (new DiscreteAttribute());
    string = StringOp.shrinkWhitespace(string);
    if (!caseSignificant) string = string.toLowerCase();
    if (!punctuationSignificant) string = StringOp.removePunctuation(string);
    if (map.containsKey(string)) return (new DiscreteAttribute(((Integer) map.get(string)).intValue()));
    if (map.containsKey("*")) return (new DiscreteAttribute(((Integer) map.get("*")).intValue()));
    throw (new BadAttException("Unknown value: " + string + " when not allowed." + map));
  }

  /** set the attributeValues array */
  public void setAttributeValues(Vector values) {
    attributeValues = values;
  }

  public int getNoOfValues() {
    return attributeValues.size();
  }

  public String getAttributeValue(int i) {
    return (String) attributeValues.get(i);
  }

  public String toString(Attribute attr) throws Exception {
    if (attr.isDefined() == false) return ("undefined");
    String retval = new String();
    int tok = ((DiscreteAttribute) attr).getValue();
    if (tok >= attributeValues.size()) throw (new Exception("Attribute value larger than allowed"));
    // This should be improved when the reader is integrated.
    retval += (String) attributeValues.get(tok);
    return (retval);
  }

  protected Vector attributeValues;
  protected HashMap map = new HashMap();

}
