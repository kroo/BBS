package jboost.examples;

/** Represents a single real valued attribute */
public class IntegerAttribute extends Attribute {

  private int value;

  public IntegerAttribute(int value) {
    this.value = value;
    setDefined();
  }

  public IntegerAttribute() {
    defined = false;
  }

  public int getValue() {
    return value;
  }

  public String toString() {
    return this.isDefined() ? String.valueOf(value) : "undefined";
  }
}
