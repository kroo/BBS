/*
 * Created on Feb 8, 2004
 */
package jboost.learner;

import java.io.Serializable;

/**
 * @author cschavis
 */
public class SplitterType implements Serializable {

  private final String m_name;

  /**
   * Private ctor insures that no one can create any more Splitter types.
   * 
   * @param name
   *            of the type
   */
  private SplitterType(String name) {
    m_name = name;
  }

  /**
   * Return true if this SplitterType is equal to the other SplitterType
   * 
   * @param other
   * @return result of comparing the String names of this and the other
   */
  public boolean equals(SplitterType other) {
    return m_name.equals(other.toString());
  }

  /**
   * @return name of this type
   */
  public String toString() {
    return m_name;
  }

  public static final SplitterType EQUALITY_SPLITTER = new SplitterType("Equality Splitter");
  public static final SplitterType INEQUALITY_SPLITTER = new SplitterType("Inequality Splitter");
  public static final SplitterType SET_SPLITTER = new SplitterType("Set Splitter");

}
