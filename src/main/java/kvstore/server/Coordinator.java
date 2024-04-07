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
  // TODO: switch to non-blocking stubs?
  private List<KeyValueStoreBlockingStub> replicaStubs;
  private static final String SERVER_HOST = "localhost";
  private final int serverPort;

  public Coordinator(int serverPort, List<Integer> allReplicaPorts) {
    this.serverPort = serverPort;
    readReplicaPortsFromCli(allReplicaPorts);
//    readReplicaPortsFromFile();
    this.createReplicaStubs();
  }

  private void readReplicaPortsFromCli(List<Integer> allReplicaPorts) {
    for (int portNumber : allReplicaPorts) {
      if (portNumber == serverPort) {
        continue;
      }
      replicaPorts.add(portNumber);
    }
  }

  private void readReplicaPortsFromFile() {
    // TODO: add server port when start, delete server port when close
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

  private void createReplicaStubs() {
    replicaStubs = new ArrayList<>();
    for (int neighbor : replicaPorts) {
      String target = SERVER_HOST + ":" + neighbor;
      ManagedChannel channel = Grpc.newChannelBuilder(target, InsecureChannelCredentials.create()).build();
      replicaStubs.add(KeyValueStoreGrpc.newBlockingStub(channel));
    }
  }

  public boolean twoPhaseCommit(String method, String key, String value) {
    // Two-phase commit.
    // Step1: send prepare
    boolean areReplicasPrepared = sendPrepare(method, key, value);
    if (!areReplicasPrepared) {
      ServerLogger.error("Cannot access the database due to concurrency control. Try again.");
      sendAbort(method, key, value);
      return false;
    }
    // Step2: commit
    sendCommit(method, key, value);
    return true;
  }


  public boolean sendPrepare(String method, String key, String value) {
    PrepareRequest request = PrepareRequest.newBuilder().setMethod(method).setKey(key).setValue(value).build();
    ServerLogger.info("Send prepare request to replicas " + replicaPorts + ": " + request);
    boolean allStubsPrepared = true;
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
      ServerLogger.info("Received prepare response from replicas " + replicaPorts.get(i) + ": " + response);
      if (!response.getIsPrepared()) {
        allStubsPrepared = false;
      }
    }
    return allStubsPrepared;
  }

  public void sendCommit(String method, String key, String value) {
    CommitRequest request = CommitRequest.newBuilder().setMethod(method).setKey(key).setValue(value).build();
    ServerLogger.info("Send commit request to replicas " + replicaPorts + ": " + request);
    for (int i = 0; i < replicaStubs.size(); i++) {
      KeyValueStoreBlockingStub replicaStub = replicaStubs.get(i);
      CommitResponse response;
      try {
        response = replicaStub.withDeadlineAfter(TIMEOUT, TimeUnit.MILLISECONDS).commit(request);
      } catch (StatusRuntimeException e) {
        ServerLogger.error("Error from replicas: " + e.getMessage());
        continue;
      }
      ServerLogger.info("Received commit response from replicas " + replicaPorts.get(i) + ": " + response);
    }
  }

  public void sendAbort(String method, String key, String value) {
    AbortRequest request = AbortRequest.newBuilder().setMethod(method).setKey(key).setValue(value).build();
    ServerLogger.info("Send abort request to replicas " + replicaPorts + ": " + request);
    for (int i = 0; i < replicaStubs.size(); i++) {
      KeyValueStoreBlockingStub replicaStub = replicaStubs.get(i);
      AbortResponse response;
      try {
        response = replicaStub.withDeadlineAfter(TIMEOUT, TimeUnit.MILLISECONDS).abort(request);
      } catch (StatusRuntimeException e) {
        ServerLogger.error("Error from replicas: " + e.getMessage());
        continue;
      }
      ServerLogger.info("Received abort response from replicas " + replicaPorts.get(i) + ": " + response);
    }
  }
}
