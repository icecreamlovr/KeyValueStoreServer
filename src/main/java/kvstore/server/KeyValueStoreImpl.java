package kvstore.server;

import io.grpc.stub.StreamObserver;
import kvstore.KeyValueStoreGrpc;
import io.grpc.*;
import kvstore.*;

import java.util.List;

public class KeyValueStoreImpl extends KeyValueStoreGrpc.KeyValueStoreImplBase {
  private final DataStorage dataStorage;
  private final Proposer proposer;
  private long largestProposalNumber;
  private long acceptedProposalNumber;
  private PaxosDatum acceptedProposalDatum;

  // Initialize the data storage for storing key-value pairs
  public KeyValueStoreImpl(int port, List<Integer> allReplicaPorts) {
    this.dataStorage = new DataStorage();
    this.proposer = new Proposer(port, allReplicaPorts);
    // Reset to initial values to indicate no proposal seen in the current Paxos round.
    largestProposalNumber = -1;
    acceptedProposalNumber = -1;
    acceptedProposalDatum = null;
  }

  // Method to handle GET requests
  @Override
  public void get(GetRequest request, StreamObserver<GetResponse> responseObserver) {
    ServerLogger.info("Received GET request: " + request.toString().replace('\n', ' '));

    // Process the message
    String key = request.getKey().toLowerCase();
    GetResponse res;

    // the coordinator will wait on read lock until success
    dataStorage.blockingReadLock(key);

    // Check if the key exists in the data storage
    if (!dataStorage.containsKey(key)) {
      dataStorage.readUnlock(key);
      ServerLogger.info("Send GET error: INVALID_ARGUMENT. Key %s doesn't exist.\n", key);
      responseObserver.onError(Status.INVALID_ARGUMENT.
              withDescription("Key " + key + " doesn't exist.").asRuntimeException());
      return;
    }

    // The coordinator gets the value in the data storage
    String value = dataStorage.get(key);
    dataStorage.readUnlock(key);
    res = GetResponse.newBuilder().setValue(value).build();
    ServerLogger.info("Send GET response: %s\n", res.toString().replace('\n', ' '));
    responseObserver.onNext(res);
    responseObserver.onCompleted();
  }

  // Method to handle PUT requests
  @Override
  public void put(PutRequest request, StreamObserver<PutResponse> responseObserver) {
    ServerLogger.info("Received PUT request: %s", request.toString().replace('\n', ' '));

    // Process the message
    String key = request.getKey().toLowerCase();
    String value = request.getValue().toLowerCase();
    PaxosDatum datum = PaxosDatum.newBuilder().setMethod("PUT").setKey(key).setValue(value).build();
    PutResponse res;

    long proposalNumber = Proposer.generateProposalNumber();
    largestProposalNumber = proposalNumber;

    // Execute Paxos and retry if necessary.
    proposer.retriablePaxosPropose(proposalNumber, datum);

    // The coordinator store the key-value pair in the data storage
    dataStorage.put(key, value);

    largestProposalNumber = -1;

    res = PutResponse.newBuilder().setStatus(true).build();
    ServerLogger.info("Send PUT response: %s\n",res.toString().replace('\n', ' '));
    responseObserver.onNext(res);
    responseObserver.onCompleted();
  }


  // Method to handle DELETE requests
  @Override
  public void delete(DeleteRequest request, StreamObserver<DeleteResponse> responseObserver) {
    ServerLogger.info("Received DELETE request: %s", request.toString().replace('\n', ' '));

    // Process the message
    // Delete the key-value pair from the data storage
    String key = request.getKey().toLowerCase();
    PaxosDatum datum = PaxosDatum.newBuilder().setMethod("DELETE").setKey(key).build();
    DeleteResponse res;

    long proposalNumber = Proposer.generateProposalNumber();
    largestProposalNumber = proposalNumber;

    if (!dataStorage.containsKey(key)) {
      ServerLogger.info("Send DELETE response: error: INVALID_ARGUMENT. Key %s doesn't exist.\n", key);
      responseObserver.onError(Status.INVALID_ARGUMENT.
              withDescription("Key " + key + " doesn't exist.").asRuntimeException());
      return;
    }

    // Execute Paxos and retry if necessary.
    proposer.retriablePaxosPropose(proposalNumber, datum);

    dataStorage.delete(key);
    largestProposalNumber = -1;

    // Send the DELETE response
    res = DeleteResponse.newBuilder().setStatus(true).build();
    ServerLogger.info("Send DELETE response: \n", res.toString().replace('\n', ' '));
    responseObserver.onNext(res);
    responseObserver.onCompleted();
  }


