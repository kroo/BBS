/*
 * Created on Dec 29, 2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package jboost.booster;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * @author cschavis To change the template for this generated type comment go to
 *         Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class BoosterTestSuite {

  public static Test suite() {
    TestSuite suite = new TestSuite("Test for jboost.booster");
    // $JUnit-BEGIN$
    suite.addTest(new TestSuite(AdaBoostTest.class));
    suite.addTest(new TestSuite(LogLossBoostTest.class));
    suite.addTest(new TestSuite(RobustBoostTest.class));
    // $JUnit-END$
    return suite;
  }

}
