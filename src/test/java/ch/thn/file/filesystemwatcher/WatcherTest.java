package ch.thn.file.filesystemwatcher;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;

import org.junit.Test;


/**
 *
 *
 * @author Thomas Naeff (github.com/thnaeff)
 *
 */
public class WatcherTest {


  @Test
  public void testAddChildrenAndParents() throws Exception {

    FileSystemWatcher watcher = new FileSystemWatcher();

    File f = new File("target/classes");

    watcher.registerPath(f.toPath(), true, true);

    System.out.println(watcher.getWatchedPaths());

    Path childPath = f.listFiles()[0].toPath();
    Path parentPath = f.toPath().getParent();

    System.out.println("Child: "
        + childPath);
    System.out.println("Parent: "
        + parentPath);
    System.out.println("Working: "
        + f.toPath());

    Collection<Path> paths = watcher.getWatchedPaths();

    // Check that the chosen path and at least one child and one parent is added
    assertThat(paths, hasItem(childPath));
    assertThat(paths, hasItem(parentPath));
    assertThat(paths, hasItem(f.toPath()));

  }



}
