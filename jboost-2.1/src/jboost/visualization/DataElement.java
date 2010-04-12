/**
 * 
 */
package jboost.visualization;

/**
 * @author yoavfreund
 */
public class DataElement implements Comparable {

  protected double value;
  protected int label;
  protected int index;
  protected double falsePositives, truePositives;

  public DataElement() {
  }

  public DataElement(double value) {
    this.value = value;
  }

  public DataElement(double value, int index, int label) {
    this.value = value;
    this.index = index;
    this.label = label;
  }

  public String toString() {
    String s =
        "[index=" + Integer.toString(index) + ",value=" + Double.toString(value) + ",label=" + Integer.toString(label) + ",TP="
            + Double.toString(truePositives) + ",FP=" + Double.toString(falsePositives) + "]";
    return s;
  }

  public int compareTo(Object that) {
    return (int) Math.signum(this.value - ((DataElement) that).value);
  }

}
