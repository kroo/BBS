package jboost.examples;

/** Represents a single real valued attribute */
public class RealAttribute extends Attribute {

  private double value;

  public RealAttribute(double value) {
    this.value = value;
    setDefined();
  }

  public RealAttribute() {
    defined = false;
  }

  public double getValue() {
    return value;
  }

  public String toString() {
    return this.isDefined() ? String.valueOf(value) : "undefined";
  }
}
