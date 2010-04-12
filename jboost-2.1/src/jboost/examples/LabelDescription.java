package jboost.examples;

import java.util.List;
import java.util.StringTokenizer;

import jboost.controller.Configuration;
import jboost.tokenizer.BadAttException;
import jboost.tokenizer.StringOp;

/**
 * the description for multi-finite attributes.
 */
public class LabelDescription extends FiniteDescription {

  LabelDescription(String name, Configuration c, List okValues) throws ClassNotFoundException {
    super(name, c, okValues);
    attributeClass = Class.forName("jboost.examples.LabelDescription");
    multiLabel = c.getBool("multiLabel", false);
  }

  private boolean multiLabel;

  public boolean isMultiLabel() {
    return multiLabel;
  }

  public String toString() {
    return super.toString() + " multiLabel: " + multiLabel;
  }

  /*
   * public String toString(Attribute attr) throws Exception { String retval=new
   * String(); boolean[] tok=((MultiDiscreteAttribute)attr).getValue();
   * if(tok==null) { int tok1=((MultiDiscreteAttribute)attr).getSingleValue();
   * if(tok1>attributeValues.size()) throw(new Exception("More attribute values
   * than allowed")); retval+=(String)attributeValues.get(tok1); } else {
   * if(tok.length>attributeValues.size()) throw(new Exception("More attribute
   * values than allowed")); for(int i=0;i<tok.length;i++) { if(tok[i]==true)
   * retval+=(String)attributeValues.get(i); // This should be improved when the
   * reader is integrated. } } return(retval); }
   */

  /**
   * Reads a MultiFinite attribute Note that it is currently based on String but
   * should use StringBuffer Also note that right now it can only deal with one
   * value.
   */
  public Attribute str2Att(String string) throws BadAttException {
    if (string == null) return new Label(null);

    StringTokenizer st = new StringTokenizer(string);
    if (!multiLabel && (st.countTokens() != 1)) throw new BadAttException((st.countTokens() == 0 ? "Zero" : "Multiple")
                                                                          + " labels found when expecting single label: " + string);
    String s;
    boolean v[] = new boolean[getNoOfValues()];
    while (st.hasMoreElements()) {
      s = st.nextToken();
      if (!caseSignificant) s = s.toLowerCase();
      if (!punctuationSignificant) s = StringOp.removePunctuation(s);
      if (map.containsKey(s)) v[((Integer) map.get(s)).intValue()] = true;
      else if (map.containsKey("*")) v[((Integer) map.get("*")).intValue()] = true;
      else throw (new BadAttException("Unknown label: " + s + " when not allowed."));
    }
    return new Label(v);
  }
}
