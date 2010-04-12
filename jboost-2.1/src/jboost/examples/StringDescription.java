package jboost.examples;

import java.util.HashMap;

import jboost.controller.Configuration;
import jboost.tokenizer.BadAttException;
import jboost.tokenizer.StringOp;

/**
 * the description for string attributes.
 */
class StringDescription extends AttributeDescription {

  StringDescription(String name, Configuration c) throws ClassNotFoundException {
    attributeName = name;
    attributeClass = Class.forName("jboost.examples.RealAttribute");

    crucial = c.getBool("crucial", false);
    ignoreAttribute = c.getBool("ignoreAttribute", false);
    caseSignificant = c.getBool("caseSignificant", false);
    punctuationSignificant = c.getBool("punctuationSignificant", false);
    existence = c.getBool("existence", false);
  }

  private HashMap map = new HashMap(); // mapping from string to int
  private int numStr = 0; // # strings seen so far

  // and value of current string (starts with 0)
  /** converts string int data file to string in attribute */
  public Attribute str2Att(String string) throws BadAttException {
    // System.err.println("DIAG: strAttReader.str2Att: string=<" + string +
    // ">");
    string = string.trim();
    if (string.length() == 0) return (new DiscreteAttribute());
    string = StringOp.shrinkWhitespace(string); // ok tho' immutable?
    if (!caseSignificant) string = string.toLowerCase();
    if (!punctuationSignificant) string = StringOp.removePunctuation(string);
    // System.err.println("DIAG strAttReader.str2Att: leaving");
    if (map.containsKey(string)) // string appeared before
    return new DiscreteAttribute(((Integer) map.get(string)).intValue());
    else {
      map.put(string, new Integer(numStr)); // put new key
      return new DiscreteAttribute(numStr++);
    }
  }

  public String toString() {
    String retval = new String(attributeName);
    retval += " " + attributeClass.getName();
    retval += " crucial: " + crucial;
    retval += " ignoreAttribute: " + ignoreAttribute;
    retval += " caseSignificant: " + caseSignificant;
    retval += " punctuationSignificant: " + punctuationSignificant;
    retval += " existence: " + existence;
    return (retval);
  }

  public String toString(Attribute attr) {
    String retval = new String();
    if (attr.isDefined() == false) return ("UNDEFINED");
    int tok = ((DiscreteAttribute) attr).getValue();
    // This should be improved when the reader is integrated.
    retval += tok;
    return (retval);
  }
}
