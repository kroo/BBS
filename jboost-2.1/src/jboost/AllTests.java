/*
 * Created on Jan 7, 2004
 *
 */
package jboost;

import jboost.atree.AtreeTestSuite;
import jboost.booster.BoosterTestSuite;
import jboost.controller.ControllerTestSuite;
import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * @author cschavis
 */
public class AllTests {

  public static Test suite() {
    TestSuite suite = new TestSuite("Test for jboost");
    // $JUnit-BEGIN$
    suite.addTest(BoosterTestSuite.suite());
    suite.addTest(ControllerTestSuite.suite());
    suite.addTest(AtreeTestSuite.suite());
    // $JUnit-END$
    return suite;
  }
}
