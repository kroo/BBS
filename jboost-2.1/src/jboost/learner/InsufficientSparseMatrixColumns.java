package jboost.learner;

class InsufficientSparseMatrixColumns extends Exception {

  InsufficientSparseMatrixColumns(String message) {
    this.message = message;
  }

  public String getMessage() {
    return (message);
  }

  private String message;
}
