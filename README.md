# FileSystemWatcher

Watch one or multiple paths for file or directory changes, locally or on a network directory

---


[![License](https://img.shields.io/badge/License-Apache_v2.0-802879.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)
[![Java Version](https://img.shields.io/badge/Java-1.6%2B-2E6CB8.svg)](https://java.com)
[![Apache Maven ready](https://img.shields.io/badge/Apache Maven ready-3.3.9%2B-FF6804.svg)](https://maven.apache.org/)


---

The file system watcher can be set up to use the system watch service that comes with java, or use a polling 
watch service in case the java watch service is not available.


Example:

```java

FileSystemWatcher watcher = new FileSystemWatcher();

//Add some listener
watcher.addPathWatcherListener(new TestListener());

// Needs to be properly used in a thread for the reporting
Thread t = new Thread(watcher);
t.start();

//...

//End watcher thread
watcher.stop();


```


--------------------------------------

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


<img src="http://maven.apache.org/images/maven-logo-black-on-white.png" alt="Built with Maven" width="150">

This project can be built with Maven

Maven command:
```
$ mvn clean install
```

pom.xml entry in your project:
```
<dependency>
	<groupId>ch.thn.file</groupId>
	<artifactId>filesystemwatcher</artifactId>
	<version>0.0.1-SNAPSHOT</version>
</dependency>
```

---

