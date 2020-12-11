package ch.thn.file.filesystemwatcher;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.junit.Before;
import org.junit.Test;


/**
 *
 *
 * @author Thomas Naeff (github.com/thnaeff)
 *
 */
public class ListenerTest {


  @Before
  public void setup() throws Exception {
    TestFileUtil.cleanup();
  }

  @Test
  public void testListener() throws Exception {

    FileSystemWatcher watcher = new FileSystemWatcher();
    watcher.create();
    TestListener testListener = new TestListener();
    watcher.addPathWatcherListener(testListener);
    watcher.start();

    // Give thread time to start...
    Awaitility.await().atMost(1, TimeUnit.SECONDS).until(() -> watcher.isRunning());


    Path testDir = TestFileUtil.newTestDirectory();
    File newDir = new File(testDir.toFile(), "test_new");

    watcher.registerPath(testDir, true, true);


    Collection<Path> paths = watcher.getWatchedPaths();

    assertTrue(paths.containsAll(testListener.watchedPaths));



    System.out.println("> Creating "
        + newDir.getPath());
    boolean res = newDir.mkdir();

    // Check that creation worked
    assertTrue(res);

    System.out.println("> Deleting "
        + newDir.getPath());
    res = newDir.delete();

    // Check that deletion worked
    assertTrue(res);

    // Give it time to react
    Awaitility.await().atMost(1, TimeUnit.SECONDS).until(testListener.createDetected::size,
        is(greaterThan(0)));
    Awaitility.await().atMost(1, TimeUnit.SECONDS).until(testListener.deleteDetected::size,
        is(greaterThan(0)));


    System.out.println("All created: "
        + testListener.createDetected);
    System.out.println("All deleted: "
        + testListener.deleteDetected);

    // Check that creation and deletion was reported by watcher
    assertThat(testListener.createDetected, hasItem(newDir.toPath()));
    assertThat(testListener.deleteDetected, hasItem(newDir.toPath()));


    System.out.println("Shutting down watcher thread...");
    watcher.stop();
    Awaitility.await().atMost(1, TimeUnit.SECONDS).until(() -> !watcher.isRunning());
    System.out.println("Done");

  }


}
