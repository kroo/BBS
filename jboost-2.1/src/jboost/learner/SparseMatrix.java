package jboost.learner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import jboost.monitor.Monitor;

/*******************************************************************************
 * Sparse matrix implementation: each row corresponds to an example (indexed by
 * <i>ExampleIndex</i>) each column corresponds to a <i>Token</i>
 * 
 * @author Yoram Singer
 * @version $Header:
 *          /cvsroot/jboost/jboost/src/jboost/learner/SparseMatrix.java,v
 *          1.1.1.1 2007/05/16 04:06:02 aarvey Exp $
 ******************************************************************************/

class SparseMatrix {

  private int nRow, nCol, maxToken;
  private boolean finalized;
  private int[][] rows, cols;
  private ArrayList tRows; /* array for storying rows before finalization */

  static public void main(String[] argv) {
    try {
      SparseMatrix sm = new SparseMatrix();
      Random r = new Random();
      int nr = 5, nt = 4;
      int[] A, T;

      for (int i = 0; i < nr; i++) {
        int l = r.nextInt(nt + 1);

        T = new int[nt];
        A = new int[l];

        for (int j = 0; j < nt; j++)
          T[j] = 0;

        for (int j = 0; j < l; j++) {
          int t;

          do {
            t = r.nextInt(nt);
          } while (T[t] == 1);

          T[t] = 1;
          A[j] = t;
        }

        sm.printit("T", T);
        sm.printit("A", A);

        sm.addRow(A);
      }

      sm.finalizeMatrix();

      /** sm.printSparseMatrix(); * */

      if (Monitor.logLevel > 3) Monitor.log(sm);

      int[][] IE = sm.intersectWithToken(1);

      if (IE != null) {
        if (Monitor.logLevel > 3) Monitor.log("Intersected matrix for token 1");
        for (int i = 1; i < IE.length; i++)
          sm.printit("Row " + IE[0][i - 1], IE[i]);
      }

    }
    catch (Exception e) {
      if (Monitor.logLevel > 3) Monitor.log(e.getMessage());
      e.printStackTrace();
    }
    finally {
      if (Monitor.logLevel > 3) Monitor.log("\nfinished testing sparse matrix implementation");
    }
  }

  /** create an empty matrix * */
  SparseMatrix() {
    tRows = new ArrayList();
    finalized = false;
    maxToken = 0;
    nRow = 0;
    nCol = 0;
  }

  /** get the array of Tokens associated with a row * */
  int[] getRow(int i) {
    if (!finalized) throw new RuntimeException("getRow: Sparse matrix not finalized");

    if (i >= nRow || i < 0) return null;

    return rows[i];
  }

  /** get the array of ExampleIndexes associated with a column */
  int[] getColumn(int i) {
    if (!finalized) throw new RuntimeException("Sparse matrix not finalized");

    if (i >= nCol || i < 0) return null;

    return cols[i];
  }

  /*
   * Return a list of tokens co-occuring with a given token. The first row in
   * the list contains the example indices in which the token appears. The rest
   * of rows are the list of tokens for eah example in which the token appears.
   */
  int[][] intersectWithToken(int tok) {
    if (!finalized) throw new RuntimeException("Sparse matrix not finalized");

    if (cols[tok] == null) return null;

    int[][] IE = new int[cols[tok].length + 1][];

    IE[0] = cols[tok];

    for (int i = 0; i < cols[tok].length; i++)
      IE[i + 1] = rows[cols[tok][i]];

    return IE;
  }

  // check whether an example ocontains a token
  boolean appears(int index, int token) {

    if (!finalized) throw new RuntimeException("Sparse matrix not finalized");

    if (rows[index] == null) return false;

    if (Arrays.binarySearch(rows[index], token) >= 0) return true;

    return false;
  }

  /** add a row of tokens corresponding to a new example * */
  void addRow(int[] tokens) {
    tRows.add(tokens);
    nRow++;

    /** this.printit("inside addRow ", tokens); * */

    for (int i = 0; i < tokens.length; i++) {
      if (tokens[i] > maxToken) maxToken = tokens[i];
    }
  }

