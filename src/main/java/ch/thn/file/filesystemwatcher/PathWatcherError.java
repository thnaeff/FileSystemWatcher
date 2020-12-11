package ch.thn.file.filesystemwatcher;

/**
 *
 *
 * @author Thomas Naeff (github.com/thnaeff)
 *
 */
public class PathWatcherError extends Error {
  private static final long serialVersionUID = -2064090030585897604L;

  /**
   *
   *
   * @param msg
   * @param e
   */
  public PathWatcherError(String msg, Throwable e) {
    super(msg, e);
  }


}
