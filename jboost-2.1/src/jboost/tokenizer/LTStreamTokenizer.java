package jboost.tokenizer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import jboost.monitor.Monitor;
import jboost.util.Util;

/**
 * Gets tokens from a string. Differs from Java's StringTokenizer as follows.
 * Disadvantages: Allows only one token terminator. (StringTokenizer allows
 * ranges.) Has less functionality Neutral: Every token must have a terminator
 * (including the last) (In StringTokenizer the terminators are actually
 * separators. See rest() method for getting the characters past last
 * terminator.) Advantages: Terminator can be any string so long as it is not
 * null, empty, or cotains the escape character (default is \), //, /*, or * /
 * (StringTokenizer supports only single-character terminators.) Provides a way
 * of expressing the terminator in text. The escape immediately before the
 * terminator escapes the terminator and causes the terminator to be interpreted
 * literally. To write the escape char (or several) immediately before the
 * terminator, double each. All other appearances of the escape char are
 * interpreted literally. For example, if the terminator is "end" then "\end"
 * will result in "end" appearing as part of the token. "\\end" will result in
 * "\" ending the token (as end is the token terminator), "\\\end" will result
 * in "\end" appearing as part of the token, \\\\end will result in \\ ending
 * the token, etc.
 */
public class LTStreamTokenizer {

  private BufferedReader br;
  private String terminator; // token terminators
  private int terLen, minLen; // terminator and min lenth of ter, //, /*, */
  private int terLines; // number of newlines in terminator
  private char escape; // char escaping terminator
  private StringBuffer strBuf = new StringBuffer();
  // part of stream that hasn't been returned yet
  private int strLen = 0; // stringBuffer length
  private int firstLineNum = 0; // line # where current token begins (>=0)
  private int lastLineNum = 0; // line # where current token ends (>=0)
  private boolean disallowComments = false; // comment marks are meaningless

  /**
   * constructor
   * 
   * @param string
   *            the whole string
   * @param terminator
   *            token terminator, cannot contain backslashes
   * @param escape
   *            character used to escape special symbols
   */
  public LTStreamTokenizer(BufferedReader br, String terminator, char escape) {
    terminator(terminator);
    escape(escape);
    this.br = br;
  }

  public LTStreamTokenizer(BufferedReader br, String terminator) {
    this(br, terminator, '\\');
  }

  /** Sets the terminator */
  public void terminator(String terminator) {
    testTerminator(terminator);
    this.terminator = terminator;
    terLen = terminator.length();
    minLen = terLen <= 2 ? 2 : terLen;
    for (int i = 0, terLines = 0; i < terLen; i++)
      if (terminator.charAt(i) == '\n') terLines++;
  }

  /** tests if value can be a terminator */
  private void testTerminator(String value) {
    if (value == null || value.equals("") || value.indexOf(escape) != -1 || value.indexOf("//") != -1 || value.indexOf("/*") != -1 || value.indexOf("*/") != -1) throw new IllegalArgumentException(
                                                                                                                                                                                                    "LTStreamTokenizer: "
                                                                                                                                                                                                        + "terminator=<"
                                                                                                                                                                                                        + value
                                                                                                                                                                                                        + "> is either null, empty, or contains //, /*, or */.");
  }

  /** Sets the escape char */
  public void escape(char escape) {
    if (terminator.indexOf(escape) == -1) this.escape = escape;
    else throw new IllegalArgumentException("LTStreamTokenizer: " + "escape=" + escape + "appears in terminator.");
  }

  public void disallowComments(boolean value) {
    disallowComments = value;
  }

  /**
   * Returns next token, empty string if terminator appears at the beginning,
   * null string if no more terminators. Handles backslashes preceding the token
   * terminator.
   */
  public String next() {
    return next(false);
  }

