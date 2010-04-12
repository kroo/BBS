package jboost.examples;

/** Holds the input features of an Example */
public class Instance {

  /** an array of the attributes, null if attribute is undefined */
  protected Attribute[] attribute;

  /** constructor */
  public Instance(Attribute[] attArray) {
    attribute = attArray;
  }

  /** returns the number of attributes */
  public int getSize() {
    return attribute.length;
  }

  /** returns true if attribute i is defined */
  public boolean isDefined(int i) {
    return attribute[i].isDefined();
  }

  /** returns the i'th attribute (null if undefined) */
  public Attribute getAttribute(int i) {
    if (isDefined(i)) {
      return attribute[i];
    }
    else {
      return null;
    }
  }

  public String toString() {
    String string = new String();
    for (int i = 0; i < attribute.length; i++)
      string += attribute[i] + ((i < (attribute.length - 1)) ? "," : ";");

    return string;
  }

  public String toString(ExampleDescription ed) {
    String s = new String();
    for (int i = 0; i < attribute.length; i++) {
      s += ed.getAttributeDescription(i).getAttributeName() + ":";
      s += attribute[i] + "\n";
      // s += ((i == attribute.length-1) ? "; " : ", ");
    }

    return s;
  }
}
