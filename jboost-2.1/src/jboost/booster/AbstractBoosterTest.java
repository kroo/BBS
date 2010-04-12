/*
 * Created on Dec 16, 2003
 */
package jboost.booster;

import jboost.examples.Label;
import junit.framework.TestCase;

/**
 * @author cschavis
 */
public abstract class AbstractBoosterTest extends TestCase {

  protected boolean[] m_splits;
  protected int[] m_examples;
  protected AbstractBooster m_odd;
  protected AbstractBooster m_even;
  protected AbstractBooster m_allTrue;
  protected AbstractBooster m_allFalse;
  protected AbstractBooster[] m_solitaires;
  protected Bag[] m_bags;
  protected int m_index;
  protected Bag m_first;
  protected Bag m_second;

  protected static int COUNT = 10;

  /**
   * Constructor for AbstractBoosterTest.
   * 
   * @param arg0
   */
  public AbstractBoosterTest(String arg0) {
    super(arg0);
  }

  /**
   * Set up the booster for testing
   */
  protected void setUp() throws Exception {
    m_examples = new int[COUNT];
    m_splits = new boolean[COUNT];
    m_index = -1;
    m_first = m_odd.newBag();
    m_second = m_odd.newBag();

    for (int i = 0; i < COUNT; i++) {
      m_allFalse.addExample(i, new Label(0));
      m_allTrue.addExample(i, new Label(1));
      if ((i % 2) == 0) {
        m_odd.addExample(i, new Label(0));
        m_even.addExample(i, new Label(1));
      }
      else {
        m_odd.addExample(i, new Label(1));
        m_even.addExample(i, new Label(0));
      }

      for (int j = 0; j < COUNT; j++) {
        if (i == j) {
          m_solitaires[i].addExample(j, new Label(0));
        }
        else {
          m_solitaires[i].addExample(j, new Label(1));
        }
      }
      m_solitaires[i].finalizeData();
      m_examples[i] = i;
      m_splits[i] = false;
    }
    m_allTrue.finalizeData();
    m_allFalse.finalizeData();
    m_odd.finalizeData();
    m_even.finalizeData();
  }

  /**
   * Reset the booster
   */
  protected void tearDown() {
    m_odd.clear();
    m_even.clear();
    m_allTrue.clear();
    m_allFalse.clear();
    for (int i = 0; i < COUNT; i++) {
      m_solitaires[i].clear();
    }
  }

  final public void testSplitAllTrue() {
    for (int i = 0; i < COUNT; i++) {
      m_splits[i] = true;
      m_index = m_allTrue.findBestSplit(m_first, m_second, m_examples, m_splits);
      assertEquals(m_index, 0);
      m_splits[i] = false;
    }
  }

  final public void testSplitAllFalse() {
    for (int i = 0; i < COUNT; i++) {
      m_splits[i] = true;
      m_index = m_allFalse.findBestSplit(m_first, m_second, m_examples, m_splits);
      assertEquals(m_index, 0);
      m_splits[i] = false;
    }
  }

  final public void testSplitOdd() {
    for (int i = 0; i < COUNT; i++) {
      m_splits[i] = true;
      m_index = m_odd.findBestSplit(m_first, m_second, m_examples, m_splits);
      if ((i % 2) == 0) {
        assertEquals(m_index, 0);
      }
      else {
        assertEquals(m_index, i);
      }
      m_splits[i] = false;
    }
  }

  final public void testSplitEven() {
    for (int i = 0; i < COUNT; i++) {
      m_splits[i] = true;
      m_index = m_even.findBestSplit(m_first, m_second, m_examples, m_splits);
      if ((i % 2) == 0) {
        assertEquals(m_index, 0);
      }
      else {
        assertEquals(m_index, i);
      }
      m_splits[i] = false;
    }
  }

  final public void testSplitSolitaires() {
    for (int i = 0; i < COUNT; i++) {
      m_splits[i] = true;
      m_index = m_solitaires[i].findBestSplit(m_first, m_second, m_examples, m_splits);
      assertEquals(m_index, i);
      m_splits[i] = false;
    }
  }

  final public void testGetLoss() {
    Bag[] bags = new Bag[COUNT];
    Bag[] half = new Bag[COUNT / 2];

    for (int i = 0; i < COUNT; i++) {
      bags[i] = m_odd.newBag(m_examples);
    }
    for (int i = 0; i < COUNT / 2; i++) {
      half[i] = m_odd.newBag(m_examples);
    }
    assertEquals(2 * m_odd.getLoss(half), m_odd.getLoss(bags), 0.0000001);
  }

}
