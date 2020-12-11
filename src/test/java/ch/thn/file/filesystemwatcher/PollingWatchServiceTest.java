package ch.thn.file.filesystemwatcher;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import ch.thn.file.filesystemwatcher.PollingWatchService.PollingWatchEvent;
import ch.thn.file.filesystemwatcher.PollingWatchService.PollingWatchKey;
import org.awaitility.Awaitility;
import org.awaitility.core.DurationFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 *
 *
 * @author Thomas Naeff (github.com/thnaeff)
 *
 */
public class PollingWatchServiceTest {

  private PollingWatchService service;

  @Before
  public void setup() throws Exception {
    TestFileUtil.cleanup();
  }

  @After
  public void closeUp() throws Exception {

    // Ensure all tests stop the service
    if (service != null && service.isReady()) {
      service.close();
      Awaitility.await().atMost(200, TimeUnit.MILLISECONDS).until(() -> !service.isReady());
    }

  }



  @Test(timeout = 1000)
  public void testServiceStartsAndStops() throws Exception {
    service = new PollingWatchService(1, TimeUnit.SECONDS);

    Path checkDir = TestFileUtil.newTestDirectory();
    service.register(checkDir);
    service.start();

    Awaitility.await().atMost(200, TimeUnit.MILLISECONDS).until(service::isReady);
    assertTrue(service.isReady());

    service.close();

    Awaitility.await().atMost(200, TimeUnit.MILLISECONDS).until(() -> !service.isReady());
    assertFalse(service.isReady());
  }


  @Test(timeout = 3000)
  public void testPollInterval2Seconds() throws Exception {
    int pollIntervalMilliSeconds = 2000;
    service = new PollingWatchService(pollIntervalMilliSeconds, TimeUnit.MILLISECONDS);

    Path checkDir = TestFileUtil.newTestDirectory();
    service.register(checkDir);
    service.start();

    Awaitility.await().atMost(200, TimeUnit.MILLISECONDS).until(service::isReady);

    TestFileUtil.newFile("testFile.txt", "a test file");

    // The event should be there in no more than the poll interval, and also not earlier.
    // Giving it a little bigger range because the polling time starts earlier when the service
    // starts.
    Duration atLeast = DurationFactory.of(pollIntervalMilliSeconds - 500, TimeUnit.MILLISECONDS);
    Duration atMost = DurationFactory.of(pollIntervalMilliSeconds + 500, TimeUnit.MILLISECONDS);
    Awaitility.await().between(atLeast, atMost).until(() -> service.take() != null);

  }


  @Test(timeout = 6000)
  public void testPollInterval5Seconds() throws Exception {
    int pollIntervalMilliSeconds = 5000;
    service = new PollingWatchService(pollIntervalMilliSeconds, TimeUnit.MILLISECONDS);

    Path checkDir = TestFileUtil.newTestDirectory();
    service.register(checkDir);
    service.start();

    Awaitility.await().atMost(200, TimeUnit.MILLISECONDS).until(service::isReady);

    TestFileUtil.newFile("testPollInterval5Seconds.txt", "a test file");

    // The event should be there in no more than the poll interval, and also not earlier.
    // Giving it a little bigger range because the polling time starts earlier when the service
    // starts.
    Duration atLeast = DurationFactory.of(pollIntervalMilliSeconds - 500, TimeUnit.MILLISECONDS);
    Duration atMost = DurationFactory.of(pollIntervalMilliSeconds + 500, TimeUnit.MILLISECONDS);
    Awaitility.await().between(atLeast, atMost).until(() -> service.take() != null);

  }


  @Test(timeout = 3000)
  public void testSingleDirWatchFileCreate() throws Exception {
    service = new PollingWatchService(1, TimeUnit.SECONDS);

    Path checkDir = TestFileUtil.newTestDirectory();
    service.register(checkDir);
    service.start();

    Awaitility.await().atMost(200, TimeUnit.MILLISECONDS).until(service::isReady);

    // Create file
    Path newFile = TestFileUtil.newFile("testFile.txt", "a test file");

    PollingWatchKey key = service.take();

    assertThat(key, notNullValue());
    // The key contains the path that was used to register
    assertThat(key.getRegisteredPath(), is(checkDir));

    List<WatchEvent<?>> events = key.pollEvents();
    assertThat(events.size(), is(1));

    PollingWatchEvent event = (PollingWatchEvent) events.get(0);
    // The event contains the actual path that the event is for
    assertThat(event.context(), is(newFile));
    assertThat(event.count(), is(1));
    assertThat(event.kind(), is(StandardWatchEventKinds.ENTRY_CREATE));

  }



