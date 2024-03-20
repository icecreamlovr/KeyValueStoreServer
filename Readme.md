# Key-Value Store Server and Client
This project implements a simple key-value store server and client application with gRPC as the RPC framework. The server allows clients to perform operations like putting, getting, and deleting key-value pairs, while the client provides a command-line interface for interacting with the server.

## Features
* **Communication Based on RPC:** The server and clients communicate using RPC instead of sockets. This is implemented by leveraging the gRPC framework. Protobuf file (`kvstore.proto`) is used as the IDL of the server.
* **Key-Value Store:** The server maintains a key-value store in memory, allowing clients to store, retrieve and delete data.
* **Concurrency:** The server can handle concurrent GET / PUT / DELETE requests from multiple clients. This is achieved by protecting the in-memory data store with a read-write lock. See `Concurrency` below for more details.
* **Pre-populated Requests:** The client pre-populates the server with a set of predefined requests for testing purposes.
* **Command-Line Interface:** The client provides a command-line interface for users to interact with the server by sending requests.
* **Error Handling:** Handles request timeouts, server unavailable and other error conditions at client side.
* **Logging:** Extensive logging of both requests, response and error conditions at both the client and the server.

## Components
### Server Interface Definition
* `kvstore.proto`: The protocol buffer file is the server's Interface Definition Language (IDL). It defines the service interface (the methods) and the message types for communication between the key-value store server and its clients.
* Service Interface:
  * `KeyValueStore`: Defines the service for putting, getting, and deleting key-value pairs.
  * `put`: RPC method for storing a key-value pair in the server.
    * `PutRequest`: Defines the structure of requests to store key-value pairs in the server.
    * `PutResponse`: Defines the structure of responses to put requests, indicating the success status of the operation.
  * `get`: RPC method for retrieving the value associated with a specific key from the server.
    * `GetRequest`: Defines the structure of requests to retrieve the value associated with a specific key.
    * `GetResponse`: Defines the structure of responses to get requests, containing the retrieved value.
  * `delete`: RPC method for deleting a key-value pair from the server.
    * `DeleteRequest`: Defines the structure of requests to delete a key-value pair from the server.
    * `DeleteResponse`: Defines the structure of responses to delete requests, indicating the success status of the operation.


### Server
The server application consists of the following components:

* **ServerApp:** Main class responsible for parsing CLI flag and starting the RPCServer.
* **RPCServer:** Class responsible for running the gRPC server and managing its lifecycle.
* **KeyValueStoreImpl:** Implementation of the gRPC service interface. Implements request handlers for `put`, `get` and `delete` methods.
* **DataStorage:** Class that implements an in-memory key-value store maintained by the server. Concurrent read and mutation accesses to the data store are handled using locks.
* **ServerLogger:** Utility class for logging server events.

### Client
The client application consists of the following components:

* **ClientApp:** Main class responsible for parsing CLI flags, starting the RPCClient and pre-populating requests.
* **RPCClient:** Implementation of the gRPC client. It instantiates the server stub and interacts with the server.
* **ClientLogger:** Utility class for logging client events.

### build.gradle
The Gradle build script (`build.gradle`) serves the following purposes:
- Manages project dependencies, specifically, the gRPC Java dependency packages required to compile and run this project.
- Provides directives to the `generateProto` gradle task. This allows integrating the proto-generated Java code in to my client and server code everytime I make changes to the `kvstore.proto` file.
- Defines custom gradle tasks (`task runServer` and `task runClient`), which allows executing the same binary with different entrance after compilation.

## Project structure
```
 |-Readme.md
 |-build.gradle
 |-src
   |-main
   | |-proto
   | | |-kvstore.proto
   | |-java
   | | |-kvstore
   | | | |-server
   | | | | |-ServerLogger.java
   | | | | |-ServerApp.java
   | | | | |-DataStorage.java
   | | | | |-KeyValueStoreImpl.java
   | | | | |-RPCServer.java
   | | | |-client
   | | | | |-RPCClient.java
   | | | | |-ClientApp.java
   | | | | |-ClientLogger.java
 |-output
   |-client-1.0-SNAPSHOT.jar
   |-server-1.0-SNAPSHOT.jar
```
## Usage

Gradle will be required to compile and run the client and the server. If gradle is not available, follow alternative ways to run JAR directly in the `Alternative Usage (Without Gradle)` section instead.

### 1. Install prerequisites
It is required to install the gradle binary before compiling and running this project. Also, Java 17 or above is required to compile the project.

