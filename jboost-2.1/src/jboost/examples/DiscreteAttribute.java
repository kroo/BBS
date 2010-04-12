package jboost.examples;

import jboost.monitor.Monitor;

/** Discrete-valued attribute */
public class DiscreteAttribute extends Attribute {

  private int value;

  public DiscreteAttribute(int value) {
    this.value = value;
    setDefined();
  }

  public DiscreteAttribute() {
    defined = false;
  }

  public int getValue() {
    return value;
  }

  public String toString() {
    if (isDefined() == false) if (Monitor.logLevel > 3) Monitor.log("UNDEFINED");
    return this.isDefined() ? String.valueOf(value) : "undefind";
  }
}
