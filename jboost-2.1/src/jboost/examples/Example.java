package jboost.examples;

/**
 * The Example class holds a single Instance of an example, along with its
 * Label.
 */
public class Example {

  /** array of the attributes, null if attribute is undefined */
  protected Instance m_instance;
  /** a discrete-valued label */
  protected Label m_label;
  /** a private weight for this example */
  protected double m_weight;
  /** a textual comment */
  protected String m_comment;
  /** INDEX */
  protected int m_index;
  /** a pointer to the exampleDescription */
  protected ExampleDescription m_exampleDescription;

  /**
   * Default ctor
   * 
   * @param attArray
   * @param label
   * @param ed
   */
  public Example(Attribute[] attArray, int index, Label label, double weight, ExampleDescription ed) {
    m_instance = new Instance(attArray);
    m_label = label;
    m_weight = weight;
    m_index = index;
    m_exampleDescription = ed;
  }

  /**
   * This ctor defaults the ExampleDescriptions to null
   * 
   * @param attArray
   * @param label
   */
  public Example(Attribute[] attArray, Label label) {
    this(attArray, -1, label, 1.0, null);
  }

  /** set instance */
  public void setInstance(Instance instance) {
    m_instance = instance;
  }

  /** return the whole instance (array of attributes) */
  public Instance getInstance() {
    return m_instance;
  }

  /** returns the i'th attribute (null if undefined) */
  public Attribute getAttribute(int i) {
    return m_instance.attribute[i];
  }

  public Label getLabel() {
    return (m_label);
  }

  public void setLabel(Label label) {
    m_label = label;
  }

  /**
   * Get the INDEX of this example
   * 
   * @return INDEX
   */
  public int getIndex() {
    return m_index;
  }

  /**
   * Set the INDEX of this example
   * 
   * @param INDEX
   */
  public void setIndex(int index) {
    m_index = index;
  }

  /**
   * Get the weight of this example
   * 
   * @return weight
   */
  public double getWeight() {
    return m_weight;
  }

  /**
   * Set the weight of this example
   * 
   * @param weight
   */
  public void setWeight(double weight) {
    m_weight = weight;
  }

  public void setDescription(ExampleDescription ed) {
    m_exampleDescription = ed;
  }

  public ExampleDescription getDescription() {
    return m_exampleDescription;
  }

  public String toString() {
    return "instance=" + m_instance.toString(m_exampleDescription) + " label=" + m_label.toString() + "\n";
  }

}
