/*
 * Created on Jan 15, 2004
 *
 */
package jboost.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Vector;

/**
 * This class provides a wrapper for loading data files The loader will look for
 * files in a several places, similar to the way the ClassLoader uses the
 * CLASSPATH The primary search point will be relative to the JBOOST_HOME By
 * using FileLoader to load files, we centralize control of where files are
 * found. If we enhance the Configuration implementation to use Properties, some
 * of the utility of this approach will go away, because file names can be
 * specified using absolute pathnames.
 * 
 * @author cschavis
 */
public class FileLoader {

  static private Vector d_path = new Vector(5);
  static private String JBOOST_HOME = "jboost.home";

  // intialization of d_path
  static {
    // set up initial paths
    addDirectory(".");

    String home = System.getProperty(JBOOST_HOME);
    if (home != null) {
      addDirectory(home);
    }
  }

  static public void addDirectory(String dirname) {
    System.out.println("Fileloader adding " + dirname + " to path.");
    d_path.addElement(dirname);
  }

  /**
   * @param filename
   * @return
   */
  static public FileReader createFileReader(String filename) throws FileNotFoundException {

    if (filename == null) {
      throw new FileNotFoundException();
    }

    // try each element of path to see if works
    boolean finished = false;
    int index = 0;
    FileReader result = null;

    while (!finished && index < d_path.size()) {
      try {
        result = new FileReader(filename);
        System.out.println("\tFound " + filename);
      }
      catch (FileNotFoundException fnf) {
        filename = d_path.get(index) + File.separator + filename;
        index++;
        continue;
      }
      finished = true;
    }

    // XXX: do we really want to throw here, or just return null?
    if (result == null) {
      throw new FileNotFoundException(filename);
    }

    return result;
  }

}
