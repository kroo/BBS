package jboost.util;

import java.util.Arrays;

/** A class holding miscellaneous utility functions */
public class Util {

  /**
   * A generic intersect function that
   * 
   * @param mask
   *            An examples mask
   * @param examples
   *            The list of examples to intersect with
   * @param intersection
   *            The examples mask with the intersection of the first 2
   *            arguments, must be the same size as mask.
   * @return The number of true elements in the intersection mask.
   */
  public static int intersect(boolean[] mask, int[] examples, boolean[] intersection) {
    if (intersection.length != mask.length) throw new RuntimeException("Argument and return value have different length.");
    Arrays.fill(intersection, false);
    int count = 0;
    for (int i = 0; i < examples.length; i++)
      if (mask[examples[i]]) {
        intersection[examples[i]] = true;
        count++;
      }
    return (count);
  }

  /**
   * Determines whether an integer is even.
   */
  public static boolean even(int i) {
    return (i / 2 * 2 == i);
  }

  /**
   * Determines whether an integer is odd.
   */
  public static boolean odd(int i) {
    return (i / 2 * 2 != i);
  }
}
