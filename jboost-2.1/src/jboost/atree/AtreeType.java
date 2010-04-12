/*
 * Created on Jan 24, 2004
 *
 */
package jboost.atree;

/**
 * This class represents the different types of Alternating Decision Trees. It
 * uses the typesafe enum pattern to provide an enum-like structure with all the
 * possible types of ADTrees.
 * 
 * @author cschavis
 */
public class AtreeType {

  private final String m_name;

  /**
   * Private ctor insures that no one can create any more ADTree types.
   * 
   * @param name
   *            of the type
   */
  private AtreeType(String name) {
    m_name = name;
  }

  public String toString() {
    return m_name;
  }

  public static final AtreeType ADD_ALL = new AtreeType("ADD_ALL");
  public static final AtreeType ADD_ROOT = new AtreeType("ADD_ROOT");
  public static final AtreeType ADD_SINGLES = new AtreeType("ADD_SINGLES");
  public static final AtreeType ADD_ROOT_OR_SINGLES = new AtreeType("ADD_ROOT_OR_SINGLES");

}
