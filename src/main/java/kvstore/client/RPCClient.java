package kvstore.client;

import io.grpc.*;
import kvstore.*;
import kvstore.KeyValueStoreGrpc.KeyValueStoreBlockingStub;

import java.util.concurrent.TimeUnit;

public class RPCClient{
  // Define a blocking stub for making synchronous RPC calls to the KeyValueStore service.
  private final KeyValueStoreBlockingStub blockingStub;
  private static final int TIMEOUT = 5000;

  // Constructor for initializing the client with a custom server IP and port.
  public RPCClient(String serverIp, int serverPort) {
    String target = serverIp + ":" + serverPort;
    ManagedChannel channel = Grpc.newChannelBuilder(target, InsecureChannelCredentials.create()).build();
    this.blockingStub = KeyValueStoreGrpc.newBlockingStub(channel);
  }

  // Method for making a 'put' RPC call to the server.
  public void put(String key, String value) {
    PutRequest request = PutRequest.newBuilder().setKey(key).setValue(value).build();
    PutResponse response;
    try {
      response = blockingStub.withDeadlineAfter(TIMEOUT, TimeUnit.MILLISECONDS).put(request);
    } catch (StatusRuntimeException e) {
      ClientLogger.error("Error from server: " + e.getMessage());
      return;
    }
    ClientLogger.info("Response from server: " + response.getStatus());
  }

  // Method for making a 'get' RPC call to the server.
  public void get(String key) {
    GetRequest request = GetRequest.newBuilder().setKey(key).build();
    GetResponse response;
    try {
      response = blockingStub.withDeadlineAfter(TIMEOUT, TimeUnit.MILLISECONDS).get(request);
    } catch (StatusRuntimeException e) {
      ClientLogger.error("Error from server: " + e.getMessage());
      return;
    }
    ClientLogger.info("Response from server: " + response.getValue());
  }

  // Method for making a 'delete' RPC call to the server.
  public void delete(String key) {
    DeleteRequest request = DeleteRequest.newBuilder().setKey(key).build();
    DeleteResponse response;
    try {
      response = blockingStub.withDeadlineAfter(TIMEOUT, TimeUnit.MILLISECONDS).delete(request);
    } catch (StatusRuntimeException e) {
      ClientLogger.error("Error from server: " + e.getMessage());
      return;
    }
    ClientLogger.info("Response from server: " + response.getStatus());
  }
}
