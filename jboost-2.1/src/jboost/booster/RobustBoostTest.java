package jboost.booster;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.StringTokenizer;

import jboost.examples.Label;
import junit.framework.TestCase;

/**
 * @author Sunsern Cheamanunkul To change the template for this generated type
 *         comment go to Window&gt;Preferences&gt;Java&gt;Code
 *         Generation&gt;Code and Comments
 */
public class RobustBoostTest extends AbstractBoosterTest {

  RobustBoost m_robustBoost;

  /**
   * Constructor for RobustBoostTest.
   * 
   * @param arg0
   */
  public RobustBoostTest(String arg0) {
    super(arg0);
  }

  /**
   * Tests the RobustBoost constructor and sets up boosters for other tests.
   * 
   * @see TestCase#setUp()
   */
  protected void setUp() throws Exception {
    m_odd = new RobustBoost();
    m_even = new RobustBoost();
    m_allTrue = new RobustBoost();
    m_allFalse = new RobustBoost();
    m_robustBoost = new RobustBoost();
    m_solitaires = new RobustBoost[COUNT];
    for (int i = 0; i < COUNT; i++) {
      m_solitaires[i] = new RobustBoost();
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

  final public void testGetPrediction() {
    // TODO Implement getPrediction().
  }

  final public void testLongData() {

    int[] best = new int[] { 1, 2, 10, 12, 1, 7, 4, 17, 8, 1, 9, 2, 6, 15, 3, 20, 19, 13, 16, 14 };
    double[] times =
        new double[] { 0.125, 0.172, 0.189, 0.203, 0.212, 0.226, 0.234, 0.240, 0.246, 0.252, 0.256, 0.260, 0.264, 0.268, 0.271, 0.273, 0.276, 0.278, 0.2791,
                      0.280 };

    int numFeatures = 21;
    int numExamples = 800;

    try {

      int[][] data = new int[numExamples][numFeatures];
      int[] labels = new int[numExamples];

      RobustBoost rBoost = new RobustBoost(false, 0.14, new double[] { 0.2, 0.2 }, new double[] { 0.1, 0.1 }, new double[] { 1, 1 });

      assertEquals(rBoost.m_epsilon, 0.14, 0.0001);
      assertEquals(rBoost.m_theta[0], 0.2, 0.0001);
      assertEquals(rBoost.m_sigma_f[0], 0.1, 0.0001);
      assertEquals(rBoost.m_rho[0], 0.7233, 0.0001);

      BufferedReader br = new BufferedReader(new FileReader("demo/Long.train"));
      // read input
      String line = br.readLine();
      int j = 0;
      while (line != null) {

        StringTokenizer st = new StringTokenizer(line, ",;");

        labels[j] = Math.round(Float.parseFloat(st.nextToken()));

        int i = 0;
        while (st.hasMoreTokens()) {
          String s = st.nextToken();
          data[j][i] = Math.round(Float.parseFloat(s));
          assertEquals(data[j][i] == -1 || data[j][i] == 1, true);
          i++;
        }
        j++;
        line = br.readLine();
      }
      br.close();

      for (int i = 0; i < numExamples; i++) {
        assertEquals((labels[i] == -1) || (labels[i] == 1), true);

        if (labels[i] == -1) labels[i] = 0;

        rBoost.addExample(i, new Label(labels[i]));
      }

      rBoost.finalizeData();
      int iter = 0;
      while (iter < 20 && !rBoost.isFinished()) {

        int[][] best_lists = new int[][] { null, null };
        int best_feature = -1;
        double best_gain = -1;

        // find the best weak rule
        for (int i = 0; i < numFeatures; i++) {

          double gain, gain0 = 0, gain1 = 0;
          int count0 = 0, count1 = 0, p = 0;
          for (int k = 0; k < numExamples; k++) {

            if (data[k][i] == -1) count0++;
            else count1++;

            if (labels[k] == 0 && data[k][i] == -1) {
              gain0 += rBoost.m_weights[k];
            }
            else if (labels[k] == 0 && data[k][i] == 1) {
              gain0 -= rBoost.m_weights[k];
            }

            if (labels[k] == 1 && data[k][i] == 1) {
              gain1 += rBoost.m_weights[k];
            }
            else if (labels[k] == 1 && data[k][i] == -1) {
              gain1 -= rBoost.m_weights[k];
            }

          }

          gain = gain0 + gain1;

          int[] list0 = new int[count0];
          int[] list1 = new int[count1];
          int list0_idx = 0, list1_idx = 0;
          for (int k = 0; k < numExamples; k++) {
            if (data[k][i] == -1) list0[list0_idx++] = k;
            else if (data[k][i] == 1) list1[list1_idx++] = k;
          }

          gain = Math.abs(gain);

          if (gain > best_gain) {
            best_feature = i;
            best_gain = gain;
            best_lists[0] = list0;
            best_lists[1] = list1;
          }
        }

        Bag[] best_bags = new Bag[2];
        best_bags[0] = rBoost.newBag(best_lists[0]);
        best_bags[1] = rBoost.newBag(best_lists[1]);

        int[][] exampleIndex = new int[2][];
        exampleIndex[0] = best_lists[0];
        exampleIndex[1] = best_lists[1];

        Prediction[] predictions = rBoost.getPredictions(best_bags, exampleIndex);
        rBoost.update(predictions, exampleIndex);

        assertEquals(best_feature + 1, best[iter]);
        assertEquals(rBoost.m_t, times[iter], 0.001);

        iter++;
      }

    }
    catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

  }

}
