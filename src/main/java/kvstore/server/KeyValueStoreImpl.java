package kvstore.server;

import io.grpc.stub.StreamObserver;
import kvstore.KeyValueStoreGrpc;

import io.grpc.*;
import kvstore.*;

public class KeyValueStoreImpl extends KeyValueStoreGrpc.KeyValueStoreImplBase {
  private final DataStorage dataStorage;

  // Initialize the data storage for storing key-value pairs
  public KeyValueStoreImpl() {
    this.dataStorage = new DataStorage();
  }

  // Method to handle PUT requests
  @Override
  public void put(PutRequest request, StreamObserver<PutResponse> responseObserver) {
    ServerLogger.info("Received PUT request: " + request.toString().replace('\n', ' '));
    // Store the key-value pair in the data storage
    dataStorage.put(request.getKey().toLowerCase(), request.getValue().toLowerCase());
    // Prepare and send the PUT response
    PutResponse res = PutResponse.newBuilder().setStatus(true).build();
    ServerLogger.info("Send PUT response: " + res.toString().replace('\n', ' '));
    responseObserver.onNext(res);
    responseObserver.onCompleted();
  }

  // Method to handle GET requests
  @Override
  public void get(GetRequest request, StreamObserver<GetResponse> responseObserver) {
    ServerLogger.info("Received GET request: " + request.toString().replace('\n', ' '));
    String key = request.getKey().toLowerCase();
    String value = dataStorage.get(key);
    // Check if the key exists in the data storage
    if (value == null) {
      ServerLogger.error("Send GET error: INVALID_ARGUMENT. Key " + key + " doesn't exist.");
      responseObserver.onError(Status.INVALID_ARGUMENT.
              withDescription("Key " + key + " doesn't exist.").asRuntimeException());
      return;
    }
    // Prepare and send the GET response with the retrieved value
    GetResponse res = GetResponse.newBuilder().setValue(value).build();
    ServerLogger.info("Send GET response: " + res.toString().replace('\n', ' '));
    responseObserver.onNext(res);
    responseObserver.onCompleted();
  }

  // Method to handle DELETE requests
  @Override
  public void delete(DeleteRequest request, StreamObserver<DeleteResponse> responseObserver) {
    ServerLogger.info("Received DELETE request: " + request.toString().replace('\n', ' '));
    // Delete the key-value pair from the data storage
    String key = request.getKey().toLowerCase();
    String value = dataStorage.delete(key);
    if (value == null) {
      ServerLogger.error("Send DELETE error: INVALID_ARGUMENT. Key " + key + " doesn't exist.");
      responseObserver.onError(Status.INVALID_ARGUMENT.
              withDescription("Key " + key + " doesn't exist.").asRuntimeException());
      return;
    }
    // Prepare and send the DELETE response
    DeleteResponse res = DeleteResponse.newBuilder().setStatus(true).build();
    ServerLogger.info("Send DELETE response: " + res.toString().replace('\n', ' '));
    responseObserver.onNext(res);
    responseObserver.onCompleted();
  }
}