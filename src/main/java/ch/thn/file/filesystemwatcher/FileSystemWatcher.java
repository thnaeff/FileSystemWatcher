/**
 * Copyright 2013 Thomas Naeff (github.com/thnaeff)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *
 */
package ch.thn.file.filesystemwatcher;

import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent.Kind;
import java.util.ArrayList;
import java.util.concurrent.ThreadFactory;

/**
 * The file system watcher watches one or multiple directories for changes. This can be file or
 * folder changes. Any registered {@link PathWatcherListener} is notified when a change occurs.<br>
 * <br>
 * Runs as system watch service or as polling service.
 *
 *
 *
 * @author Thomas Naeff (github.com/thnaeff)
 *
 */
public class FileSystemWatcher extends AbstractWatcher {


  private ArrayList<PathWatcherListener> listeners = null;

  /**
   * A path watcher service using the file system watcher.<br />
   * This is the preferred setup compared to using the setup with polling time.
   *
   */
  public FileSystemWatcher() {
    this(null);
  }

  /**
   *
   *
   * @param threadFactory The factory which creates the thread(s) to run the watcher on
   */
  public FileSystemWatcher(ThreadFactory threadFactory) {
    super();
    listeners = new ArrayList<>();
  }


  /**
   * Adds a {@link PathWatcherListener} which is notified when an event occurs
   *
   * @param l
   */
  public void addPathWatcherListener(PathWatcherListener l) {
    listeners.add(l);
  }

  /**
   * Removes a {@link PathWatcherListener}
   *
   * @param l
   */
  public void removePathWatcherListener(PathWatcherListener l) {
    listeners.remove(l);
  }

  /**
   * Fires the listener method which matches the current event
   *
   * @param eventKind
   * @param path
   * @param context
   * @param overflow
   */
  private void firePathWatcherListener(Kind<?> eventKind, Path path, Path context,
      boolean overflow) {

    for (PathWatcherListener l : listeners) {
      l.pathChanged(path, context, overflow);
    }

    if (eventKind == StandardWatchEventKinds.ENTRY_CREATE) {
      for (PathWatcherListener l : listeners) {
        l.directoryCreated(path, context);
      }
    } else if (eventKind == StandardWatchEventKinds.ENTRY_DELETE) {
      for (PathWatcherListener l : listeners) {
        l.directoryDeleted(path, context);
      }
    } else if (eventKind == StandardWatchEventKinds.ENTRY_MODIFY) {
      for (PathWatcherListener l : listeners) {
        l.directoryModified(path, context);
      }
    }

  }

  /**
   *
   *
   * @param path
   */
  private void fireNewPathWatched(Path path) {

    for (PathWatcherListener l : listeners) {
      l.newPathWatched(path);
    }

  }

  @Override
  public boolean registerPath(Path path) {
    boolean result = super.registerPath(path);
    fireNewPathWatched(path);
    return result;
  }

  /**
   *
   *
   * @param key
   * @param dir
   */
  @Override
  protected void processEvent(Kind<?> eventKind, Path path, Path context, boolean overflow) {
    firePathWatcherListener(eventKind, path, context, overflow);
  }


}
