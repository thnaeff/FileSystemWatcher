# FileSystemWatcher

Watch one or multiple paths for file or directory changes, locally or on a network directory

---


[![License](https://img.shields.io/badge/License-Apache_v2.0-802879.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)
[![Java Version](https://img.shields.io/badge/Java-1.6%2B-2E6CB8.svg)](https://java.com)
[![Apache Maven ready](https://img.shields.io/badge/Apache_Maven_ready-3.3.9%2B-FF6804.svg)](https://maven.apache.org/)


---

The file system watcher can be set up to use the system watch service that comes with java, or use a polling 
watch service in case the java watch service is not available.


**Example:**

```java

// Using the system file watch service
FileSystemWatcher watcher = new FileSystemWatcher();
// Or using a polling service
FileSystemWatcher watcher = new FileSystemWatcher(10, TimeUnit.SECONDS);


// Add some listener.
// See 'TestListener' example below.
watcher.addPathWatcherListener(new TestListener());

// Start watcher thread
watcher.start();

// ...

// End watcher thread
watcher.stop();


```


--------------------------------------

## The PathWatcherListener Interface


```java

class TestListener implements PathWatcherListener {

    @Override
    public void newPathWatched(Path path) {

      //...

    }

    @Override
    public void pathChanged(Path path, Path context, boolean overflow) {

      //...

    }

    @Override
    public void directoryCreated(Path path, Path created) {

      //...

    }

    @Override
    public void directoryDeleted(Path path, Path deleted) {

      //...

    }

    @Override
    public void directoryModified(Path path, Path modified) {

     	//...

    }

  }
  
```

---

# Building the Project

<img src="http://maven.apache.org/images/maven-logo-black-on-white.png" alt="Built with Maven" width="150">

This project can be built with Maven

Maven command:

```
$ mvn clean install
```

pom.xml entry in your project:

```xml
<dependency>
	<groupId>ch.thn.file</groupId>
	<artifactId>filesystemwatcher</artifactId>
	<version>0.5.0-SNAPSHOT</version>
</dependency>
```

---