  @Test(timeout = 3000)
  public void testSingleDirWatchFileCreateWithOtherExistingFile() throws Exception {
    service = new PollingWatchService(2, TimeUnit.SECONDS);

    Path existingFile =
        TestFileUtil.newFile("existingFile.txt", "a file that exists before watching starts");

    Path checkDir = TestFileUtil.newTestDirectory();
    assertThat(TestFileUtil.getContent(checkDir).length, is(1));
    service.register(checkDir);
    service.start();

    Awaitility.await().atMost(200, TimeUnit.MILLISECONDS).until(service::isReady);

    // Create file
    Path newFile = TestFileUtil.newFile("testFile.txt", "a test file");

    PollingWatchKey key = service.take();

    assertThat(key, notNullValue());
    // The key contains the path that was used to register
    assertThat(key.getRegisteredPath(), is(checkDir));

    List<WatchEvent<?>> events = key.pollEvents();
    assertThat(events.size(), is(1));

    PollingWatchEvent event = (PollingWatchEvent) events.get(0);
    // The event contains the actual path that the event is for
    assertThat(event.context(), is(newFile));
    assertThat(event.count(), is(1));
    assertThat(event.kind(), is(StandardWatchEventKinds.ENTRY_CREATE));

    assertTrue(existingFile.toFile().exists());

  }


  @Test(timeout = 4000)
  public void testSingleDirWatchFileDelete() throws Exception {
    service = new PollingWatchService(2, TimeUnit.SECONDS);

    Path existingFile =
        TestFileUtil.newFile("existingFile.txt", "a file that exists before watching starts");

    Path checkDir = TestFileUtil.newTestDirectory();
    assertThat(TestFileUtil.getContent(checkDir).length, is(1));
    service.register(checkDir);
    service.start();

    Awaitility.await().atMost(200, TimeUnit.MILLISECONDS).until(service::isReady);

    // Delete file
    existingFile.toFile().delete();
    Awaitility.await().atMost(200, TimeUnit.MILLISECONDS)
        .until(() -> !existingFile.toFile().exists());

    PollingWatchKey key = service.take();

    assertThat(key, notNullValue());
    // The key contains the path that was used to register
    assertThat(key.getRegisteredPath(), is(checkDir));

    List<WatchEvent<?>> events = key.pollEvents();
    assertThat(events.size(), is(1));

    PollingWatchEvent event = (PollingWatchEvent) events.get(0);
    // The event contains the actual path that the event is for
    assertThat(event.context(), is(existingFile));
    assertThat(event.count(), is(1));
    assertThat(event.kind(), is(StandardWatchEventKinds.ENTRY_DELETE));

  }


  @Test(timeout = 2000)
  public void testSingleDirWatchFileModify() throws Exception {
    service = new PollingWatchService(1, TimeUnit.SECONDS);

    Path existingFile =
        TestFileUtil.newFile("existingFile.txt", "a file that exists before watching starts");

    Path checkDir = TestFileUtil.newTestDirectory();
    assertThat(TestFileUtil.getContent(checkDir).length, is(1));
    service.register(checkDir);
    service.start();

    Awaitility.await().atMost(200, TimeUnit.MILLISECONDS).until(service::isReady);

    // Modify file
    TestFileUtil.appendToFile(existingFile, "\nappended content");

    PollingWatchKey key = service.take();

    assertThat(key, notNullValue());
    // The key contains the path that was used to register
    assertThat(key.getRegisteredPath(), is(checkDir));

    List<WatchEvent<?>> events = key.pollEvents();
    assertThat(events.size(), is(1));

    PollingWatchEvent event = (PollingWatchEvent) events.get(0);
    // The event contains the actual path that the event is for
    assertThat(event.context(), is(existingFile));
    assertThat(event.count(), is(1));
    assertThat(event.kind(), is(StandardWatchEventKinds.ENTRY_MODIFY));

  }


