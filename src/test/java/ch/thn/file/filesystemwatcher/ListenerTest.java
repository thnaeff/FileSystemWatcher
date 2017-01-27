package ch.thn.file.filesystemwatcher;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.Test;


/**
 * 
 * 
 * @author Thomas Naeff (github.com/thnaeff)
 *
 */
public class ListenerTest {


  private Collection<Path> watchedPaths = new ArrayList<>();

  private List<Path> changeDetected = new ArrayList<>();
  private List<Path> createDetected = new ArrayList<>();
  private List<Path> deleteDetected = new ArrayList<>();
  private List<Path> modifyDetected = new ArrayList<>();


  @Test
  public void testListener() throws Exception {

    FileSystemWatcher watcher = new FileSystemWatcher();
    watcher.addPathWatcherListener(new TestListener());

    // Needs to be properly used in a thread for the reporting
    Thread t = new Thread(watcher);
    t.start();

    // Give thread time to start...
    Thread.sleep(1000);

    File f = new File("target/classes");
    File f2 = new File(f.getPath() + "/test_new");

    // Clean up first if path exists
    if (f2.exists()) {
      f2.delete();
    }

    watcher.registerPath(f.toPath(), true, true);


    Collection<Path> paths = watcher.getWatchedPaths();

    assertTrue(paths.containsAll(watchedPaths));



    System.out.println("> Creating " + f2.getPath());
    boolean res = f2.mkdir();

    // Check that creation worked
    assertTrue(res);

    System.out.println("> Deleting " + f2.getPath());
    res = f2.delete();

    // Check that deletion worked
    assertTrue(res);


    // Check that creation and deletion was reported by watcher
    assertThat(createDetected, hasItem(f2.toPath()));
    assertThat(deleteDetected, hasItem(f2.toPath()));

    
    System.out.println("Shutting down watcher thread...");
    watcher.stop(true);
    System.out.println("Done");

  }



  /********************************************************************************
   * 
   * 
   * @author Thomas Naeff (github.com/thnaeff)
   *
   */
  private class TestListener implements PathWatcherListener {

    @Override
    public void newPathWatched(Path path) {

      System.out.println("New path watched: " + path);
      watchedPaths.add(path);

    }

    @Override
    public void pathChanged(Path path, Path context, boolean overflow) {

      System.out.println("Changed:");
      System.out.println("  Path=" + path);
      System.out.println("  Change=" + context);
      changeDetected.add(path);

    }

    @Override
    public void directoryCreated(Path path, Path created) {

      System.out.println("Created:");
      System.out.println("  Path=" + path);
      System.out.println("  New=" + created);
      createDetected.add(created);

    }

    @Override
    public void directoryDeleted(Path path, Path deleted) {

      System.out.println("Deleted:");
      System.out.println("  Path=" + path);
      System.out.println("  Del=" + deleted);
      deleteDetected.add(deleted);

    }

    @Override
    public void directoryModified(Path path, Path modified) {

      System.out.println("Modified:");
      System.out.println("  Path=" + path);
      System.out.println("  Mod=" + modified);
      modifyDetected.add(modified);

    }

  }

}
