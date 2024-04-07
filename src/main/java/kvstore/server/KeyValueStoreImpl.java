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
    if (port == 3334) {
      dataStorage.tryWriteLock("aaa");
    }
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
      ServerLogger.error("Send GET error: INVALID_ARGUMENT. Key " + key + " doesn't exist.\n");
      responseObserver.onError(Status.INVALID_ARGUMENT.
              withDescription("Key " + key + " doesn't exist.").asRuntimeException());
      return;
    }

    // The coordinator gets the value in the data storage
    String value = dataStorage.get(key);
    dataStorage.readUnlock(key);
    res = GetResponse.newBuilder().setValue(value).build();
    ServerLogger.info("Send GET response: " + res.toString().replace('\n', ' ') + '\n');
    responseObserver.onNext(res);
    responseObserver.onCompleted();
  }


  // Method to handle PUT requests
  @Override
  public void put(PutRequest request, StreamObserver<PutResponse> responseObserver) {
    ServerLogger.info("Received PUT request: " + request.toString().replace('\n', ' '));

    // Process the message
    String key = request.getKey().toLowerCase();
    String value = request.getValue().toLowerCase();
    PutResponse res;

    // This server is both a Coordinator and one of the Replicas.
    // To proceed with two-phase commit, it also needs to acquire its own local write lock.
    boolean locked = dataStorage.tryWriteLock(key);
    if (!locked) {
      ServerLogger.error("Abort PUT due to unable to acquire local lock.\n");
      responseObserver.onError(Status.UNAVAILABLE.
              withDescription("Cannot put the key/value pair due to database being locked by other requests.").
              asRuntimeException());
      return;
    }

    // Two-phase commit.
    boolean twoPhaseCommitSuccess = coordinator.twoPhaseCommit("PUT", key, value);
    if (twoPhaseCommitSuccess) {
      // The coordinator store the key-value pair in the data storage
      dataStorage.put(key, value);
      dataStorage.writeUnlock(key);
      res = PutResponse.newBuilder().setStatus(true).build();
      ServerLogger.info("Send PUT response: " + res.toString().replace('\n', ' ') + "\n");
      responseObserver.onNext(res);
      responseObserver.onCompleted();

    } else {
      dataStorage.writeUnlock(key);
      ServerLogger.error("Abort PUT due to failure in two-phase commit.\n");
      responseObserver.onError(Status.UNAVAILABLE.
              withDescription("Cannot put the key/value pair due to database being locked by other requests.").
              asRuntimeException());
    }
  }


  // Method to handle DELETE requests
  @Override
  public void delete(DeleteRequest request, StreamObserver<DeleteResponse> responseObserver) {
    ServerLogger.info("Received DELETE request: " + request.toString().replace('\n', ' '));

    // Process the message
    // Delete the key-value pair from the data storage
    String key = request.getKey().toLowerCase();
    DeleteResponse res;

    // This server is both a Coordinator and one of the Replicas.
    // To proceed with two-phase commit, it also needs to acquire its own local write lock.
    boolean locked = dataStorage.tryWriteLock(key);
    if (!locked) {
      ServerLogger.error("Abort DELETE due to unable to acquire local lock.\n");
      responseObserver.onError(Status.UNAVAILABLE.
              withDescription("Cannot delete the value due to database being locked by other requests.").
              asRuntimeException());
      return;
    }

    // Check if the key exists in the data storage
    if (!dataStorage.containsKey(key)) {
      dataStorage.writeUnlock(key);
      ServerLogger.error("Send DELETE response: error: INVALID_ARGUMENT. Key " + key + " doesn't exist.\n");
      responseObserver.onError(Status.INVALID_ARGUMENT.
              withDescription("Key " + key + " doesn't exist.").asRuntimeException());
      return;
    }

    // Two-phase commit.
    boolean twoPhaseCommitSuccess = coordinator.twoPhaseCommit("DELETE", key, "");
    if (twoPhaseCommitSuccess) {
      // The coordinator store the key-value pair in the data storage
      dataStorage.delete(key);
      dataStorage.writeUnlock(key);
      res = DeleteResponse.newBuilder().setStatus(true).build();
      // Send the DELETE response
      ServerLogger.info("Send DELETE response: " + res.toString().replace('\n', ' ') + '\n');
      responseObserver.onNext(res);
      responseObserver.onCompleted();
      return;
    } else {
      dataStorage.writeUnlock(key);
      ServerLogger.error("Abort DELETE due to failure in two-phase commit.\n");
      responseObserver.onError(Status.INVALID_ARGUMENT.
              withDescription("Cannot delete the value due to database being locked by other requests.").
              asRuntimeException());
    }
  }


  // Method to handle PREPARE requests
  @Override
  public void prepare(PrepareRequest request, StreamObserver<PrepareResponse> responseObserver) {
    ServerLogger.info("Received 2pc prepare: " + request.toString().replace('\n', ' '));

    String method = request.getMethod().toUpperCase();
    String key = request.getKey();
    PrepareResponse res;

    // Acquire necessary lock. If successful, reply "commit" to coordinator. If unsuccessful, reply "abort".
    if (dataStorage.tryWriteLock(key)) {
      res = PrepareResponse.newBuilder().setIsPrepared(true).build();
      ServerLogger.info(String.format("Successfully locked resources for method %s. Reply 'COMMIT' to coordinator.\n", method));
    } else {
      res = PrepareResponse.newBuilder().setIsPrepared(false).build();
      ServerLogger.info(String.format("Unable to lock resources for method %s. Reply 'ABORT' to coordinator.\n", method));
    }

    responseObserver.onNext(res);
    responseObserver.onCompleted();
  }

  // Method to handle COMMIT requests
  @Override
  public void commit(CommitRequest request, StreamObserver<CommitResponse> responseObserver) {
    ServerLogger.info("Received 2pc commit: " + request.toString().replace('\n', ' '));

    String method = request.getMethod();
    String key = request.getKey().toLowerCase();
    CommitResponse res;

    if (method.equalsIgnoreCase("PUT")) {
      dataStorage.put(key, request.getValue());
    } else if (method.equalsIgnoreCase("DELETE")) {
      String value = dataStorage.delete(key);
      // if the key doesn't exist in this data storage, log it
      if (value == null) {
        ServerLogger.error("Data error while processing DELETE: key " + key + " doesn't exist.\n");
      }
    } else {
      ServerLogger.error("Unknown method " + method + '\n');
    }
    dataStorage.writeUnlock(key);
    ServerLogger.info("Commit successful.\n");
    res = CommitResponse.newBuilder().setIsCommitted(true).build();
    responseObserver.onNext(res);
    responseObserver.onCompleted();
  }

  // Method to handle ABORT requests
  @Override
  public void abort(AbortRequest request, StreamObserver<AbortResponse> responseObserver) {
    ServerLogger.info("Received 2pc abort: " + request.toString().replace('\n', ' '));
    dataStorage.writeUnlock(request.getKey());
    ServerLogger.info("Abort successful.\n");
    AbortResponse res = AbortResponse.newBuilder().setIsAbort(true).build();
    responseObserver.onNext(res);
    responseObserver.onCompleted();
  }
}