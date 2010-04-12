package jboost.examples;

import jboost.controller.Configuration;
import jboost.tokenizer.BadAttException;

/**
 * The description for a private weight of each example.
 */
public class WeightDescription extends NumberDescription {

  public static final double MAX_WEIGHT = 1;

  WeightDescription(String name, Configuration config) throws ClassNotFoundException {
    super(name, config);
  }

  /**
   * Reads a RealAttribute as an example weight If no value is specified,
   * default to 1 If the value is negative, return 0 as the weight. If the value
   * is greater than 1, return 1.
   * 
   * @param weight
   *            the string representation of the weight
   * @return Attribute the RealAttribute corresponding to the weight
   */
  public Attribute str2Att(String weight) throws BadAttException {
    double att = 1.0; // initialized because try complains otherwise.

    if (weight != null) {
      weight = weight.trim();
      if (weight.length() != 0) {
        try {
          att = Double.parseDouble(weight);
        }
        catch (NumberFormatException nfe) {
          throw new BadAttException(weight + " is not a float", 0, 0);
        }
        if (att < 0) {
          att = 0;
          throw new BadAttException("The weight: " + weight + " is less than 0", 0, 0);
        }
        /*
         * if (att > MAX_WEIGHT) { att= MAX_WEIGHT; throw new
         * BadAttException("The weight: " + weight + " is larger than
         * MAX_WEIGHT=" + MAX_WEIGHT,0,0); }
         */
      }
    }
    return new RealAttribute(att);
  }

}
