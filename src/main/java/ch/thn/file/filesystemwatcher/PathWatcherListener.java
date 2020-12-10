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
   * @param path The path that has been added to be watched
   */
  public void newPathWatched(Path path);

  /**
   * Any change in the path, with indicator if an event overflow occurred.<br>
   * Always gets fired before created, deleted or modified.
   *
   * @param path The path under which the change happened
   * @param context The created, deleted or modified path, or <code>null</code> if
   *        overflow=<code>true</code>
   * @param overflow An indicator by the watch service which indicates that events may have been
   *        lost or discarded
   */
  public void pathChanged(Path path, Path context, boolean overflow);

  /**
   * Fired when a directory or its content has been created
   *
   * @param path The path under which the change happened
   * @param created The created path under <code>path</code>
   */
  public void directoryCreated(Path path, Path created);

  /**
   * Fired when a directory or its content has been deleted
   *
   * @param path The path under which the change happened
   * @param deleted The deleted path under <code>path</code>
   */
  public void directoryDeleted(Path path, Path deleted);

  /**
   * Fired when a directory or its content has been modified
   *
   * @param path The path under which the change happened
   * @param modified The modified path under <code>path</code>
   */
  public void directoryModified(Path path, Path modified);

}
