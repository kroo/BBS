/*
 * Created on Jan 4, 2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package jboost.controller;

import junit.framework.TestCase;

/**
 * @author cschavis To change the template for this generated type comment go to
 *         Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class ConfigurationTest extends TestCase {

  private String m_commandline; // command line config
  private Configuration m_config;
  private static int INT = 9;
  private static String STRING = "testString";
  private static double DOUBLE = -0.89;
  private static int[] INT_ARRAY = { 0, 1, 2, 3 };
  private static double[] DOUBLE_ARRAY = { 0.9, 1.9, -2.0, 3.9 };
  private static String FLAG = "Flag";
  private static String[] STRING_ARRAY = { "foo", "bar", "dum", "my" };

  /*
   * @see TestCase#setUp()
   */
  protected void setUp() throws Exception {
    super.setUp();
    m_config = new Configuration();
    m_commandline = "-Integer 9 -String testString -Double -0.89 " + "-IntArray 0,1,2,3 -DoubleArray 0.9,1.9,-2.0,3.9 " + "-StringArray foo,bar,dum,my -Flag";
  }

  /*
   * @see TestCase#tearDown()
   */
  protected void tearDown() throws Exception {
    super.tearDown();
  }

  public final void testUnused() {
    // TODO Implement unused().
    System.out.println(m_config.unused());
  }

  /**
   * Test parseArgv() make sure that parseArgv() throws the expected exceptions
   */
  public final void testParseArgv() {
    try {
      String[] noDash = { "foo", "bar" };
      m_config.parseArgv(noDash);
      fail("Expected a BadCommandException to be thrown for option name that does not begin with '-'.");
    }
    catch (BadCommandException success) {
    }

    try {
      String[] badName = { "234", "653" };
      m_config.parseArgv(badName);
      fail("Expected a BadCommandException to be thrown for option name that starts with a number.");
    }
    catch (BadCommandException success) {
    }

  }

  /**
   * Test parseCommandString() which uses the parseStringTokenizer() This test
   * checks getBool, getInt(), getString(), getDouble(), getIntArray()
   * getDoubleArray() and getStringArray()
   */
  public final void testParseCommandString() {
    int[] dummy_ints = { 0, 0, 0, 0 };
    double[] dummy_doubles = { 0.0, 0.0, 0.0, 0.0 };
    String[] dummy_strs = { "dum", "my", "foo", "bar" };
    m_config.parseCommandString(m_commandline);

    int[] ints = m_config.getIntArray("IntArray", dummy_ints);
    double[] doubles = m_config.getDoubleArray("DoubleArray", dummy_doubles);
    int[] garbints = m_config.getIntArray("dummy", dummy_ints);
    double[] garbdoubs = m_config.getDoubleArray("dummy", dummy_doubles);
    String[] strs = m_config.getStringArray("StringArray", dummy_strs);
    String[] garbstrs = m_config.getStringArray("dummy", dummy_strs);

    assertEquals(m_config.getBool("Flag", false), true);
    assertEquals(m_config.getBool("Dummy", true), true);
    assertEquals(m_config.getInt("Integer", 0), INT);
    assertEquals(m_config.getInt("dummy", 10), 10);
    assertEquals(m_config.getString("String", ""), STRING);
    assertEquals(m_config.getString("dummy", "dummy"), "dummy");
    assertEquals(m_config.getDouble("Double", 0), DOUBLE, 0.0001);
    assertEquals(m_config.getDouble("dummy", 13.13), 13.13, 0.0001);
    for (int i = 0; i < 4; i++) {
      assertEquals(ints[i], INT_ARRAY[i]);
      assertEquals(doubles[i], DOUBLE_ARRAY[i], 0.0001);
      assertEquals(strs[i], STRING_ARRAY[i]);
      assertEquals(garbints[i], dummy_ints[i]);
      assertEquals(garbdoubs[i], dummy_doubles[i], 0.0001);
      assertEquals(garbstrs[i], dummy_strs[i]);
    }
  }

  /**
   * Test addOption(), addValid(), isValid(), safeAddOption()
   */
  public final void testAddOption() {

    m_config.addOption("foo", "carl");
    assertEquals(m_config.getString("foo", "dummy"), "carl");
    assertEquals(m_config.isValid("foo"), false);
    m_config.addValid("foo");
    assertTrue(m_config.isValid("foo"));
    try {
      m_config.safeAddOption("bar", "joe");
      fail("Expected an Exception to be thrown for unsafe option addition.");
    }
    catch (Exception success) {
    }

    try {
      m_config.safeAddOption("foo", "barney");
    }
    catch (Exception e) {
      fail("Unexpected exception.");
    }

  }

}
