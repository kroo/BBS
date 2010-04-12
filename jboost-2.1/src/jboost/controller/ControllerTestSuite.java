/*
 * Created on Jan 4, 2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package jboost.controller;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * @author cschavis To change the template for this generated type comment go to
 *         Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class ControllerTestSuite {

  public static Test suite() {
    TestSuite suite = new TestSuite("Test for jboost.controller");
    // $JUnit-BEGIN$
    suite.addTestSuite(ConfigurationTest.class);
    suite.addTestSuite(ControllerTest.class);
    // $JUnit-END$
    return suite;
  }
}
