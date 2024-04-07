# Key-Value Store Server and Client
This project implements a distributed key-value store. The server can be started multiple times on different IP-Port combination.
Each replicas maintains their own in-memory key-value data store, and attempts to sync each other on updates, following **two-phase commit** protocol.

A client can communicate any of the server replicas, and issue GET, PUT and DELETE RPC calls. Upon receiving GET, a server replica simply returns
the value from its local data copy. Upon receiving PUT and DELETE, a server replica would execute two-phase commit protocol, acting as both the coordinator and one of the replicas. It first issues `prepare`
to all the replicas (including itself), to try to acquire the necessary resources (in this project, a **write lock** on the particular data row, in
the local in-memory data store). If all replicas reply ready, the coordinator would then issue `commit` to all the replicas, to apply the change and release the locks.
If any replica replies not ready (e.g. because the local data row is locked at the moment), the coordinator would then issue `abort` to all the replicas, releasing the locks.

## Features
* **Distributed Key-Value Store:** The server can be instantiated multiple times with different IP-Port combinations, effectively forming a distributed key-value store with multiple replicas.
* **Strict Consistency:** Each server replica maintains their own copy of the data store. When processing write requests, the replicas execute two-phase commit protocol to ensure the data in all the local copies are in-sync.
* **Communication Based on RPC:** Client-server, server-server communicate using RPC. The server exposes 3 APIs for client to communicate - GET, PUT, DELETE. The server also exposes 3 APIs for other server replicas to communicate - prepare, commit, abort.
* **Flexible Replica Selection:** A client can choose which server replica to talk to by passing different server IP/Port from CLI.
* **Concurrency:** The cluster of server replicas can handle high concurrency of GET / PUT / DELETE requests from multiple clients.
* **Pre-populated Requests:** The client (optionally) pre-populates the server with a set of predefined requests for testing purposes.
* **Error Handling:** Handles request timeouts, server unable to acquire resources, RPC timeouts, server unavailable and other error conditions.
* **Logging:** Extensive logging of both requests, response and error conditions at both the client and the server.

## Usage

Gradle (8.0 or above) and Java (17 or above) will be required to compile and run the client and the server.

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
To start the server, run the `gradle runServer` custom task with port and replica ports in --args (This is how to pass CLI flags when running via gradle): `gradle runServer --args "<port-number> [<all-replicas-port-number>]"`

```bash
Usage: ServerApp <port-number> [<all-replicas-port-number>]
  <port-number>: Port number of this replica. Must be between 0 and 65535
  <all-replicas-port-number>: Port numbers of the other replicas. Specify as comma-separated integers
      Optional flag. If unspecified, will use default value [3333, 3334, 3335, 3336, 3337]
```

Example:
```
> gradle runServer --args "3333 3333,3334,3335,3336,3337"
```

This command will start the server at port `3333` in localhost. It also tells this server that there are totally 5 replicas it needs to sync data with.

### 4. Start multiple server replicas
To start multiple server replicas, open multiple terminals and run the `gradle runServer` custom task multiple times. Note the each server replica's port must be different from each other, and they need to match what's specified in the <all-replicas-port-number>.

Example:
```
# Terminal 1
> gradle runServer --args "3333 3333,3334,3335,3336,3337"

# Terminal 2
> gradle runServer --args "3334 3333,3334,3335,3336,3337"

# Terminal 3
> gradle runServer --args "3335 3333,3334,3335,3336,3337"

# Terminal 4
> gradle runServer --args "3336 3333,3334,3335,3336,3337"

# Terminal 5
> gradle runServer --args "3337 3333,3334,3335,3336,3337"
```

This starts 5 server replicas at port `3333` to `3337` in localhost.

### 5. Start client
To start the client, run the `gradle runClient` custom task with server IP and port in --args  (This is how to pass CLI flags when running via gradle) `gradle runClient --args "<server-ip> <port-number>"`

```bash
Usage: ClientApp <server-ip> <server-port> [--skip-prepopulate]
  <server-ip>: IP of the server
  <server-port>: Port numbers of the server replica to talk to
  --skip-prepopulate: Optional flag. If specified, skips prepopulating 5 PUTs, 5 GETs and 5 DELETEs
```

You can also start different clients and talk to different server relicas

Example:
```
# Terminal 6
> gradle runClient --args "127.0.0.1 3333"

# Terminal 7
> gradle runClient --args "127.0.0.1 3335 --skip-prepopulate"
```

Once the client is running, you will see **5 automated sample requests of GET, PUT and DELETE** each, unless `--skip-prepopulate` is specified in the command line.

Then you can interact with the server using the following commands:

* **GET:** Retrieve the value associated with a key from the server.
* **PUT:** Store a key-value pair in the server.
* **DELETE:** Remove a key-value pair from the server.

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
   | | | | |-Coordinator.java
   | | | | |-ServerLogger.java
   | | | | |-ServerApp.java
   | | | | |-DataStorage.java
   | | | | |-KeyValueStoreImpl.java
   | | | | |-RPCServer.java
   | | | |-client
   | | | | |-RPCClient.java
   | | | | |-ClientApp.java
   | | | | |-ClientLogger.java
```

## Components
### Server Interface Definition
* `kvstore.proto`: The protocol buffer file is the server's Interface Definition Language (IDL). It defines the service interface (the methods)
and the message types for communication between the key-value store server and its clients.
* Service Interface:
  * `KeyValueStore`: Defines the service for putting, getting, and deleting key-value pairs.
  * `put`: RPC method for client to store a key-value pair in the server.
  * `get`: RPC method for client to retrieve the value associated with a specific key from the server.
  * `delete`: RPC method for client to delete a key-value pair from the server.
  * `prepare`: RPC method for replica to acquire necessary resources and reply its readiness to the coordinator. This implements phase one in the two-phase commit protocol.
  * `commit`: RPC method for replica to apply changes and release resources. This implements phase two (commit path) in the two-phase commit protocol.
  * `abort`: RPC method for replica to abort changes and release resources. This implements phase two (abort path) in the two-phase commit protocol.

### Server
The server application consists of the following components:

* **ServerApp:** Main class responsible for parsing CLI flag and starting the RPCServer.
* **RPCServer:** Class responsible for running the gRPC server and managing its lifecycle.
* **KeyValueStoreImpl:** Implementation of the gRPC service interface. Implements request handlers for `put`, `get` and `delete` methods.
* **Coordinator:** Implementation of the coordinator that executes two-phase commit by issuing `prepare`, `commmit` and `abort` to other replicas.
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
