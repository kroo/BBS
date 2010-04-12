package jboost.tokenizer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import jboost.monitor.Monitor;

/**
 * Some string operations. Q: Can we extend String?
 */
public class StringOp {

  /** Determines whether a string contains any non-whitespace */
  public static boolean isBlank(String b) {
    if (b == null) return (true);
    b = b.trim();
    if (b.length() == 0) return (true);
    return (false);
  }

  /** Returns all text up to comment character, trimmed with whitespaces shrunk */
  public static String cleanLine(String a, String comment) {
    if (a == null) return (null);
    LTStringTokenizer lts = new LTStringTokenizer(a, comment);
    String retval = lts.next();
    if (retval == null) retval = a;
    retval = StringOp.shrinkWhitespace(retval);
    retval = retval.trim();
    if (StringOp.isBlank(retval)) return (null);
    else return (retval);
  }

  /**
   * Splits a string at each seperator and returns a list of the unique tokens,
   * trimmed between the seperators.
   */
  public static List toUniqList(String s, String seperator) {
    int numstr = 0;
    LTStringTokenizer lts = new LTStringTokenizer(s, seperator);
    HashMap map = new HashMap();
    List retval = new Vector();

    String str = lts.next();
    while (str != null) {
      str = str.trim();
      if (str.length() != 0 && !map.containsKey(str)) {
        map.put(str, new Integer(numstr));
        retval.add(str);
        numstr++;
      }
      str = lts.next();
    }

    str = lts.rest();
    if (str == null) throw (new RuntimeException("Last element in list succeeded by a " + seperator + ": " + s));
    str = str.trim();
    if (str.length() != 0 && !map.containsKey(str)) retval.add(str);
    return (retval);
  }

  /**
   * Replaces >= 1 white spaces by 1 simple space. Remarks: 1. All white spaces,
   * even a single one, converted to simple space. 2. Beginning and trailing
   * spaces also compressed into one (use String.trim to remove them
   * completely).
   * 
   * @param string
   *            to be shrunk
   * @return shrunk string
   */
  public static String shrinkWhitespace(String string) {

    char[] chaArr = string.toCharArray();
    int i, j;
    for (i = j = 0; j < chaArr.length; i++) {
      if (!Character.isWhitespace(chaArr[j])) {
        if (i < j) chaArr[i] = chaArr[j];
        j++;
      }
      else { // whitespace
        chaArr[i] = ' '; // forcing regular space
        while (++j < chaArr.length && Character.isWhitespace(chaArr[j]))
          ; // skipping subsequent white spaces
      }
    }
    return (new String(chaArr, 0, i));
  }

  /**
   * transforms all non letter to spaces and all letters to lower-case
   * 
   * @param string
   *            the string to be manipulated
   * @param toLower
   *            whether or not to transform the letters to lower-case
   * @return string with all non-letters changed to spaces
   */
  public static String toLowerCaseLetters(String string, boolean toLower) {
    char[] s = string.toCharArray();

    int j = 0; // counts the chars in the transformed string
    boolean firstSpace = true;
    for (int i = 0; i < s.length; i++) {
      if (!Character.isLetter(s[i])) {
        if (firstSpace) { // keep only the first space between
          // letter sequences
          firstSpace = false;
          s[j] = ' ';
          j++;
        }
      }
      else {
        firstSpace = true;
        if (toLower) s[j] = Character.toLowerCase(s[i]);
        else s[j] = s[i];
        j++;
      }
    }

    return new String(s, 0, j); // return a string of the transformed part
  }

  /**
   * Removes punctuation marks.
   * 
   * @param string
   *            whose punctuations need to be removed.
   * @return string after punctuation was removed.
   */
  public static String removePunctuation(String string) {

    char[] chaArr = string.toCharArray();
    int i = 0, j = 0;
    while (j < chaArr.length) {
      if (!isPunctuation(chaArr[j])) {
        if (i < j) chaArr[i] = chaArr[j];
        i++;
      }
      j++;
    }
    return (new String(chaArr, 0, i));
  }

  /**
   * Checks if a character is a punctuation mark IMPROVE!! Currently only checks
   * for ,;.
   */
  public static boolean isPunctuation(char c) {
    if (c == ',' || c == ';' || c == '.')
    // if (Character.UnicodeBlock.of(c)
    // == Character.UnicodeBlock.GENERAL_PUNCTUATION)
    return true;
    return false;
  }

  /**
   * Returns location of first no white space following a point in string
   * 
   * @param string
   *            a string
   * @param index
   *            an index between 0 and the string's lenth -1
   * @return location of first non whitespace >= index, string's length if none
   *         found, aborts if index is not in
   */
  public static int firstNonWhitespace(String string, int index) {
    testStrInd(string, index);
    int i;
    for (i = index; i < string.length() && Character.isWhitespace(string.charAt(i)); i++) {
    }
    return i;
  }