  /**
   * Returns next token. Empty string if terminator appears at the beginning. If
   * returnPartial is false, behaves exactly as next(): returns null if no more
   * terminators. If returnPartial is true, returns partial token if reaches EOF
   * and returns null only if there is nothing after last terminator. Handles
   * backslashes preceding the token terminator
   */
  public String next(boolean returnPartial) {
    String curTok = ""; // current token, contains the processed part of
    // strBuf (after eliminating escapes)
    int toCopy = 0; // beginning of string not yet copied to curTok
    int i = 0; // location currently processed
    int numEscapes; // number of trailing escapes
    int endComInd;
    boolean ongoingComment = false; // in ongoing comment region
    boolean lineComment = false; // in line comment region
    int numRead = 0; // number of characters read from file
    final int bufLen = 1000; // read bufLen characters at a time
    char[] cBuf = new char[bufLen]; // contains characters read from file
    firstLineNum = lastLineNum;
    while (true) {
      // if(Monitor.logLevel>3) Monitor.log("strLen=" + strLen + " toCopy=" +
      // toCopy + " i=" + i);
      if (i > strLen - minLen) {
        try {
          numRead = br.read(cBuf, 0, bufLen);
        }
        catch (IOException e) {
          if (Monitor.logLevel > 3) Monitor.log("LTStreamTokenizer.next(): " + "IO exception: " + e.getMessage());
          e.printStackTrace();
        }
        if (numRead == -1) { // no more data
          if (strLen != 0 && returnPartial) {
            curTok += strBuf.substring(toCopy, strLen);
            strBuf.delete(0, strLen); // necessary?
            toCopy = i = strLen = 0; // necessary?
            return curTok;
          }
          else // nothing after last token or
          return null; // don't want partial tokens
        }
        // make sure we increase line number
        strBuf.append(cBuf, 0, numRead);
        strLen += numRead;
      }
      else if (ongoingComment) { // ongoing comment
        if ((endComInd = StringOp.indexOf(strBuf, i, "*/")) != -1) {
          // ongoing comment ends in current buffer?
          lastLineNum += numNewLines(i, endComInd);
          numEscapes = numEscapes(strBuf.substring(0, endComInd), escape);
          if (Util.even(numEscapes)) // ends!
          ongoingComment = false;
          toCopy = i = endComInd + 2; // whether or not it ends
        }
        else { // ongoing comment doesn't end in current buffer
          lastLineNum += numNewLines(i, strLen);
          toCopy = i = strLen - numEscapes(strBuf.substring(0, strLen), escape);
        }
      }
      else if (lineComment) { // line comment
        if ((endComInd = StringOp.indexOf(strBuf, i, "\n")) != -1) {
          // line ends in current buffer
          lastLineNum++;
          lineComment = false;
          toCopy = i = endComInd + 1;
        }
        else // line doesn't end in current buffer
        toCopy = i = strLen;
      }
      else if (!disallowComments && i <= strLen - 2 && strBuf.substring(i, i + 2).equals("/*")) {
        // ongoing comment starts?
        numEscapes = numEscapes(strBuf.substring(0, i), escape);
        if (Util.even(numEscapes)) { // onging comment starts!
          curTok += strBuf.substring(toCopy, i - numEscapes / 2);
          ongoingComment = true;
        }
        else { // escaped, part of token
          curTok += strBuf.substring(toCopy, i - (numEscapes + 1) / 2) + "/*";
          toCopy = i + 2;
        }
        i += 2;
      }
      else if (!disallowComments && i <= strLen - 2 && strBuf.substring(i, i + 2).equals("//")) {
        // line comment starts?
        numEscapes = numEscapes(strBuf.substring(0, i), escape);
        if (Util.even(numEscapes)) { // line comment starts!
          curTok += strBuf.substring(toCopy, i - numEscapes / 2);
          lineComment = true;
        }
        else // escpaed, part of token
        curTok += strBuf.substring(toCopy, i - (numEscapes + 1) / 2) + "//";
        toCopy = i + 2;
      }
      else if (i <= strLen - terLen && strBuf.substring(i, i + terLen).equals(terminator)) {
        // terminator?
        numEscapes = numEscapes(strBuf.substring(0, i), escape);
        lastLineNum += terLines;
        if (Util.even(numEscapes)) { // terminator!
          curTok += strBuf.substring(toCopy, i - (numEscapes) / 2);
          toCopy = i = i + terLen;
          strBuf.delete(0, toCopy); // necessary?
          strLen -= toCopy;
          return curTok;
        }
        else { // escaped
          curTok += strBuf.substring(toCopy, i - (numEscapes + 1) / 2) + terminator;
          toCopy = i = i + terLen;
        }
      }
      else {
        if (strBuf.charAt(i) == '\n') lastLineNum++;
        i++;
      }
    }
  }

  /**
   * Returns rest of string Handles backslashes preceding the token terminator
   * Normally used for the part remaining after last token, so no such
   * conversions will take place.
   */
  public String rest() {
    String curTok; // current token
    int holdLineNum = lastLineNum; // holds line number where rest starts
    String temp = "";
    while ((curTok = next(true)) != null)
      temp += curTok;
    firstLineNum = holdLineNum; // restore first line of rest
    return temp;
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
   * convention as String.substring) Example: numEscapes("\b\\a", beg, end) is 0
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
  private int numEscapes(String string, char escape) {
    int i = string.length();
    int numEscapes = 0;
    while (--i >= 0 && string.charAt(i) == escape)
      numEscapes++;
    return numEscapes;
  }

  /**
   * @param from
   *            start location
   * @param to
   *            one more than last location
   * @return number of new lines in strBuf at locations satisfying from<=location<to
   */
  private int numNewLines(int from, int to) {
    int numNewLines = 0;
    for (int i = from; i < to; i++)
      if (strBuf.charAt(i) == '\n') numNewLines++;
    return numNewLines;
  }

  public String toString() {
    return "Contents of LTStreamTokenizer:" + " terminator=<" + terminator + ">" + " escape=<" + escape + ">" + " strBuf=<" + strBuf + ">" + " firstLineNum="
           + firstLineNum + " lastLineNum=" + firstLineNum;
  }

  /**
   * Tests LTStreamTokenizer. Three modes: Without arguments, the string is
   * "Hello brave new world\nHow's life?" and the terminator " ". With one
   * argument which is a file name, the terminator is the first line of the
   * file, and the string is the rest. With one argument which is -i, prompts
   * for string and terminator
   */
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
    String string;
    String terminator;
    if (Monitor.logLevel > 3) Monitor.log("Testing LTStreamTokenizer.");
    if (args.length != 1) // wrong number of arguments
    throw new RuntimeException("Usage: LTStreamTokenizer <filnename>");
    BufferedReader br = new BufferedReader(new FileReader(args[0]));
    terminator = br.readLine();
    if (Monitor.logLevel > 3) Monitor.log("Token terminator is: <" + terminator + ">" + " tokenizing rest of file.");
    LTStreamTokenizer sst = new LTStreamTokenizer(br, terminator);
    if (Monitor.logLevel > 3) Monitor.log("Tokens:");
    String newToken;
    while ((newToken = sst.next()) != null)
      if (Monitor.logLevel > 3) Monitor.log("<" + newToken + ">" + " lines " + sst.firstLineNum() + " to " + sst.lastLineNum());
    if (Monitor.logLevel > 3) Monitor.log(" Rest: <" + sst.rest() + ">" + " lines " + sst.firstLineNum() + " to " + sst.lastLineNum());
  }
}
