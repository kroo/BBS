package jboost.examples.ngram;

import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

/**
 * This is the abstract class for all ngrams. The constructor and nextElement
 * methods (used for all ngrams) are defined.
 * 
 * @author Rob Schapire (rewritten by Aaron Arvey)
 */

abstract class AbstractNgram implements Enumeration {

  /** The stream of ngrams */
  Enumeration enumer;

  /** The words in the original string */
  String[] words;

  /**
   * The constructor simply sets up the array of words.
   */
  AbstractNgram(String s) {
    StringTokenizer st = new StringTokenizer(s);
    words = new String[st.countTokens()];
    int i = 0;
    while (st.hasMoreElements()) {
      words[i] = st.nextToken();
      i += 1;
    }
  }

  /**
   * @return true if there are more elements
   */
  abstract public boolean hasMoreElements();

  /**
   * @return next element
   */
  public Object nextElement() {
    if (!hasMoreElements()) throw new NoSuchElementException();
    return ((String) enumer.nextElement());
  }

  /**
   * a main for testing public static void main(String[] argv) { int size =
   * Integer.parseInt(argv[0]); String ngram_type = argv[1];
   * java.io.BufferedReader in = new java.io.BufferedReader(new
   * java.io.InputStreamReader(System.in)); String line; try { while ((line =
   * in.readLine()) != null) { Enumeration st; if (ngram_type.equals("sparse"))
   * st = new SparseNgram(line, size); else if (ngram_type.equals("full")) st =
   * new FullNgram(line, size); else // fixed ngram st = new FixedNgram(line,
   * size); while(st.hasMoreElements()) { System.out.println((String)
   * st.nextElement()); } } } catch (java.io.IOException e) {
   * e.printStackTrace(); } }
   */
}
