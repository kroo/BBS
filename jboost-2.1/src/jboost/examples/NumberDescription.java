package jboost.examples;

import jboost.controller.Configuration;
import jboost.tokenizer.BadAttException;

/**
 * the description for number attributes.
 */
class NumberDescription extends AttributeDescription {

  NumberDescription(String name, Configuration c) throws ClassNotFoundException {
    attributeName = name;
    attributeClass = Class.forName("jboost.examples.RealAttribute");

    // Need to make a SAFE version of configuration, which implements an error()
    // function, which
    // can return and error message.

    crucial = c.getBool("crucial", false);
    ignoreAttribute = c.getBool("ignoreAttribute", false);
    existence = c.getBool("existence", false);
    existence = c.getBool("order", true);
  }

  /**
   * checks format of string in datafile and converts to float If the attribute
   * is missing it creates a new real attribute, that is not defined.
   */
  public Attribute str2Att(String string) throws BadAttException {
    if (string == null) return (new RealAttribute());
    string = string.trim();
    if (string.length() == 0) return (new RealAttribute());
    double att = 0.; // initialized because try complains otherwise.
    try {
      att = Double.parseDouble(string);
    }
    catch (NumberFormatException e) {
      System.err.println(string + " is not a float.");
      throw new BadAttException(string + " is not a float", 0, 0);
    }
    return new RealAttribute(att);
  }

  public String toString() {
    String retval = new String(attributeName);
    retval += " " + attributeClass.getName();
    retval += " crucial: " + crucial;
    retval += " ignoreAttribute: " + ignoreAttribute;
    retval += " existence: " + existence;
    retval += " order: " + order;
    return (retval);
  }

  public String toString(Attribute attr) {
    String retval = new String();
    if (attr == null) return ("undefined");
    retval += ((RealAttribute) attr).toString();
    return (retval);
  }
}
