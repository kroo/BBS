package jboost.tokenizer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

import jboost.monitor.Monitor;
import jboost.util.Util;

/**
 * Gets tokens from a string. Differs from Java's StringTokenizer as follows.
 * Disadvantages: Allows only one token terminator. (StringTokenizer allows
 * ranges.) Has less functionality Neutral: Every token must have a terminator
 * (including the last) (In StringTokenizer the terminators are actually
 * separators. See rest() method for getting the characters past last
 * terminator.) Advantages: Terminator can be any string so long as it is not
 * empty, null, or cotains backslashes or newlines. (StringTokenizer supports
 * only single-character terminators.) Provides a way of expressing the
 * terminator in text. A backslash immediately before the terminator escapes the
 * terminator and the terminator is interpreted literally. To write a backslash
 * (or several) immediately before the terminator, replace each one with \\. All
 * other backslashes are interpreted literally. For example, if the terminator
 * is "end" then "\end" will result in "end" appearing as part of the token.
 * "\\end" will result in "\" ending the token (as end is the token terminator),
 * "\\\end" will result in "\end" appearing as part of the token, \\\\end will
 * result in \\ ending the token, etc.
 */

public class LTStringTokenizer {

  private String string, terminator; // string, and token terminators
  private int strLng, terLng; // string and terminator lengths
  private int curLoc = 0; // start location of unparsed data
  private int firstLineNum = 0; // line # where current token begins (>=0)
  private int lastLineNum = 0; // line # where current token ends (>=0)

  /**
   * Only constructor
   * 
   * @param string
   *            the whole string
   * @param terminator
   *            token terminator, cannot contain backslashes
   */
  public LTStringTokenizer(String string, String terminator) {

    testTerminator(terminator);
    this.string = string;
    this.terminator = terminator;
    strLng = string.length();
    terLng = terminator.length();
  }

  public void setTerminator(String value) {
    testTerminator(value);
    terminator = value;
  }

  /** tests if value can be a terminator */
  private void testTerminator(String value) {
    if (value == null || value.equals("") || value.indexOf('\\') != -1 || value.indexOf('\n') != -1) throw new IllegalArgumentException(
                                                                                                                                        "LTStreamTokenizer: "
                                                                                                                                            + "terminator=<"
                                                                                                                                            + value
                                                                                                                                            + "> is either null, empty, or contains a \\ or a newline.");
  }

  /**
   * Returns next token, empty string if terminator appears at the beginning,
   * null string if no more terminators. Handles backslashes preceding the token
   * terminator
   */
  public String next() {

    String curTok = ""; // current token
    int numTrailBS; // number of trailing backslashes

    int i = curLoc;

    // if(Monitor.logLevel>3) Monitor.log("string = " + string + "strLng= " +
    // strLng +
    // " terLng= "+ terLng + " curLoc= " + curLoc);
    // if(Monitor.logLevel>3) Monitor.log("LTStringTokenizer:
    // "+string.charAt(i));

    firstLineNum = lastLineNum;
    while (i <= strLng - terLng) {
      if (string.charAt(i) == '\n') // couning on the terminator
      // not containing \n (so all potential \n are seen here)
      lastLineNum++;
      if (terminator.equals(string.substring(i, i + terLng))) {
        // found a match, check if escaped
        numTrailBS = numTrailBS(string.substring(curLoc, i));
        // eliminate ceil(# backslashes/2)
        curTok += string.substring(curLoc, i - (numTrailBS + 1) / 2);
        i = curLoc = i + terLng;
        if (Util.odd(numTrailBS)) // odd # backslashes - escaped
        curTok += terminator;
        else { // even # backslashes - real
          // if(Monitor.logLevel>3) Monitor.log("string = " + string + "strLng=
          // "
          // + strLng +
          // " terLng= "+ terLng + " curLoc= " + curLoc + "curTok= " + curTok);
          return curTok;
        }

      }
      else i++;
    }

    // no unescaped terminators found
    return null;
  }

