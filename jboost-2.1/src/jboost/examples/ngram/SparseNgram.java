package jboost.examples.ngram;

/**
 * This is the class for sparse ngrams, i.e., all ngrams up to a given length,
 * including those with wildcards.
 * 
 * @author Rob Schapire (rewritten by Aaron Arvey)
 */

public class SparseNgram extends AbstractNgram {

  /** current ngram pattern */
  private int n = 1;

  /** stopping position for constructing ngrams */
  private int end;

  /**
   * Constructor. ngrams will be constructed from string s and have length up to
   * the given size. The given size cannot exceed 25 for overflow reasons
   * (really its 31, but we use 25 out of paranoia).
   * 
   * @param s
   *            string from which ngrams are constructed
   * @param size
   *            maximum size of all ngrams returned
   */

  public SparseNgram(String s, int size) {
    super(s);
    if (size > 25) {
      throw new IllegalArgumentException("cannot have window size bigger " + "than 25 in SparseNgram");
    }
    enumer = new PatternNgram(words, new boolean[] { true });
    end = 1 << size;
  }

  public boolean hasMoreElements() {
    if (n >= end) return false;
    while (!enumer.hasMoreElements() && (n += 2) < end) {
      int pos = 0;
      for (int i = n; i != 0; i >>= 1)
        pos++;
      boolean[] pat = new boolean[pos];
      pos = 0;
      for (int i = n; i != 0; i >>= 1) {
        pat[pos] = ((i & 1) != 0);
        pos++;
      }
      enumer = new PatternNgram(words, pat);
    }
    return (n < end);
  }

}