  /**
   * Returns location of first white space following a point in string
   * 
   * @param string
   *            a string
   * @param index
   *            an index between 0 and the string's lenth -1
   * @return location of first non whitespace >= index, string's length if none
   *         found, aborts if index is not in
   */
  public static int firstWhitespace(String string, int index) {
    testStrInd(string, index);
    int i;
    for (i = index; i < string.length() && !Character.isWhitespace(string.charAt(i)); i++) {
    }
    return i;
  }

  /**
   * Returns the remaining part of a word in a string.
   * 
   * @param string
   *            a string
   * @param index
   *            an index between 0 and the string's lenth -1
   * @return substring from index to last contiguous nonwhitespace, empty if
   *         index is whitespace, rest of string if all nonwhite.
   */
  public static String curWord(String string, int index) {
    testStrInd(string, index);
    return string.substring(index, firstWhitespace(string, index));
  }

  /**
   * Returns the next word in a string.
   * 
   * @param string
   *            a string
   * @param index
   *            an index between 0 and the string's lenth -1
   * @return substring from first nonwhitespace following index to last
   *         contiguous whitespace after that, empty if all white.
   */
  public static String nextWord(String string, int ind) {
    testStrInd(string, ind);
    int fnw = firstNonWhitespace(string, ind);
    return fnw < string.length() ? curWord(string, fnw) : "";
  }

  /**
   * Tests if index is between 0 and string's length - 1. Aborts if not.
   * 
   * @param string
   *            a string
   * @param index
   *            an index
   */
  public static void testStrInd(String string, int index) {
    if (index < 0 || index >= string.length()) throw new IndexOutOfBoundsException("testStringInd: index " + index + " outside range [0,"
                                                                                   + (string.length() - 1) + "] in string " + string);
  }

  /**
   * takes a file name and returns a string containing the file's contents. If
   * the file ends in a newline, it is omitted. 1. Is there a better and more
   * accurate way of doing this? 2. Should this be done with stringBuffer?
   */
  public static String fileName2String(String fileName) throws IOException {

    BufferedReader br = new BufferedReader(new FileReader(fileName));
    String string = br.readLine();
    String line;

    while ((line = br.readLine()) != null)
      string += "\n" + line;
    return string;
  }

  /**
   * Returns the first index >= start of pattern in sb. Written because
   * String.indexOf() does not exist for StringBuffer.
   */
  public static int indexOf(StringBuffer sb, int start, String pattern) {

    int strLen = sb.length();
    int patLen = pattern.length();

    for (int i = start; i <= strLen - patLen; i++)
      if (sb.substring(i, i + patLen).equals(pattern)) return i;
    return -1;
  }

  public static void main(String[] args) {
    try {
      mainCore(args);
    }
    catch (IOException e) {
      if (Monitor.logLevel > 3) Monitor.log("IO exception: " + e.getMessage());
      e.printStackTrace();
    }
    catch (RuntimeException e) {
      if (Monitor.logLevel > 3) Monitor.log("Runtime exception: " + e.getMessage());
      e.printStackTrace();
    }
  }

  public static void mainCore(String[] args) throws IOException {

    if (args.length != 0) // wrong number of arguments
    throw new RuntimeException("Usage: StringOp");

    System.out.print("ENTER          to TEST\n" + "-----          -------\n" + "  1        shrinkWhitespace\n" + "  2        removePunctuation\n"
                     + "  3        nextWord\n" + "Selection: ");

    String string;
    BufferedReader console = new BufferedReader(new InputStreamReader(System.in));

    switch (Integer.parseInt(console.readLine())) {
      case 1:
        System.out.print("File name: ");
        string = fileName2String(console.readLine());
        if (Monitor.logLevel > 3) Monitor.log("Original string: <" + string + ">");
        if (Monitor.logLevel > 3) Monitor.log("Shrunk string: <" + shrinkWhitespace(string) + ">");
        break;
      case 2:
        System.out.print("File name: ");
        string = fileName2String(console.readLine());
        if (Monitor.logLevel > 3) Monitor.log("Original string: <" + string + ">");
        if (Monitor.logLevel > 3) Monitor.log("Depunctuated string: <" + removePunctuation(string) + ">");
        break;
      case 3:
        if (Monitor.logLevel > 3) Monitor.log("nextWord test is not implemented yet.");
        break;
      default:
        if (Monitor.logLevel > 3) Monitor.log("Selection out of range.");
    }
  }

}
