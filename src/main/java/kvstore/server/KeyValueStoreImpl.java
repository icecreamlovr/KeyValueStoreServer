package kvstore.server;

import io.grpc.stub.StreamObserver;
import kvstore.KeyValueStoreGrpc;

import io.grpc.*;
import kvstore.*;

import java.util.List;


public class KeyValueStoreImpl extends KeyValueStoreGrpc.KeyValueStoreImplBase {
  private final DataStorage dataStorage;
  private final Coordinator coordinator;

  // Initialize the data storage for storing key-value pairs
  public KeyValueStoreImpl(int port, List<Integer> allReplicaPorts) {
    this.dataStorage = new DataStorage();
    this.coordinator = new Coordinator(port, allReplicaPorts);
  }

  // Method to handle GET requests
  @Override
  public void get(GetRequest request, StreamObserver<GetResponse> responseObserver) {
    ServerLogger.info("Received GET request: " + request.toString().replace('\n', ' '));

    // Process the message
    String key = request.getKey().toLowerCase();
    GetResponse res;

    // the coordinator keeps trying to lock resources until success
    dataStorage.blockingReadLock(key);

    // Check if the key exists in the data storage
    if (!dataStorage.containsKey(key)) {
      ServerLogger.error("Send GET error: INVALID_ARGUMENT. Key " + key + " doesn't exist.");
      responseObserver.onError(Status.INVALID_ARGUMENT.
              withDescription("Key " + key + " doesn't exist.").asRuntimeException());
      return;
    }

    // The coordinator gets the value in the data storage
    String value = dataStorage.get(key);
    res = GetResponse.newBuilder().setValue(value).build();
    ServerLogger.info("Send GET response: " + res.toString().replace('\n', ' '));
    responseObserver.onNext(res);
    responseObserver.onCompleted();

    dataStorage.readUnlock(key);
  }


  // Method to handle PUT requests
  @Override
  public void put(PutRequest request, StreamObserver<PutResponse> responseObserver) {
    ServerLogger.info("Received PUT request: " + request.toString().replace('\n', ' '));

    // Process the message
    String key = request.getKey().toLowerCase();
    String value = request.getValue().toLowerCase();
    PutResponse res;

    if (dataStorage.tryWriteLock(key)) {
      // Two-phase commit.
      boolean twoPhaseCommitSuccess = coordinator.twoPhaseCommit("PUT", key, value);
      if (twoPhaseCommitSuccess) {
        // The coordinator store the key-value pair in the data storage
        dataStorage.put(key, value);
        res = PutResponse.newBuilder().setStatus(true).build();
        ServerLogger.info("Send PUT response: " + res.toString().replace('\n', ' '));
        responseObserver.onNext(res);
        responseObserver.onCompleted();
        return;
      }
    }

    dataStorage.writeUnlock(key);
    ServerLogger.error("Abort PUT due to concurrency control.");
    responseObserver.onError(Status.INVALID_ARGUMENT.
            withDescription("Cannot put the key/value pair due to database being locked by other requests.").
            asRuntimeException());
  }


  // Method to handle DELETE requests
  @Override
  public void delete(DeleteRequest request, StreamObserver<DeleteResponse> responseObserver) {
    ServerLogger.info("Received DELETE request: " + request.toString().replace('\n', ' '));

    // Process the message
    // Delete the key-value pair from the data storage
    String key = request.getKey().toLowerCase();
    DeleteResponse res;

    if (dataStorage.tryWriteLock(key)) {
      // Check if the key exists in the data storage
      if (!dataStorage.containsKey(key)) {
        dataStorage.writeUnlock(key);
        ServerLogger.error("Send DELETE error: INVALID_ARGUMENT. Key " + key + " doesn't exist.");
        responseObserver.onError(Status.INVALID_ARGUMENT.
                withDescription("Key " + key + " doesn't exist.").asRuntimeException());
        return;
      }

      // Two-phase commit.
      boolean twoPhaseCommitSuccess = coordinator.twoPhaseCommit("DELETE", key, null);
      if (twoPhaseCommitSuccess) {
        // The coordinator store the key-value pair in the data storage
        dataStorage.delete(key);
        res = DeleteResponse.newBuilder().setStatus(true).build();
        // Send the DELETE response
        ServerLogger.info("Send DELETE response: " + res.toString().replace('\n', ' '));
        responseObserver.onNext(res);
        responseObserver.onCompleted();
        return;
      }
    }

    dataStorage.writeUnlock(key);
    ServerLogger.error("Abort DELETE due to concurrency control.");
    responseObserver.onError(Status.INVALID_ARGUMENT.
            withDescription("Cannot delete the value due to database being locked by other requests.").
            asRuntimeException());
  }


  @Override
  public void prepare(PrepareRequest request, StreamObserver<PrepareResponse> responseObserver) {
    String method = request.getMethod().toUpperCase();
    String key = request.getKey();
    PrepareResponse res;

    // Try lock resources
    if (dataStorage.tryWriteLock(key)) {
      res = PrepareResponse.newBuilder().setIsPrepared(true).build();
      ServerLogger.info("Successfully locked resources for method " + method);
    } else {
      // If failed
      res = PrepareResponse.newBuilder().setIsPrepared(false).build();
      ServerLogger.info("Cannot lock resources for method " + method);
    }

    responseObserver.onNext(res);
    responseObserver.onCompleted();
  }

  @Override
  public void commit(CommitRequest request, StreamObserver<CommitResponse> responseObserver) {
    String method = request.getMethod();
    String key = request.getKey().toLowerCase();
    CommitResponse res;

    if (method.equalsIgnoreCase("PUT")) {
      dataStorage.put(key, request.getValue());
    } else if (method.equalsIgnoreCase("DELETE")) {
      String value = dataStorage.delete(key);
      // if the key doesn't exist in this data storage, log it
      if (value == null) {
        ServerLogger.error("Possible data based error. Key " + key + " doesn't exist.");
      }
    } else {
      String value = dataStorage.get(key);
      // if the key doesn't exist in this data storage, log it
      if (value == null) {
        ServerLogger.error("Possible data based error. Key " + key + " doesn't exist.");
      }
    }
    res = CommitResponse.newBuilder().setIsCommitted(true).build();
    responseObserver.onNext(res);
    responseObserver.onCompleted();
  }

  @Override
  public void abort(AbortRequest request, StreamObserver<AbortResponse> responseObserver) {
    dataStorage.writeUnlock(request.getKey());
    AbortResponse res = AbortResponse.newBuilder().setIsAbort(true).build();
    responseObserver.onNext(res);
    responseObserver.onCompleted();
  }
}