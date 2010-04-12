/*
 * Created on Jan 26, 2004
 *
 */
package jboost.atree;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * @author cschavis
 */
public class AtreeTestSuite {

  public static Test suite() {
    TestSuite suite = new TestSuite("Test for jboost.atree");
    // $JUnit-BEGIN$
    suite.addTestSuite(InstrumentedAlternatingTreeTest.class);
    suite.addTestSuite(AlternatingTreeTest.class);
    // $JUnit-END$
    return suite;
  }
}
