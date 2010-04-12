package jboost.learner;

/**
 * This class contains summary information about simple base hypotheses. It
 * ought to be useful for spitting out efficient code.
 * 
 * @author Nigel Duffy
 */
public class Summary {

  public char type;
  public int index;

  public Object val;

  public static final char EQUALITY = 1;
  public static final char LESS_THAN = 2;
  public static final char CONTAINS_ABSTAIN = 3;
  public static final char CONTAINS_NOABSTAIN = 4;
}