- Java (17 or above)
  - Download openjdk and install: https://jdk.java.net/archive/
  - Make sure to download the version that is compatible with your hardware

- Gradle (8.0 or above)
  - On mac, install using sdkman https://sdkman.io/ by running the following commands:

```bash
$ curl -s "https://get.sdkman.io" | bash
$ sdk install gradle 8.0
```

### 2. Compile
* Compile the code using `gradle`. Make sure to `cd` to the root directory of the project where `build.gradle` is in the pwd.
```
> gradle clean
> gradle build
```

Note the `> gradle build` command executes multiple gradle tasks in series. This includes `> gradle generateProto`, which creates protobuf-generated Java classes from `src/main/proto/kvstore.proto`, and place them under `build/generated`.

### 3. Start server
To start the server, run the `gradle runServer` custom task with port in --args (This is how to pass CLI flags when running via gradle): `gradle runServer --args "<port-number>"`

Example:
```
> gradle runServer --args "33333"
```

This command will start the server at port `33333` in localhost.

### 4. Start client
To start the client, run the `gradle runClient` custom task with server IP and port in --args  (This is how to pass CLI flags when running via gradle) `gradle runClient --args "<server-ip> <port-number>"`

Example:
```
> gradle runClient --args "127.0.0.1 33333"
```
Once the client is running, you will see **5 automated sample requests of GET, PUT and DELETE** each. 

Then you can interact with the server using the following commands:

* **PUT:** Store a key-value pair in the server.
* **GET:** Retrieve the value associated with a key from the server.
* **DELETE:** Remove a key-value pair from the server.

## Alternative Usage (Without Gradle)

I have also compiled the server and the client JAR files and placed them in `output/`. In case gradle is not available, use the following commands to **run the compiled JAR files directly**:

### 1. Start server

Run `output/server-1.0-SNAPSHOT.jar` directly with `java -jar`, and provide server port number. Example:

```
> java -jar ./output/server-1.0-SNAPSHOT.jar 22222
```

This command will start the server at port 22222 in localhost.

### 2. Start client

Run `output/client-1.0-SNAPSHOT.jar` directly with `java -jar`, and provide server IP and server port. Example:

```
> java -jar ./output/client-1.0-SNAPSHOT.jar localhost 22222
```

This command will start the client and connect to the server running on port 22222 in localhost.

## Accepted User Input Format
* Space is used to separate request type and data
* Only English letter a-z, A-Z, and numbers will be accepted as content of keys or values.

* Template:
  ```
  put <key> <value>
  get <key>
  delete <key>
  ```
* Example:
  ```
  put apple fruit
  put 1 10
  get apple
  delete 1
  ```

## Concurrency

The gRPC framework supports multi-threading natively - each method handlers in `KeyValueStoreImpl.java` is spawned in its own thread. The thread pool can be tuned for better performance but I did not explore that in this project.

On the other hand, synchronization is needed at the server end to support concurrent client operations. In my project, concurrency is supported by coordinating the access to the data store with `a pair of read-write locks` in DataStorage.java.

- The lock objects are instantiated when the DataStorage class instantiates.
- The DataStorage get() method acquires and releases the `read` lock. The put() and delete() methods acquires and releases the `write` lock. This means multiple get operations can be parallelized, whereas get-put, get-delete, put-delete, put-put etc. can't be parallelized.
- **Why not mutex lock?** Mutex lock can also be used to synchronize get, put and delete operations. However **read-write lock provides better parallelization** as it allows multiple get requests to be handled concurrently at the server, increasing the performance of the server.
- The only `critical section` protected by the lock is the data store access. Other logics at the server end, such as request parsing and response building are local to the threads. Also I am assuming the `System.out.println()` in ServerLogger [is thread safe](https://ioflood.com/blog/system-out-println/). If in certain JVM implementations this is not, another mutex lock needs to be introduced to `ServerLogger.java` whenever it prints to STDOUT or STDERR.

## Logging
* Client
  * Client logs the response received from the server.
  * Client also logs when the user input is malformed. For example, if the user mis-spell PUT as PT.
  * Client also logs when it timeouts receiving server response.
* Server
  * Server logs both the requests it receives from the client, and the response it sends to the client.
  * Server also logs error events. For example, if the key-value store receives attempts to delete a non-existing key. Such operations are disallowed at the handler layer. However if for any reason it's slipped to the data layer (e.g. due to race condition), this will be logged.
