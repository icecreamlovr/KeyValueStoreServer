# Key-Value Store Server and Client
This project implements a simple key-value store server and client application using both TCP and UDP protocols. The server allows clients to perform operations like putting, getting, and deleting key-value pairs, while the client provides a command-line interface for interacting with the server.

## Features
* **TCP and UDP Support:** The server supports both TCP and UDP protocols for communication with clients.
* **Key-Value Store:** The server maintains a key-value store in memory, allowing clients to store and retrieve data.
* **Checksum Validation:** Messages exchanged between the server and clients include checksums for data integrity verification.
* **Pre-populated Requests:** The client can pre-populate the server with a set of predefined requests for testing purposes.
* **Command-Line Interface:** The client provides a command-line interface for users to interact with the server by sending requests.
* **Logging:** Extensive logging of both requests, response and error conditions at both the client and the server.
## Components
### Server
The server application consists of the following components:

* **TCPHandler:** Handles incoming TCP connections from clients.
* **UDPHandler:** Handles incoming UDP datagrams from clients.
* **AbstractHandler:** Abstract class providing common functionality for handling requests.
* **KeyValue:** Class representing the key-value store maintained by the server.
* **Response:** Class representing response messages sent by the server.
* **ServerLogger:** Utility class for logging server events.
### Client
The client application consists of the following components:

* **ClientApp:** Main class responsible for starting the client application.
* **AbstractClient:** Abstract class providing common functionality for interacting with the server.
* **TCPClient:** Implementation of the client using TCP protocol.
* **UDPClient:** Implementation of the client using UDP protocol.
* **CliFlags:** Class representing command-line arguments parsed by the client.
* **ClientLogger:** Utility class for logging client events.
### Util
* **Checksum:** Utility class for generating and verifying checksums in messages.

## Project structure
```
src
├── Readme.md
└── util
│   ├── Checksum.java
├── client
│   ├── AbstractClient.java
│   ├── Client.java
│   ├── ClientApp.java
│   ├── ClientLogger.java
│   ├── TCPClient.java
│   └── UDPClient.java
└── server
    ├── AbstractHandler.java
    ├── KeyValue.java
    ├── Response.java
    ├── ServerApp.java
    ├── ServerLogger.java
    ├── TCPHandler.java
    └── UDPHandler.java

3 directories, 15 files
```
## Usage
### Compile
* Compile the code using `javac server/*.java client/*.java`
```
> javac server/*.java client/*.java
```
### Server
To start the server, run the `ServerApp` class with two command-line arguments specifying the TCP and UDP port numbers: `java server.ServerApp <tcp-port-number> <udp-port-number>`. This will spawn both TCP and UDP server in two separate threads.
```
> java server.ServerApp 32000 32001
```
### Client
To start the client, run the ClientApp class with three command-line arguments specifying the server IP address, TCP port number, and protocol (TCP or UDP):
`java client.ClientApp <host-name> <port-number> <protocol>`
```aidl
> java client.ClientApp 127.0.0.1 32000 tcp
> java client.ClientApp 127.0.0.1 32001 udp
```
Once the client is running, you will see **5 automated sample requests of GET, PUT and DELETE** each. 
Then you can interact with the server using the following commands:

* **PUT:** Store a key-value pair in the server.
* **GET:** Retrieve the value associated with a key from the server.
* **DELETE:** Remove a key-value pair from the server.

#### Accepted User Input Format
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

## Communication Protocol

### Request to server
* Client forwards the message and appends a checksum when sending request to server. The checksum is surrounded by semi-colons for server to parse.
  * request = message + ";" + checksum + ";".
* Example client request:
  ```
  put apple fruit;1493;
  get apple;882;
  ```

### Server response
* In server response:
  * the first character is always the status code. Status code can be 0 (success) or 1 (failure).
    * status code 0 means the client request has been handled successfully.
    * status code 1 means the client request has encountered an error.
  * The detailed message comes after the status code.
  * There is also a checksum at the end.
* Server response = status code + " " + message.
* Example server response:
  ```
  0 Value of "apple" is: "fruit";2428;
  ```
  * In this example:
    * '0' is the status code
    * 'Value of "apple" is: "fruit"' is the message
    * 2428 is the checksum, surrounded by semicolons

## Logging
* Client
  * Client logs the response received from the server, including the checksum.
  * Client also logs when the user input is malformed. For example, if the user mis-spell PUT as PT.
  * Client also logs when it timeouts receiving server response.
* Server
  * Server logs both the requests it receives from the client, and the response it sends to the client.
  * Server also logs error events. For example, if the key-value store receives attempts to delete a non-existing key. Such operations are disallowed at the handler layer. However if for any reason it's slipped to the data layer (e.g. due to race condition), this will be logged.

### Feel free to use any of these files as is or modify according to your needs! 
