package jboost.examples.ngram;

import java.util.Arrays;

/**
 * Class for fixed ngrams
 * 
 * @author Rob Schapire (rewritten by Aaron Arvey)
 */

public class FixedNgram extends AbstractNgram {

  /**
   * Ngrams are constructed from the string s and have size n.
   * 
   * @param s
   *            string from which ngrams are constructed
   * @param n
   *            size of all ngrams returned
   */

  public FixedNgram(String s, int n) {
    super(s);
    boolean[] pat = new boolean[n];
    Arrays.fill(pat, true);
    enumer = new PatternNgram(words, pat);
  }

  public boolean hasMoreElements() {
    return enumer.hasMoreElements();
  }

}
