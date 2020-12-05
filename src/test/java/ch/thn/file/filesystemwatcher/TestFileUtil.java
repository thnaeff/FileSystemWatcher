package ch.thn.file.filesystemwatcher;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 *
 *
 * @author Thomas Naeff (github.com/thnaeff)
 *
 */
public class TestFileUtil {

  public static final Path TEST_DIR = Path.of("target", "test_dir");

  static {
    if (!TEST_DIR.toFile().exists()) {
      if (!TEST_DIR.toFile().mkdirs()) {
        throw new RuntimeException("Failed to set up test directory "
            + TEST_DIR);
      }
    }
  }

  private TestFileUtil() {
    // Private. Only static methods.
  }

  public static Path newFile(String path, String content) throws IOException {
    File newFile = new File(TEST_DIR.toFile(), path);
    return newFile(newFile.toPath(), content);
  }

  public static Path newFile(Path path, String content) throws IOException {
    return fileOperation(path, content, StandardOpenOption.CREATE_NEW);
  }

  public static void appendToFile(Path path, String content) throws IOException {
    fileOperation(path, content, StandardOpenOption.APPEND);
  }

  private static Path fileOperation(Path path, String content, StandardOpenOption operation)
      throws IOException {
    if (!path.startsWith(TEST_DIR)) {
      File newFile = new File(TEST_DIR.toFile(), path.toFile().getPath());
      path = newFile.toPath();
    }
    File newFile = path.toFile();
    newFile.getParentFile().mkdirs();
    Path pathObj = newFile.toPath();
    Files.writeString(pathObj, content, operation);
    return pathObj;
  }

  public static Path testDirectory() {
    return TEST_DIR;
  }

  public static Path newDirectory(String path) throws IOException {
    File newDir = new File(TEST_DIR.toFile(), path);
    newDir.mkdirs();
    return newDir.toPath();
  }

  public static File[] getContent(String path) {
    File dir = new File(TEST_DIR.toFile(), path);
    if (!dir.exists()) {
      return new File[0];
    }
    return dir.listFiles();
  }

  public static File[] getContent(Path path) {
    if (!path.startsWith(TEST_DIR)) {
      File newFile = new File(TEST_DIR.toFile(), path.toFile().getPath());
      path = newFile.toPath();
    }
    File dir = path.toFile();
    if (!dir.exists()) {
      return new File[0];
    }
    return dir.listFiles();
  }


  public static File[] getContent() {
    File dir = TEST_DIR.toFile();
    if (!dir.exists()) {
      return new File[0];
    }
    return dir.listFiles();
  }

  public static void cleanup() throws IOException {
    if (TEST_DIR.toFile().exists()) {
      Files.walk(TEST_DIR).forEach(path -> path.toFile().delete());
      TEST_DIR.toFile().delete();
    }
  }

  public static void deleteDir(Path dir) throws IOException {
    if (dir.toFile().exists()) {
      Files.walk(dir).forEach(path -> path.toFile().delete());
      dir.toFile().delete();
    }
  }

}