  /* if maximal number of tokens is not given use maximal token index */
  void finalizeMatrix() {
    try {
      finalizeMatrix(maxToken + 1);
    }
    catch (InsufficientSparseMatrixColumns e) {
      System.err.println("Impossible...");
    }
  }

  /** finalize the matrix - has to be called before issuing a get command * */
  void finalizeMatrix(int maxCol) throws InsufficientSparseMatrixColumns {
    int[] token_count;

    if (maxCol < maxToken) throw new InsufficientSparseMatrixColumns("Insufficient number of colums in finalization " + maxCol + " < " + maxToken);

    nCol = maxCol;

    rows = new int[nRow][];
    cols = new int[nCol][];
    token_count = new int[nCol];

    /* Set row pointers and count number of appearances for each token */
    for (int i = 0; i < nRow; i++) {
      rows[i] = (int[]) tRows.get(i);
      for (int j = 0; j < rows[i].length; j++)
        token_count[rows[i][j]]++;
    }

    /* Set column pointers */
    for (int i = 0; i < nCol; i++) {
      if (token_count[i] > 0) cols[i] = new int[token_count[i]];
      token_count[i] = 0;
    }

    /* Fill up column-based representation */
    for (int i = 0; i < nRow; i++)
      for (int j = 0; j < rows[i].length; j++) {
        int t = rows[i][j];
        cols[t][token_count[t]] = i;
        token_count[t]++;
      }

    finalized = true;

    tRows.clear();
  }

  public String toString() {
    if (!finalized) return null;
    else {
      String s = "\nSparse matrix of " + nRow + " rows and " + nCol + " columns\n\n";
      int i, j;

      for (i = 0; i < nRow; i++) {
        int[] tmpr = new int[nCol];

        for (j = 0; j < nCol; j++)
          tmpr[j] = 0;

        if (rows[i] != null) for (j = 0; j < rows[i].length; j++)
          tmpr[rows[i][j]] = 1;

        for (j = 0; j < tmpr.length; j++)
          s += tmpr[j];
        s += "\n";
      }

      s += "\n";

      for (i = 0; i < nCol; i++) {
        int[] tmpr = new int[nRow];

        for (j = 0; j < nRow; j++)
          tmpr[j] = 0;

        if (cols[i] != null) for (j = 0; j < cols[i].length; j++)
          tmpr[cols[i][j]] = 1;

        for (j = 0; j < tmpr.length; j++)
          s += tmpr[j];
        s += "\n";
      }

      return s;
    }
  }

  void printSparseMatrix() {
    if (!finalized) throw new RuntimeException("Sparse matrix not finalized");
    else {
      int i, j;

      if (Monitor.logLevel > 3) Monitor.log("Rows: " + nRow + " Columns: " + nCol + "\n");

      if (Monitor.logLevel > 3) Monitor.log("Printing matrix by row:");
      if (Monitor.logLevel > 3) Monitor.log("-----------------------\n");

      for (i = 0; i < nRow; i++) {
        int[] tmpr = new int[nCol];

        for (j = 0; j < nCol; j++)
          tmpr[j] = 0;

        if (rows[i] != null) for (j = 0; j < rows[i].length; j++)
          tmpr[rows[i][j]] = 1;

        for (j = 0; j < tmpr.length; j++)
          System.out.print(tmpr[j]);
        System.out.print("\n");
      }

      if (Monitor.logLevel > 3) Monitor.log("Printing matrix by column:");
      if (Monitor.logLevel > 3) Monitor.log("-------------------------\n");

      for (i = 0; i < nCol; i++) {
        int[] tmpr = new int[nRow];

        for (j = 0; j < nRow; j++)
          tmpr[j] = 0;

        if (cols[i] != null) for (j = 0; j < cols[i].length; j++)
          tmpr[cols[i][j]] = 1;

        for (j = 0; j < tmpr.length; j++)
          System.out.print(tmpr[j]);
        System.out.print("\n");
      }
    }
  }

  int numRows() {
    return this.nRow;
  }

  int numCols() {
    return this.nCol;
  }

  void printit(String name, int[] A) {
    System.out.print(name + " :");
    for (int i = 0; i < A.length; i++)
      System.out.print(" " + A[i]);
  }

}
