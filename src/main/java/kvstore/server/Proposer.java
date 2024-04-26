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

public class Proposer {
  private static final int TIMEOUT = 5000;
  private static final int PAXOS_RESTART_DELAY_IN_MS = 1000;
  private final List<Integer> replicaPorts = new ArrayList<>();
  private List<KeyValueStoreBlockingStub> replicaStubs;
  private static final String SERVER_HOST = "localhost";
  private final int serverPort;

  // Constructor for Proposer class
  public Proposer(int serverPort, List<Integer> allReplicaPorts) {
    this.serverPort = serverPort;
    readReplicaPortsFromCli(allReplicaPorts);
  // readReplicaPortsFromFile();
    this.createReplicaStubs();
  }

  // To be invoked by Constructor. Method to read replica ports from command-line arguments
  private void readReplicaPortsFromCli(List<Integer> allReplicaPorts) {
    for (int portNumber : allReplicaPorts) {
      if (portNumber == serverPort) {
        continue;
      }
      replicaPorts.add(portNumber);
    }
  }

  // To be invoked by Constructor. Method to read replica ports from command-line arguments
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

  // To be invoked by Constructor. Method to create blocking stubs for communication with replicas
  private void createReplicaStubs() {
    replicaStubs = new ArrayList<>();
    for (int neighbor : replicaPorts) {
      String target = SERVER_HOST + ":" + neighbor;
      ManagedChannel channel = Grpc.newChannelBuilder(target, InsecureChannelCredentials.create()).build();
      replicaStubs.add(KeyValueStoreGrpc.newBlockingStub(channel));
    }
  }

  // Generate global unique and incremental proposal ID.
  // In this project, we simplify this to timestamp in milliseconds. In real world,
  // there should be a service to provide such ID in distributed environment.
  public static long generateProposalNumber() {
    return System.currentTimeMillis();
  }

  // Check if the number responded OK is majority
  private boolean isMajority(int numOk) {
    return numOk > replicaStubs.size() / 2;
  }

  // Method for executing Paxos three-phase proposal protocol, and retry if no majority is reached.
  public void retriablePaxosPropose(long proposalNumber, PaxosDatum datum) {
    while (true) {
      boolean paxosSuccess = paxosPropose(proposalNumber, datum);
      if (paxosSuccess) {
        break;
      }
      // Wait for small amount of time (one second) then restart Paxos.
      ServerLogger.info("Unable to reach majority. Wait for 1 second then restart Paxos.");
      try {
        Thread.sleep(PAXOS_RESTART_DELAY_IN_MS);
      } catch (InterruptedException e) {
        // Handle interrupted exception
        ServerLogger.error("Received interruption.");
        e.printStackTrace();
        System.exit(2);
      }
    }
  }

  // Method for executing Paxos three-phase proposal protocol to reach consensus on DB state.
  private boolean paxosPropose(long proposalNumber, PaxosDatum datum) {
    ServerLogger.info("\n");
    ServerLogger.info("Initiating Paxos protocol...");

    // Phase 1: send prepare
    ServerLogger.info("Send Prepare with proposal number %d", proposalNumber);
    PrepareResponse prepareResponse = sendPrepare(proposalNumber, datum);
    if (!prepareResponse.getPrepareOk()) {
      ServerLogger.info("Paxos failed to reach consensus during prepare. Will retry later.");
      return false;
    }

    // Phase 2: send accept
    ServerLogger.info(
            "Send Accept with proposal number %d and value %s",
            prepareResponse.getPreviousProposalNumber(),
            prepareResponse.getPreviousProposalValue().toString().replace('\n', ' '));
    boolean acceptOk =
            sendAccept(
                    prepareResponse.getPreviousProposalNumber(),
                    prepareResponse.getPreviousProposalValue());
    if (!acceptOk) {
      // TODO: restart Paxos protocol after short amount of delay
      ServerLogger.info("Paxos failed to reach consensus during accept. Will retry later.");
      return false;
    }

    // Phase 3: send decide
    ServerLogger.info(
            "Send Decide with value %s",
            prepareResponse.getPreviousProposalValue().toString().replace('\n', ' '));
    sendDecide(prepareResponse.getPreviousProposalValue());
    ServerLogger.info(
            "Paxos successful. Committed: %s",
            prepareResponse.getPreviousProposalValue().toString().replace('\n', ' '));
    return true;
  }