  // Method to handle Paxos Prepare request as an Acceptor
  @Override
  public void prepare(PrepareRequest request, StreamObserver<PrepareResponse> responseObserver) {
    ServerLogger.info("Received Paxos prepare: %s", request.toString().replace('\n', ' '));

    RandomException.randomlyThrowException();

    PrepareResponse.Builder responseBuilder = PrepareResponse.newBuilder();

    long proposalNumber = request.getProposalNumber();
    if (proposalNumber < largestProposalNumber) {
      responseBuilder.setPrepareOk(false);
    } else {
      largestProposalNumber = proposalNumber;

      if (acceptedProposalNumber == -1) {
        responseBuilder.setPrepareOk(true).setPreviousProposalNumber(-1);
      } else {
        responseBuilder
                .setPrepareOk(true)
                .setPreviousProposalNumber(acceptedProposalNumber)
                .setPreviousProposalValue(acceptedProposalDatum);
      }
    }

    PrepareResponse response = responseBuilder.build();
    ServerLogger.info("Sent Paxos prepare response: %s\n", response.toString().replace('\n', ' '));
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  // Method to handle Paxos Accept request as an Acceptor
  @Override
  public void accept(AcceptRequest request, StreamObserver<AcceptResponse> responseObserver) {
    ServerLogger.info("Received Paxos accept: %s", request.toString().replace('\n', ' '));

    RandomException.randomlyThrowException();

    AcceptResponse.Builder responseBuilder = AcceptResponse.newBuilder();

    long proposalNumber = request.getProposalNumber();
    if (proposalNumber < largestProposalNumber) {
      responseBuilder.setAcceptOk(false);
    } else {
      largestProposalNumber = proposalNumber;
      acceptedProposalNumber = proposalNumber;
      acceptedProposalDatum = request.getProposalValue();
      responseBuilder.setAcceptOk(true);
    }

    AcceptResponse response = responseBuilder.build();
    ServerLogger.info("Sent Paxos accept response: %s\n", response.toString().replace('\n', ' '));
    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  // Method to handle Paxos Decide request as a Learner
  @Override
  public void decide(DecideRequest request, StreamObserver<DecideResponse> responseObserver) {
    ServerLogger.info("Received Paxos decide: %s", request.toString().replace('\n', ' '));

    PaxosDatum datum = request.getProposalValue();
    String method = datum.getMethod();
    String key = datum.getKey().toLowerCase();
    if (method.equalsIgnoreCase("PUT")) {
      dataStorage.put(key, datum.getValue());
    } else if (method.equalsIgnoreCase("DELETE")) {
      String value = dataStorage.delete(key);
      // if the key doesn't exist in this data storage, log it
      if (value == null) {
        ServerLogger.error("Data error while processing DELETE: key " + key + " doesn't exist.");
      }
    } else {
      ServerLogger.error("Unknown method " + method + '\n');
    }

    DecideResponse response = DecideResponse.newBuilder().setSuccess(true).build();
    ServerLogger.info("Sent Paxos decide response: %s\n", response.toString().replace('\n', ' '));
    responseObserver.onNext(response);
    responseObserver.onCompleted();

    // Reset to initial values to indicate no proposal seen in the current Paxos round.
    largestProposalNumber = -1;
    acceptedProposalNumber = -1;
    acceptedProposalDatum = null;
  }
}