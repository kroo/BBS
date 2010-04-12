package jboost.examples.ngram;

import java.util.Arrays;

/**
 * Full ngrams are all ngrams up to a given length.
 * 
 * @author Rob Schapire (rewritten by Aaron Arvey)
 */

public class FullNgram extends AbstractNgram {

  /** current ngram length */
  private int n = 1;

  /** window size */
  private int size;

  /**
   * Constructor. The ngrams will be constructed from the string s and will have
   * length up to the given size.
   * 
   * @param s
   *            string from which ngrams are constructed
   * @param n
   *            maximum size of all ngrams returned
   */
  public FullNgram(String s, int size) {
    super(s);
    this.size = size;
    boolean[] pat = new boolean[] { true };
    enumer = new PatternNgram(words, pat);
  }

  public boolean hasMoreElements() {
    if (n > size) return false;
    n += 1;
    while (!enumer.hasMoreElements() && n <= size) {
      boolean[] pat = new boolean[n];
      Arrays.fill(pat, true);
      enumer = new PatternNgram(words, pat);
      n += 1;
    }
    return (n <= size);
  }

}
