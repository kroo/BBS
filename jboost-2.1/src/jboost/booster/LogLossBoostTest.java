/*
 * Created on Dec 29, 2003
 */
package jboost.booster;

import jboost.examples.Label;

/**
 * @author cschavis
 */
public class LogLossBoostTest extends AbstractBoosterTest {

  LogLossBoost m_logLoss;

  /**
   * @param arg0
   */
  public LogLossBoostTest(String arg0) {
    super(arg0);
    // TODO Auto-generated constructor stub
  }

  /*
   * @see TestCase#setUp()
   */
  protected void setUp() throws Exception {
    m_odd = new LogLossBoost();
    m_even = new LogLossBoost();
    m_allTrue = new LogLossBoost();
    m_allFalse = new LogLossBoost();
    m_solitaires = new LogLossBoost[COUNT];
    for (int i = 0; i < COUNT; i++) {
      m_solitaires[i] = new LogLossBoost();
    }
    super.setUp();
    m_logLoss = new LogLossBoost();
  }

  public final void testFinalizeData() {
    // TODO Implement finalizeData().
  }

  public final void testUpdate() {
    // TODO Implement update().
    // fill in booster
    int[] indices = new int[COUNT];
    int[] ones = new int[COUNT / 2];
    int[] zeroes = new int[COUNT / 2];
    for (int i = 0; i < COUNT / 2; i++) {
      m_logLoss.addExample(i, new Label(0));
      indices[i] = i;
      zeroes[i] = i;
    }
    for (int i = COUNT / 2, j = 0; i < COUNT; i++, j++) {
      m_logLoss.addExample(i, new Label(1));
      indices[i] = i;
      ones[j] = i;
    }
    m_logLoss.finalizeData();
    for (int i = 0; i < zeroes.length; i++) {
      zeroes[i] = indices[COUNT / 2 + i];
      Prediction p1 = m_logLoss.getPrediction(m_logLoss.newBag(zeroes));
      m_logLoss.update(new Prediction[] { p1 }, new int[][] { zeroes });
      for (int j = 0; j < zeroes.length; j++) {
        int index = zeroes[j];
        double weight = m_logLoss.m_weights[index];
        double margin = m_logLoss.m_margins[index];
        assertEquals(weight, 1 / (1 + Math.exp(margin)), 0.00001);
      }
    }

    for (int i = 0; i < ones.length; i++) {
      ones[i] = indices[COUNT / 2 + i];
      Prediction p1 = m_logLoss.getPrediction(m_logLoss.newBag(ones));
      m_logLoss.update(new Prediction[] { p1 }, new int[][] { ones });
      for (int j = 0; j < ones.length; j++) {
        int index = ones[j];
        double weight = m_logLoss.m_weights[index];
        double margin = m_logLoss.m_margins[index];
        assertEquals(weight, 1 / (1 + Math.exp(margin)), 0.00001);
      }
    }
  }

  public final void testGetBound() {
    // TODO Implement getBound().
  }

}
