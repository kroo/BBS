/*
 * Created on Dec 29, 2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package jboost.booster;

import jboost.examples.Label;

/**
 * @author cschavis To change the template for this generated type comment go to
 *         Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class AdaBoostTest extends AbstractBoosterTest {

  AdaBoost m_adaBoost;

  /**
   * Constructor for AdaBoostTest.
   * 
   * @param arg0
   */
  public AdaBoostTest(String arg0) {
    super(arg0);
  }

  /*
   * @see TestCase#setUp()
   */
  protected void setUp() throws Exception {
    m_odd = new AdaBoost();
    m_even = new AdaBoost();
    m_allTrue = new AdaBoost();
    m_allFalse = new AdaBoost();
    m_adaBoost = new AdaBoost();
    m_solitaires = new AdaBoost[COUNT];
    for (int i = 0; i < COUNT; i++) {
      m_solitaires[i] = new AdaBoost();
    }
    super.setUp();
  }

  final public void testAddExample() {
    // TODO Implement addExample().
  }

  final public void testFinalizeData() {
    // TODO Implement finalizeData().
  }

  final public void testClear() {
    // TODO Implement clear().
  }

  /*
   * Test for Bag newBag(int[])
   */
  final public void testNewBagintArray() {
    // TODO Implement newBag().
  }

  /*
   * Test for Bag newBag()
   */
  final public void testNewBag() {
    // TODO Implement newBag().
  }

  /*
   * Test for Bag newBag(Bag)
   */
  final public void testNewBagBag() {
    // TODO Implement newBag().
  }

  public final void testUpdate() {
    // TODO Implement update().
    // fill in booster
    int[] indices = new int[COUNT];
    int[] ones = new int[COUNT / 2];
    int[] zeroes = new int[COUNT / 2];
    for (int i = 0; i < COUNT / 2; i++) {
      m_adaBoost.addExample(i, new Label(0));
      indices[i] = i;
      zeroes[i] = i;
    }
    for (int i = COUNT / 2, j = 0; i < COUNT; i++, j++) {
      m_adaBoost.addExample(i, new Label(1));
      indices[i] = i;
      ones[j] = i;
    }
    m_adaBoost.finalizeData();
    for (int i = 0; i < zeroes.length; i++) {
      zeroes[i] = indices[COUNT / 2 + i];
      Prediction p1 = m_adaBoost.getPrediction(m_adaBoost.newBag(zeroes));
      m_adaBoost.update(new Prediction[] { p1 }, new int[][] { zeroes });
      for (int j = 0; j < zeroes.length; j++) {
        int index = zeroes[j];
        double weight = m_adaBoost.m_weights[index];
        double margin = m_adaBoost.m_margins[index];
        assertEquals(weight, Math.exp(-margin), 0.00001);
      }
    }

    for (int i = 0; i < ones.length; i++) {
      ones[i] = indices[COUNT / 2 + i];
      Prediction p1 = m_adaBoost.getPrediction(m_adaBoost.newBag(ones));
      m_adaBoost.update(new Prediction[] { p1 }, new int[][] { ones });
      for (int j = 0; j < ones.length; j++) {
        int index = ones[j];
        double weight = m_adaBoost.m_weights[index];
        double margin = m_adaBoost.m_margins[index];
        assertEquals(weight, Math.exp(-margin), 0.00001);
      }
    }
  }

  final public void testGetPredictions() {
    // TODO Implement getPredictions().
  }

  final public void testGetTheoryBound() {
    // TODO Implement getTheoryBound().
  }

  final public void testGetMargins() {
    // TODO Implement getMargins().
  }

  /*
   * Test for void AdaBoost()
   */
  final public void testAdaBoost() {
    // TODO Implement AdaBoost().
  }

  /*
   * Test for void AdaBoost(double)
   */
  final public void testAdaBoostdouble() {
    // TODO Implement AdaBoost().
  }

  final public void testGetPrediction() {
    // TODO Implement getPrediction().
  }

}