  @Test(timeout = 3000)
  public void testSingleDirWatchDirDelete() throws Exception {
    service = new PollingWatchService(1, TimeUnit.SECONDS);

    // Two files in directory before watching starts
    Path existingFile1 =
        TestFileUtil.newFile("existingFile1.txt", "a file 1 that exists before watching starts");
    Path existingFile2 =
        TestFileUtil.newFile("existingFile2.txt", "a file 2 that exists before watching starts");

    Path checkDir = TestFileUtil.newTestDirectory();
    assertThat(TestFileUtil.getContent(checkDir).length, is(2));
    service.register(checkDir);
    service.start();

    Awaitility.await().atMost(200, TimeUnit.MILLISECONDS).until(service::isReady);

    // Deleting the actual registered watch directory
    TestFileUtil.deleteDir(checkDir);
    Awaitility.await().atMost(500, TimeUnit.MILLISECONDS).until(() -> !checkDir.toFile().exists());

    int totalExpectedEvents = 2;
    List<Path> registeredPaths = new ArrayList<>();
    List<Kind<?>> kinds = new ArrayList<>();
    List<Path> paths = new ArrayList<>();
    List<Integer> counts = new ArrayList<>();

    Awaitility.await().atMost(2, TimeUnit.SECONDS).until(() -> {
      PollingWatchKey key = service.take();
      registeredPaths.add(key.getRegisteredPath());
      assertThat(key, notNullValue());
      List<WatchEvent<?>> events = key.pollEvents();
      events.stream().map((event) -> event.kind()).forEach(kinds::add);
      events.stream().map((event) -> event.count()).forEach(counts::add);
      events.stream().map((event) -> ((PollingWatchEvent) event).context()).forEach(paths::add);
      return kinds.size() == totalExpectedEvents;
    });

    assertThat(kinds.size(), is(totalExpectedEvents));
    assertThat(registeredPaths, containsInAnyOrder(checkDir));
    assertThat(kinds, containsInAnyOrder(StandardWatchEventKinds.ENTRY_DELETE,
        StandardWatchEventKinds.ENTRY_DELETE));
    assertThat(paths, containsInAnyOrder(existingFile1, existingFile2));
    assertThat(counts, containsInAnyOrder(1, 1));

  }


  @Test(timeout = 3000)
  public void testSingleDirDirectoryDelete() throws Exception {
    service = new PollingWatchService(1, TimeUnit.SECONDS);

    Path existingDir = TestFileUtil.newDirectory("testDir");

    Path checkDir = TestFileUtil.newTestDirectory();
    assertThat(TestFileUtil.getContent(checkDir).length, is(1));
    service.register(checkDir);
    service.start();

    Awaitility.await().atMost(200, TimeUnit.MILLISECONDS).until(service::isReady);

    // Delete directory
    existingDir.toFile().delete();
    Awaitility.await().atMost(200, TimeUnit.MILLISECONDS)
        .until(() -> !existingDir.toFile().exists());

    PollingWatchKey key = service.take();

    assertThat(key, notNullValue());
    // The key contains the path that was used to register
    assertThat(key.getRegisteredPath(), is(checkDir));

    List<WatchEvent<?>> events = key.pollEvents();
    assertThat(events.size(), is(1));

    PollingWatchEvent event = (PollingWatchEvent) events.get(0);
    // The event contains the actual path that the event is for
    assertThat(event.context(), is(existingDir));
    assertThat(event.count(), is(1));
    assertThat(event.kind(), is(StandardWatchEventKinds.ENTRY_DELETE));

  }

  @Test(timeout = 3000)
  public void testSingleDirDirectoryCreate() throws Exception {
    service = new PollingWatchService(1, TimeUnit.SECONDS);

    Path checkDir = TestFileUtil.newTestDirectory();
    assertThat(TestFileUtil.getContent(checkDir).length, is(0));
    service.register(checkDir);
    service.start();

    Awaitility.await().atMost(200, TimeUnit.MILLISECONDS).until(service::isReady);

    // Create directory
    Path newDir = TestFileUtil.newDirectory("testDir");
    Awaitility.await().atMost(200, TimeUnit.MILLISECONDS).until(() -> newDir.toFile().exists());

    PollingWatchKey key = service.take();

    assertThat(key, notNullValue());
    // The key contains the path that was used to register
    assertThat(key.getRegisteredPath(), is(checkDir));

    List<WatchEvent<?>> events = key.pollEvents();
    assertThat(events.size(), is(1));

    PollingWatchEvent event = (PollingWatchEvent) events.get(0);
    // The event contains the actual path that the event is for
    assertThat(event.context(), is(newDir));
    assertThat(event.count(), is(1));
    assertThat(event.kind(), is(StandardWatchEventKinds.ENTRY_CREATE));

  }



}
