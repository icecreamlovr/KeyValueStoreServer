package kvstore.server;

import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import kvstore.*;
import kvstore.KeyValueStoreGrpc.KeyValueStoreBlockingStub;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Coordinator {
  private static final int TIMEOUT = 5000;
  private final List<Integer> replicaPorts = new ArrayList<>();
  private List<KeyValueStoreBlockingStub> replicaStubs;
  private static final String SERVER_HOST = "localhost";
  private final int serverPort;

  // Constructor for Coordinator class
  public Coordinator(int serverPort, List<Integer> allReplicaPorts) {
    this.serverPort = serverPort;
    readReplicaPortsFromCli(allReplicaPorts);
  // readReplicaPortsFromFile();
    this.createReplicaStubs();
  }

  // Method to read replica ports from command-line arguments
  private void readReplicaPortsFromCli(List<Integer> allReplicaPorts) {
    for (int portNumber : allReplicaPorts) {
      if (portNumber == serverPort) {
        continue;
      }
      replicaPorts.add(portNumber);
    }
  }

  // Method to read replica ports from command-line arguments
  private void readReplicaPortsFromFile() {
    String filename = "serverPorts";
    try {
      FileReader fileReader = new FileReader(filename);
      BufferedReader bufferedReader = new BufferedReader(fileReader);
      String line = bufferedReader.readLine();
      if (line != null) {
        String[] ports = line.split(" ");
        for (String port : ports) {
          int portNumber = Integer.parseInt(port);
          if (portNumber == serverPort) {
            continue;
          }
          replicaPorts.add(portNumber);
        }
      }
      bufferedReader.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  // Method to create blocking stubs for communication with replicas
  private void createReplicaStubs() {
    replicaStubs = new ArrayList<>();
    for (int neighbor : replicaPorts) {
      String target = SERVER_HOST + ":" + neighbor;
      ManagedChannel channel = Grpc.newChannelBuilder(target, InsecureChannelCredentials.create()).build();
      replicaStubs.add(KeyValueStoreGrpc.newBlockingStub(channel));
    }
  }

  // Method for two-phase commit protocol
  public boolean twoPhaseCommit(String method, String key, String value) {
    // Two-phase commit.
    // Phase 1: send prepare
    ServerLogger.info("Initiating two-phase commit protocol...");
    boolean areAllReplicasPrepared = sendPrepare(method, key, value);
    if (!areAllReplicasPrepared) {
      ServerLogger.error("Two-phase commit failed: not all replicas are ready to commit.");
      // Phase 2: send abort
      sendAbort(method, key, value);
      return false;
    }
    // Phase 2: send commit
    sendCommit(method, key, value);
    ServerLogger.info("Two-phase commit successful.");
    return true;
  }


  // Method to send prepare request to replicas
  public boolean sendPrepare(String method, String key, String value) {
    PrepareRequest request = PrepareRequest.newBuilder().setMethod(method).setKey(key).setValue(value).build();
    ServerLogger.info("Send prepare request to replicas " + replicaPorts + ": " + request.toString().replace('\n', ' '));
    boolean allStubsPrepared = true;

    // Iterate over replica stubs
    for (int i = 0; i < replicaStubs.size(); i++) {
      KeyValueStoreBlockingStub replicaStub = replicaStubs.get(i);
      PrepareResponse response;
      try {
          response = replicaStub.withDeadlineAfter(TIMEOUT, TimeUnit.MILLISECONDS).prepare(request);
      } catch (StatusRuntimeException e) {
        ServerLogger.error("Error from replicas: " + e.getMessage());
        allStubsPrepared = false;
        continue;
      }
      ServerLogger.info("Received prepare response from replicas " + replicaPorts.get(i) + ": " + response.toString().replace('\n', ' '));
      if (!response.getIsPrepared()) {
        allStubsPrepared = false;
      }
    }
    return allStubsPrepared;
  }

  // Method to send commit request to replicas
  public void sendCommit(String method, String key, String value) {
    CommitRequest request = CommitRequest.newBuilder().setMethod(method).setKey(key).setValue(value).build();
    ServerLogger.info("Send commit request to replicas " + replicaPorts + ": " + request.toString().replace('\n', ' '));
    // Iterate over replica stubs
    for (int i = 0; i < replicaStubs.size(); i++) {
      KeyValueStoreBlockingStub replicaStub = replicaStubs.get(i);
      CommitResponse response;
      try {
        response = replicaStub.withDeadlineAfter(TIMEOUT, TimeUnit.MILLISECONDS).commit(request);
      } catch (StatusRuntimeException e) {
        ServerLogger.error("Error from replicas: " + e.getMessage());
        continue;
      }
      ServerLogger.info("Received commit response from replicas " + replicaPorts.get(i) + ": " + response.toString().replace('\n', ' '));
    }
  }

  // Method to send abort request to replicas
  public void sendAbort(String method, String key, String value) {
    AbortRequest request = AbortRequest.newBuilder().setMethod(method).setKey(key).setValue(value).build();
    ServerLogger.info("Send abort request to replicas " + replicaPorts + ": " + request.toString().replace('\n', ' '));

    // Send abort request with timeout
    for (int i = 0; i < replicaStubs.size(); i++) {
      KeyValueStoreBlockingStub replicaStub = replicaStubs.get(i);
      AbortResponse response;
      try {
        // Send abort request with timeout
        response = replicaStub.withDeadlineAfter(TIMEOUT, TimeUnit.MILLISECONDS).abort(request);
      } catch (StatusRuntimeException e) {
        ServerLogger.error("Error from replicas: " + e.getMessage());
        continue;
      }
      ServerLogger.info("Received abort response from replicas " + replicaPorts.get(i) + ": " + response.toString().replace('\n', ' '));
    }
  }
}
