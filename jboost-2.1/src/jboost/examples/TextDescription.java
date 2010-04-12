package jboost.examples;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;

import jboost.controller.Configuration;
import jboost.examples.ngram.FixedNgram;
import jboost.examples.ngram.FullNgram;
import jboost.examples.ngram.SparseNgram;
import jboost.monitor.Monitor;
import jboost.tokenizer.BadAttException;

/**
 * the description for text attributes.
 * 
 * @author: Nigel Duffy
 * @version $Header:
 *          /cvsroot/jboost/jboost/src/jboost/examples/TextDescription.java,v
 *          1.2 2008/02/16 00:02:19 aarvey Exp $
 */
public class TextDescription extends AttributeDescription {

  TextDescription(String name, Configuration c) throws ClassNotFoundException {
    attributeName = name;
    attributeClass = Class.forName("jboost.examples.SetAttribute");

    // get configuration parameters
    crucial = c.getBool("crucial", false);
    ignoreAttribute = c.getBool("ignoreAttribute", false);
    caseSignificant = c.getBool("caseSignificant", false);
    existence = c.getBool("existence", false);
    ngramsize = c.getInt("ngramsize", 1);
    ngramtype = c.getString("ngramtype", "full").toLowerCase();
    if (ngramsize < 1) // these should probably be SpecFileExceptions
    throw new RuntimeException("Illegal ngramsize received for attribute " + name);
    if (!(ngramtype.equals("fixed") || ngramtype.equals("full") || ngramtype.equals("sparse"))) throw new RuntimeException("Illegal ngram type for attribute "
                                                                                                                           + name);
  }

  /**
   * Reads a text attribute. Main method, str2Att, converts text to an arrya of
   * integers. The integers reflect the order in which the word appeared in the
   * data file. The integers are then compressed so that none repeats in any
   * given example. For example, if in the first four examples in the data file
   * a given attribute is: it is time, time is money, money is not all money is
   * set to be to be or not to be The resulting arrays will be: 0 1 2 2 1 3 3 1
   * 4 5 6 7 8 7 8 9 4 Contains three main objects: gloMap - maps every word
   * that appeard in all documents to an object containing: token - the number
   * of words that preceded it in all documents (starts at 0); and numApp - the
   * number of times the word appeared int all documents. locMap - maps every
   * word that appeared in local document to an object cotaining numApp - the
   * number of times the word appeared in the local document. wordList - the
   * unique least that appeared int this document, int order.
   */
  public Attribute str2Att(String string) throws BadAttException {
    try {
      if (string == null) return new SetAttribute(null, this);
    }
    catch (RepeatedElementException e) {
      throw new RuntimeException("TextDescription.str2Att: got a RepeatedElementException " + "on a null set !!");
    }
    string = string.trim();
    if (string.length() == 0) {
      try {
        return (new SetAttribute(new int[0]));
      }
      catch (RepeatedElementException e) {
        throw new RuntimeException("RepeatedElementException in TextDescription.str2Att");
      }
    }
    WordTable wt = WordTable.globalTable;
    // going over words int string and mapping each to an int
    HashMap locMap = new HashMap();
    // locMap - maps every word that appeared in local document to an
    // object cotaining the number of times the word appeared int
    // current document.
    int numLocWords = 0; // Number of words seen so far int text
    String word; // word to process next
    ArrayList wordList = new ArrayList();
    // the unique "words" that appeared in this document, where a
    // word may actually be an ngram
    Enumeration st;
    if (ngramtype.equals("sparse")) st = new SparseNgram(string, ngramsize);
    else if (ngramtype.equals("full")) st = new FullNgram(string, ngramsize);
    else // fixed ngram
    st = new FixedNgram(string, ngramsize);
    try {
      String[] wordArr = string.split(" ");
      // while ( st.hasMoreElements()) {
      for (int i = 0; i < wordArr.length; i++) {
        // word = (String) st.nextElement();
        word = wordArr[i];
        // if(Monitor.logLevel>3) Monitor.log("DIAG TextAttReader.str2Att:
        // word=" +word);
        if (wt.frozen && !wt.map.containsKey(word)) continue;

        if (locMap.containsKey(word)) { // word appeared in curr text
          ((LocWord) locMap.get(word)).inc();
        }
        else { // word not in curr text
          locMap.put(word, new LocWord());
          if (!wt.frozen && !wt.map.containsKey(word)) { // string is nowhere
            if (Monitor.logLevel > 30) Monitor.log("adding to wordTable word:" + word + ", token=" + wt.size);
            wt.map.put(word, new GloWord(wt.size)); // put new key
            wt.words.add(word);
            wt.size++;
          }
          wordList.add(word);
          numLocWords++;
        }
        ((GloWord) wt.map.get(word)).inc();
      }
    }
    catch (Exception e) {
      if (Monitor.logLevel > 3) Monitor.log(e.getMessage() + e);
      throw new BadAttException("Error reading: " + string, 0, 0);
    }
    int[] wordTokenArr = new int[numLocWords];
    for (int i = 0; i < numLocWords; i++)
      wordTokenArr[i] = ((GloWord) wt.map.get(wordList.get(i))).getToken();

    Attribute setAttribute = null;
    try {
      setAttribute = new SetAttribute(wordTokenArr, this);
    }
    catch (Exception e) {
      e.printStackTrace();
      if (Monitor.logLevel > 3) Monitor.log(e.getMessage() + e);
      throw new BadAttException("Error reading: " + string + "\n" + e, 0, 0);
    }
    return setAttribute;
  }

