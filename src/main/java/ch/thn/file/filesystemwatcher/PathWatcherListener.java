package ch.thn.file.filesystemwatcher;

import java.nio.file.Path;

/**
 * 
 * 
 * @author Thomas Naeff (github.com/thnaeff)
 *
 */
public interface PathWatcherListener {


  /**
   * Fired when a new path is added to the list of watched paths
   * 
   * @param path
   */
  public void newPathWatched(Path path);

  /**
   * Any change in the path, with indicator if an event overflow occurred.<br>
   * Always gets fired before created, deleted or modified.
   * 
   * @param path
   * @param context The created, deleted or modified path, or <code>null</code> if
   *        overflow=<code>true</code>
   * @param overflow An indicator by the watch service which indicates that events may have been
   *        lost or discarded
   */
  public void pathChanged(Path path, Path context, boolean overflow);

  /**
   * Fired when a directory or its content has been created
   * 
   * @param path
   * @param created
   */
  public void directoryCreated(Path path, Path created);

  /**
   * Fired when a directory or its content has been deleted
   * 
   * @param path
   * @param deleted
   */
  public void directoryDeleted(Path path, Path deleted);

  /**
   * Fired when a directory or its content has been modified
   * 
   * @param path
   * @param modified
   */
  public void directoryModified(Path path, Path modified);

}