  // Method to send prepare request to replicas
  private PrepareResponse sendPrepare(long proposalNumber, PaxosDatum datum) {
    PrepareRequest request =
            PrepareRequest.newBuilder().setProposalNumber(proposalNumber).build();
    int numPrepareOk = 0;
    PaxosDatum previousDatum = null;
    long previousProposal = -1;
    // Treat all other replicas as acceptors.
    for (int i = 0; i < replicaStubs.size(); i++) {
      KeyValueStoreBlockingStub replicaStub = replicaStubs.get(i);
      PrepareResponse response;
      try {
          response = replicaStub.withDeadlineAfter(TIMEOUT, TimeUnit.MILLISECONDS).prepare(request);
      } catch (StatusRuntimeException e) {
        ServerLogger.error("Error from replicas %d: %s", i, e.getMessage());
        continue;
      }
      ServerLogger.info("Received prepare response from replicas %d: prepare ok %b, proposal number %d, proposal value %s", i, response.getPrepareOk(), response.getPreviousProposalNumber(), response.getPreviousProposalValue().toString().replace('\n', ' '));
      if (response.getPrepareOk()) {
        numPrepareOk++;
        if (response.getPreviousProposalNumber() > previousProposal) {
          previousProposal = response.getPreviousProposalNumber();
          previousDatum = response.getPreviousProposalValue();
        }
      }
    }

    ServerLogger.info("Total replicas: %d. Prepare OK: %d", replicaStubs.size(), numPrepareOk);
    if (!isMajority(numPrepareOk)) {
      return PrepareResponse.newBuilder().setPrepareOk(false).build();
    }

    if (previousDatum == null) {
      // This means no value has been accepted before. The acceptors can accept new value
      return PrepareResponse
              .newBuilder()
              .setPrepareOk(true)
              .setPreviousProposalNumber(proposalNumber)
              .setPreviousProposalValue(datum)
              .build();
    }

    // The acceptors already promised to accept an existing datum
    return PrepareResponse
            .newBuilder()
            .setPrepareOk(true)
            .setPreviousProposalNumber(proposalNumber)
            .setPreviousProposalValue(previousDatum)
            .build();
  }

  // Method to send commit request to replicas
  private boolean sendAccept(long proposalNumber, PaxosDatum datum) {
    AcceptRequest request =
            AcceptRequest.newBuilder()
                    .setProposalNumber(proposalNumber)
                    .setProposalValue(datum)
                    .build();
    int numAcceptOk = 0;
    // Treat all other replicas as acceptors.
    for (int i = 0; i < replicaStubs.size(); i++) {
      KeyValueStoreBlockingStub replicaStub = replicaStubs.get(i);
      AcceptResponse response;
      try {
        response = replicaStub.withDeadlineAfter(TIMEOUT, TimeUnit.MILLISECONDS).accept(request);
      } catch (StatusRuntimeException e) {
        ServerLogger.error("Error from replicas %d: %s", i, e.getMessage());
        continue;
      }
      ServerLogger.info("Received accept response from replicas %d: accept ok %b", i, response.getAcceptOk());
      if (response.getAcceptOk()) {
        numAcceptOk++;
      }
    }
    ServerLogger.info("Total replicas: %d. Accept OK: %d", replicaStubs.size(), numAcceptOk);
    return isMajority(numAcceptOk);
  }

  // Method to send abort request to replicas
  private void sendDecide(PaxosDatum datum) {
    DecideRequest request = DecideRequest.newBuilder().setProposalValue(datum).build();
    // Treat all other replicas as learners.
    for (int i = 0; i < replicaStubs.size(); i++) {
      KeyValueStoreBlockingStub replicaStub = replicaStubs.get(i);
      DecideResponse response;
      try {
        response = replicaStub.withDeadlineAfter(TIMEOUT, TimeUnit.MILLISECONDS).decide(request);
      } catch (StatusRuntimeException e) {
        ServerLogger.error("Error from replicas %d: %s", i, e.getMessage());
        continue;
      }
      ServerLogger.info("Received decide response from replicas %d: %s", i, response.toString().replace('\n', ' '));
    }
  }
}
