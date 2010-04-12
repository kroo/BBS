package jboost.examples;

/** Represents a single real valued attribute */
public class BooleanAttribute extends Attribute {

  private boolean value;

  public BooleanAttribute(boolean value) {
    this.value = value;
    setDefined();
  }

  public BooleanAttribute() {
    defined = false;
  }

  public boolean getValue() {
    return value;
  }

  public String toString() {
    return this.isDefined() ? String.valueOf(value) : "undefined";
  }
}
