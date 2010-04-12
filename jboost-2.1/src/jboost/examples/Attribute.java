package jboost.examples;

import jboost.monitor.Monitor;

/** holds value of a single attribute of an Instance */
public class Attribute {

  public static void main(String[] argv) {
    Attribute a = new RealAttribute(9.5);
    if (Monitor.logLevel > 3) Monitor.log("The className is " + a.getClass().getName());
  }

  /** default constructor for the base At.tribute class */
  public Attribute() {
    defined = false;
  }

  /**
   * a flag indicating whether this attribute value is defined for the example
   */
  protected boolean defined; // is attribute defined for example

  public void setDefined() {
    defined = true;
  }

  public boolean isDefined() {
    return defined;
  }
}