  private int ngramsize;
  private String ngramtype;

  public String getAttributeValue(int i) {
    return (String) WordTable.globalTable.words.get(i);
  }

  public String toString() {
    String retval = new String(attributeName);
    retval += " " + attributeClass.getName();
    retval += " crucial: " + crucial;
    retval += " ignoreAttribute: " + ignoreAttribute;
    retval += " caseSignificant: " + caseSignificant;
    retval += " existence: " + existence;
    retval += " ngramsize: " + ngramsize;
    retval += " ngramtype: " + ngramtype;
    return (retval);
  }

  public String toString(Attribute attr) {
    if (attr.isDefined() == false) return ("UNDEFINED");
    String retval = new String();
    int[] tok = ((SetAttribute) attr).getList();
    // This should be improved when the reader is integrated.
    if (tok == null) return (null);

    int lineLength = 0;
    for (int i = 0; i < tok.length; i++) {
      String s = (String) getAttributeValue(tok[i]);
      if (s.startsWith("1 ")) // if this is a simple one word token
      // then remove the prefix
      s = s.substring(2);
      retval += s;
      if (i < tok.length - 1) retval += ",";
      lineLength += s.length() + 1;
      if (lineLength > 80) {
        lineLength = 0;
        retval += "\n";
      }
    }

    return (retval);
  }

  static public void setTokenSet(String[] t) {
    if (WordTable.globalTable.size > 0) throw new RuntimeException("attempted to call TextDescription." + "setTokenSet with a nonempty hash " + "table");
    for (int i = 0; i < t.length; i++) {
      if (WordTable.globalTable.map.containsKey(t[i])) throw new IllegalArgumentException("tokens passed to "
                                                                                          + "TextDescription.setTokenSet must all be unique");
      WordTable.globalTable.map.put(t[i], new GloWord(i));
      WordTable.globalTable.words.add(t[i]);
    }
    WordTable.globalTable.size = t.length;
    WordTable.globalTable.frozen = true;
  }

}

/**
 * contains the global token of a word and number of times it appeared in all
 * documents
 */
class GloWord extends LocWord {

  private int token;

  public GloWord() {
    this.token = 0;
  }

  public GloWord(int token) {
    this.token = token;
  }

  public int getToken() {
    return token;
  }
}

/** contains the number of times a word appeared in current document */
class LocWord implements Serializable {

  int numApp; // number of times word appeared

  public LocWord() {
    this.numApp = 1;
  }

  public void inc() {
    numApp++;
  }

  public int getNumApp() {
    return numApp;
  }
}
