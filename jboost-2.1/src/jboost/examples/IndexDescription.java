package jboost.examples;

import jboost.controller.Configuration;
import jboost.tokenizer.BadAttException;

/**
 * The description for a private weight of each example.
 */
public class IndexDescription extends IntegerDescription {

  public IndexDescription(String name, Configuration config) throws ClassNotFoundException {
    super(name, config);
  }

  /**
   * Reads an id if anything goes wrong, return -1
   * 
   * @param id
   *            the string representation of the id
   * @return Attribute the IntegerAttribute corresponding to the id
   */
  public Attribute str2Att(String id) throws BadAttException {

    int att = -1; // initialized because try complains otherwise.

    if (id != null) {
      id = id.trim();
      if (id.length() != 0) {
        try {
          att = Integer.parseInt(id);
        }
        catch (NumberFormatException nfe) {
          throw new BadAttException(id + " is not an integer", 0, 0);
        }
      }
    }
    return new IntegerAttribute(att);
  }

}
