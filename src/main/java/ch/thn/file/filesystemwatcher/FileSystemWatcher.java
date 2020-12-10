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

import java.io.File;
import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class FileSystemWatcher implements Runnable {

  private static final Logger logger = LoggerFactory.getLogger(FileSystemWatcher.class);

  private WatchService watcher = null;

  private ThreadFactory threadFactory = null;
  private Thread thread = null;

  private boolean usePolling = false;
  private boolean isRunning = false;

  /**
   * Currently watched paths and their watch keys to identify them
   */
  private HashMap<WatchKey, Path> keys = null;

  /**
   * For each path there is a flag which defines if all its children should be added to the watch
   * list or not
   */
  private HashMap<WatchKey, Boolean> allChildren = null;

  private ArrayList<PathWatcherListener> listeners = null;


  /**
   * A path watcher service using the file system watcher.<br />
   * This is the preferred setup compared to using the setup with polling time.
   *
   */
  public FileSystemWatcher() {
    this(0, null);
  }

  /**
   * A path watcher service with optional polling mode
   *
   * @param pollingTime If no time (=0) is set here, the java file system watch service is used. If
   *        a time is set (>0), the {@link PollingWatchService} is used.
   * @param timeUnit The unit of the <code>pollingTime</code>
   */
  public FileSystemWatcher(long pollingTime, TimeUnit timeUnit) {
    this(pollingTime, timeUnit, null);
  }

  /**
   * A path watcher service with optional polling mode
   *
   * @param pollingTime If no time (<=0) is set here, the java file system watch service is used. If
   *        a time is set (>0), the {@link PollingWatchService} is used.
   * @param timeUnit The unit of the <code>pollingTime</code>
   * @param threadFactory The factory which creates the thread(s) to run the watcher on
   */
  public FileSystemWatcher(long pollingTime, TimeUnit timeUnit, ThreadFactory threadFactory) {

    // Use polling if polling time is set
    usePolling = pollingTime > 0 && timeUnit != null;

    if (usePolling) {
      watcher = new PollingWatchService(pollingTime, timeUnit, threadFactory);
    } else {
      try {
        watcher = FileSystems.getDefault().newWatchService();
      } catch (IOException exc) {
        throw new PathWatcherError("Failed to construct new watch service", exc);
      } catch (UnsupportedOperationException exc) {
        throw new PathWatcherError("File system seems not to support file system watching. "
            + "Try the polling functionality.", exc);
      }
    }

    keys = new HashMap<>();
    allChildren = new HashMap<>();
    listeners = new ArrayList<>();

  }

  /**
   * Returns an unmodifiable collection of all paths which are currently being watched
   *
   * @return
   */
  public Collection<Path> getWatchedPaths() {
    return Collections.unmodifiableCollection(keys.values());
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
  public void firePathWatcherListener(Kind<?> eventKind, Path path, Path context,
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
  public void fireNewPathWatched(Path path) {

    for (PathWatcherListener l : listeners) {
      l.newPathWatched(path);
    }

  }

  /**
   *
   */
  public void clearAllRegisteredPaths() {
    // Cancel all old keys
    for (WatchKey k : keys.keySet()) {
      k.cancel();
    }

    keys.clear();

  }

  /**
   * Adds a new path to the list of watched paths. If a path to a file is given, its parent
   * directory is registered instead because only directories can be watched.
   *
   *
   * @param path
   * @param allChildren If set to <code>true</code>, all child directories are registered too
   * @param allParents If set to <code>true</code>, all parent directories are registered too
   * @return
   */
  public boolean registerPath(Path path, boolean allChildren, boolean allParents) {
    File f = path.toFile();

    if (f.isFile()) {
      // The path exists and it is a file -> get its parent directory
      path = path.getParent();
    } else if (!f.exists()) {
      // The path is not a file and does not exist
      return false;
    }

    if (!allChildren && !allParents) {
      return register(path, false);
    } else {
      if (allChildren) {
        registerAllChildren(path);
      }

      if (allParents) {
        registerAllParents(path);
      }
    }

    return true;
  }

  /**
   * Adds a new path to the list of watched paths. If a path to a file is given, its parent
   * directory is registered instead because only directories can be watched.
   *
   *
   * @param path
   * @param allChildren If set to <code>true</code>, all child directories are registered too
   * @param allParents If set to <code>true</code>, all parent directories are registered too
   * @return
   */
  public boolean registerPath(String path, boolean allChildren, boolean allParents) {
    return registerPath(Paths.get(path), allChildren, allParents);
  }

  /**
   * Adds a new path to the list of watched paths.
   *
   * @param path
   * @return
   */
  public boolean registerPath(String path) {
    return registerPath(path, false, false);
  }

  /**
   * Adds a new path to the list of watched paths.
   *
   * @param path
   * @return
   */
  public boolean registerPath(Path path) {
    return registerPath(path, false, false);
  }

  /**
   * Registers the given path for all the events
   *
   * @param dir
   * @param allChildren
   * @return
   * @throws IOException
   */
  private boolean register(Path dir, boolean allChildren) {

    WatchKey key = null;

    try {
      if (usePolling) {
        key = ((PollingWatchService) watcher).register(dir);
      } else {
        // 'Path' only accepts an sun.nio.fsAbstractWatchService (checked internally with
        // 'instanceof') which contains a 'register' method that will get called
        key = dir.register(watcher, StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY,
            StandardWatchEventKinds.OVERFLOW);
      }
    } catch (Exception e) {
      throw new PathWatcherError("Failed to register path "
          + dir, e);
    }

    // If its the same one it will just be updated
    keys.put(key, dir);
    this.allChildren.put(key, allChildren);

    fireNewPathWatched(dir);

    return true;
  }

  /**
   * Walks through the file tree and registers all child paths
   *
   * @param path
   * @throws IOException
   */
  private void registerAllChildren(Path path) {

    try {
      Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
          try {
            register(dir, true);
          } catch (Exception e) {
            logger.warn("Failed to recursively register child path {}", dir, e);
          }
          return FileVisitResult.CONTINUE;
        }
      });
    } catch (IOException e) {
      logger.warn("Failed to recursively register path and children of {}", path, e);
    }

  }

  /**
   * Walks upwards through the file tree and registers all parent paths
   *
   * @param path
   * @throws IOException
   */
  private void registerAllParents(Path path) {
    Path p = path;

    while (p != null) {
      try {
        register(p, false);
      } catch (Exception e) {
        logger.warn("Failed to recursively register parent path {}", path, e);
      }
      p = p.getParent();
    }

  }

  public boolean isRunning() {
    return isRunning;
  }

  /**
   *
   *
   * @throws IOException
   */
  public void start() throws IOException {
    if (isRunning) {
      throw new IllegalStateException("Watch service is already running");
    }

    if (usePolling) {
      startPolling();
    }

    startWatchService();
    isRunning = true;
  }

  /**
   *
   *
   * @throws IOException
   */
  private void startPolling() throws IOException {
    try {
      ((PollingWatchService) watcher).start();
    } catch (IOException exc) {
      throw new IOException("Failed to start watch service", exc);
    }
  }

  /**
   *
   *
   */
  private void startWatchService() {
    if (threadFactory != null) {
      thread = threadFactory.newThread(this);
    } else {
      thread = new Thread(this);
    }
    thread.start();
  }

  /**
   *
   *
   * @throws IOException
   */
  public void stop() throws IOException {
    if (!isRunning) {
      throw new IllegalStateException("Watch service is not running");
    }

    stopWatchService(0, null);
    isRunning = false;
  }

  /**
   *
   *
   * @param waitTimeout The amount of time to wait until the thread finished
   * @param timeUnit The unit of the provided time
   * @throws IOException
   */
  public void stop(long waitTimeout, TimeUnit timeUnit) throws IOException {
    if (!isRunning) {
      throw new IllegalStateException("Watch service is not running");
    }

    stopWatchService(waitTimeout, timeUnit);
    isRunning = false;
  }

  /**
   *
   *
   * @param waitTimeout The amount of time to wait until the thread finished. Not used if < 0.
   * @param timeUnit The unit of the provided time. Can be <code>null</code>.
   * @throws IOException
   */
  private void stopWatchService(long waitTimeout, TimeUnit timeUnit) throws IOException {
    watcher.close();

    try {
      thread.interrupt();
      if (waitTimeout >= 0 && timeUnit != null) {
        thread.join(timeUnit.toMillis(waitTimeout));
      }
    } catch (final InterruptedException exc) {
      Thread.currentThread().interrupt();
    }
  }

  @Override
  public void run() {

    while (isRunning) {

      WatchKey key = null;

      try {
        key = watcher.take();
      } catch (InterruptedException exc) {
        Thread.currentThread().interrupt();
        continue;
      } catch (ClosedWatchServiceException exc) {
        break;
      }

      Path dir = keys.get(key);

      processEvents(key, dir);

      boolean valid = key.reset();
      if (!valid) {
        // Directory not accessible any more -> remove it
        keys.remove(key);
      }

    }

    clearAllRegisteredPaths();

  }

  /**
   *
   *
   * @param key
   * @param dir
   */
  private void processEvents(WatchKey key, Path dir) {
    for (WatchEvent<?> event : key.pollEvents()) {
      Kind<?> kind = event.kind();

      // TODO is overflow handled correctly?
      if (event == StandardWatchEventKinds.OVERFLOW) {
        firePathWatcherListener(kind, dir, null, true);
        continue;
      }

      @SuppressWarnings("unchecked")
      WatchEvent<Path> ev = (WatchEvent<Path>) event;
      Path name = ev.context();
      Path child = dir.resolve(name);

      firePathWatcherListener(kind, dir, child, false);

      // Add new directories and their child directories to the watch
      if (Boolean.TRUE.equals(allChildren.get(key))
          && kind == StandardWatchEventKinds.ENTRY_CREATE) {
        if (child.toFile().isDirectory()) {
          registerAllChildren(child);
        }
      }

    }
  }



  /**************************************************************************
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


}
