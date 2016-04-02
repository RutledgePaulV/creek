[![Build Status](https://travis-ci.org/RutledgePaulV/creek.svg?branch=develop)](https://travis-ci.org/RutledgePaulV/creek)
[![Coverage Status](https://coveralls.io/repos/github/RutledgePaulV/creek/badge.svg?branch=develop)](https://coveralls.io/github/RutledgePaulV/creek?branch=develop)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.rutledgepaulv/creek/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.github.rutledgepaulv/creek)

# Creek

Creek is a library for working with Java byte streams (InputStream, OutputStream). I created
it because I often found myself in situations where I wanted to be efficient about memory
usage and yet have several different readers that need access to the request stream in a web
request (which, by default, is only traversable once). 

Most articles and stackoverflow answers you see on how to re-use a request stream just blindly
tell users to wrap it in a BufferedInputStream or just call "toByteArray". Depending on your
application this can be a pretty dangerous thing to do. Imagine you're letting a user upload
videos through your application and they just uploaded a 2GB file. You now need 2GB in your
heap just to support that one user if you use either of the often suggested approaches.

Name inspired by "being up a creek without a paddle" and "creek" being an alternative for "stream".


## Creek provides better ways
Creek provides what are known in the signal community as "demuxers" / "demultiplexers". These
take a single source signal and split it into many. Where the benefit comes from is in the relationship
of each split stream to the source stream. The internals of each demux use different strategies
to keep the total heap memory footprint low while still allowing multiple readers to make progress.


#### TempFileStreamDemux
This demux just writes the stream to a temporary file (on disk) which can then be used
to generate multiple handles to the same data. The temporary file is deleted once each
reader has been closed. 

#### FollowTheLeaderStreamDemux
This demux maintains a buffer that contains only the bytes between the furthest-along
reader and the furthest-behind reader of all the streams split from the demux. This means
that if you can read from each stream concurrently at about the same rate that the buffer
will remain very small. The underlying source stream will close and the buffer will empty
once every split stream has been closed.


## Usage
<br/>

#### TempFileStreamDemux
```java
Supplier<InputStream> demux = new TempFileStreamDemux(request.getInputStream());

InputStream stream1 = demux.get();
InputStream stream2 = demux.get();
InputStream stream3 = demux.get();

// each of these should close the stream when done using it
MimeType mime = detectMimeType(stream1);
String text = extractTextualContent(stream2);
persistStream(stream3);
```



#### FollowTheLeaderStreamDemux
```java
Supplier<InputStream> demux = new FollowTheLeaderStreamDemux(request.getInputStream());

InputStream stream1 = demux.get();
InputStream stream2 = demux.get();
InputStream stream3 = demux.get();

// each of these should close the stream when done using it
CompletableFuture<?> future1 = spawnDetectMimetypeThread(stream1);
CompletableFuture<?> future2 = spawnExtractTextualContentThread(stream2);
CompletableFuture<?> future3 = spawnPersistThread(stream3);

CompletableFuture<Void> afterAll = CompletableFuture.allOf(future1, future2, future3);

// wait for each future to finish (or maintain your asynchronous
// context until the last moment like a good reactive dev)
afterAll.get();
```


#### Release Versions
```xml
<dependencies>
    <dependency>
        <groupId>com.github.rutledgepaulv</groupId>
        <artifactId>creek</artifactId>
        <version>0.5</version>
    </dependency>
</dependencies>
```

#### Snapshot Versions
```xml
<dependencies>
    <dependency>
        <groupId>com.github.rutledgepaulv</groupId>
        <artifactId>creek</artifactId>
        <version>0.6-SNAPSHOT</version>
    </dependency>
</dependencies>

<repositories>
    <repository>
        <id>ossrh</id>
        <name>Repository for snapshots</name>
        <url>https://oss.sonatype.org/content/repositories/snapshots</url>
        <snapshots>
            <enabled>true</enabled>
        </snapshots>
    </repository>
</repositories>
```

### License

This project is licensed under [MIT license](http://opensource.org/licenses/MIT).