  /**
   * Returns rest of string Handles backslashes preceding the token terminator
   * Normally used for the part remaining after last token, so no such
   * conversions will take place.
   */
  public String rest() {
    String rest = ""; // rest of string
    String curTok; // current token
    int holdLineNum = lastLineNum; // holds line number where rest starts

    while ((curTok = next()) != null)
      rest += curTok;
    firstLineNum = holdLineNum; // restore first line of rest
    // if(Monitor.logLevel>3) Monitor.log(curLoc+" "+strLng);
    String tmpStr = string.substring(curLoc, strLng);
    // if(Monitor.logLevel>3) Monitor.log(tmpStr+tmpStr.length());
    return rest + string.substring(curLoc, strLng);
  }

  /** Returns line number where current token begins */

  public int firstLineNum() {
    return firstLineNum;
  }

  /** Returns line number where current token ends */

  public int lastLineNum() {
    return lastLineNum;
  }

  /**
   * Returns the number of contiguous backslashes starting at a location >= beg
   * and ending in location exactly end-1 of string. (end-1 uses the same
   * convention as String.substring) Example: numTrailBS("\b\\a", beg, end) is 0
   * when end=2 or 5 it is 2 for beg=0,1,2 and end=3 and 1 for beg=3 and end=3.
   * 
   * @param string
   *            a string
   * @param beg
   *            first index considered
   * @param end
   *            one after last index considered
   * @return number of contiguous backslashes starting at location >= beg and
   *         ending in location end of string.
   */

  public static int numTrailBS(String string) {

    int i = string.length();
    int numTrailBS = 0;

    while (--i >= 0 && string.charAt(i) == '\\')
      numTrailBS++;
    return numTrailBS;
  }

  /**
   * Not used. Tests whether there is an odd number of backslashes immediately
   * before a given location in a string.
   * 
   * @param string
   *            a sting
   * @param index
   *            an index
   * @return true if there is an odd number of contiguous backslashes ending in
   *         location i-1.
   */

  public static boolean oddNumBS(String string, int index) {

    boolean oddNumBS = false;

    StringOp.testStrInd(string, index);
    while (--index >= 0 && string.charAt(index) == '\\')
      oddNumBS = !oddNumBS;
    return oddNumBS;
  }

  /**
   * Tests LTStringTokenizer. Three modes: Without arguments, the string is
   * "Hello brave new world\nHow's life?" and the terminator " ". With one
   * argument which is a file name, the terminator is the first line of the
   * file, and the string is the rest. With one argument which is -i, prompts
   * for string and terminator
   */
  public static void main(String[] args) {
    try {
      coreMain(args);
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

  public static void coreMain(String[] args) throws IOException {

    String string;
    String terminator;

    if (Monitor.logLevel > 3) Monitor.log("Testing LTStringTokenizer.");
    switch (args.length) {
      case 0:
        string = new String("Hello brave new world\nHow's life?");
        terminator = new String(" ");
        break;
      case 1:
        if (args[0].equals("-i")) {
          BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
          System.out.print("Terminator: ");
          terminator = console.readLine();
          System.out.print("String: ");
          string = console.readLine();
        }
        else {
          BufferedReader br = new BufferedReader(new FileReader(args[0]));

          terminator = br.readLine();

          // readline eliminates EOL's, hence we cannot
          // determine whether there is an EOL at EOF.
          // We do not add a final EOL.

          String line;
          if ((string = br.readLine()) != null) while ((line = br.readLine()) != null)
            string += "\n" + line;
          br.close();
        }
        break;
      default:
        throw new RuntimeException("Usage: LTStringTokenizer" + " [-i|filnename]");
    }
    if (Monitor.logLevel > 3) Monitor.log("String is: <" + string + ">");
    if (Monitor.logLevel > 3) Monitor.log("Token terminator is: <" + terminator + ">");
    LTStringTokenizer sst = new LTStringTokenizer(string, terminator);
    if (Monitor.logLevel > 3) Monitor.log("Tokens:");
    String newToken;
    while ((newToken = sst.next()) != null)
      if (Monitor.logLevel > 3) Monitor.log("<" + newToken + ">" + " lines " + sst.firstLineNum() + " to " + sst.lastLineNum());
    if (Monitor.logLevel > 3) Monitor.log(" Rest: <" + sst.rest() + ">" + " lines " + sst.firstLineNum() + " to " + sst.lastLineNum());
  }

  public String toString() {
    return "LTStringTokenizer: " + "string: " + string + ", terminator: " + terminator + ", curLoc: " + curLoc;
  }

}
