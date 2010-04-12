package jboost.examples.ngram;

import java.util.Enumeration;
import java.util.NoSuchElementException;

/**
 * An enumeration object that creates a stream of all ngrams with specified
 * pattern defined on a given string of words.
 * 
 * @author Rob Schapire (rewritten by Aaron Arvey)
 */

class PatternNgram implements Enumeration {

  // current position in the list of words
  private int pos = 0;
  private String[] words;
  private boolean[] pattern;
  private String prepend = "";

  private static final String[] prependChar = { "0", "1" };

  /**
   * The constructor for this class. The stream of ngrams is defined using
   * tuples from the given array of words. Tuples are created using the pattern
   * defined by the boolean array. For instance, a pattern of the form 1101
   * indicates that triples should be created of the form word[i] word[i+1]
   * word[i+3] (skipping all places in which the pattern is false).
   */
  PatternNgram(String[] words, boolean[] pattern) {
    this.words = words;
    this.pattern = pattern;
    for (int i = 0; i < pattern.length; i++)
      prepend += prependChar[pattern[i] ? 1 : 0];
  }

  /**
   * Returns true if and only if there are more elements in the stream.
   */
  public boolean hasMoreElements() {
    int eltsInStream = words.length - pattern.length;
    return (pos <= eltsInStream);
  }

  /**
   * Next tuple in the stream. Tuples are concatenated, separated by spaces.
   */
  public Object nextElement() {
    if (!hasMoreElements()) throw new NoSuchElementException();
    String ret = prepend;
    for (int i = 0; i < pattern.length; i++) {
      if (pattern[i]) ret += " " + words[pos + i];
    }
    pos++;
    return ret;
  }
}
