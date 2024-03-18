package kvstore.client;

import io.grpc.*;
import kvstore.*;

public class RPCClient{
  private final KeyValueStoreGrpc.KeyValueStoreBlockingStub blockingStub;

  public RPCClient() {
    String target = "localhost:32000";
    ManagedChannel channel = Grpc.newChannelBuilder(target, InsecureChannelCredentials.create()).build();
    this.blockingStub = KeyValueStoreGrpc.newBlockingStub(channel);
  }

  // Construct client for accessing KeyValueStore server using the channel.
  public RPCClient(String serverIp, int serverPort) {
    String target = serverIp + ":" + serverPort;
    ManagedChannel channel = Grpc.newChannelBuilder(target, InsecureChannelCredentials.create()).build();
    this.blockingStub = KeyValueStoreGrpc.newBlockingStub(channel);
  }

  public void put(String key, String value) {
    PutRequest request = PutRequest.newBuilder().setKey(key).setValue(value).build();
    PutResponse response;
    try {
      response = blockingStub.put(request);
    } catch (StatusRuntimeException e) {
      ClientLogger.error("Error from server: " + e.getMessage());
      return;
    }
    ClientLogger.info("Response from server: " + response.getStatus());
  }

  public void get(String key) {
    GetRequest request = GetRequest.newBuilder().setKey(key).build();
    GetResponse response;
    try {
      response = blockingStub.get(request);
    } catch (StatusRuntimeException e) {
      ClientLogger.error("Error from server: " + e.getMessage());
      return;
    }
    ClientLogger.info("Response from server: " + response.getValue());
  }

  public void delete(String key) {
    DeleteRequest request = DeleteRequest.newBuilder().setKey(key).build();
    DeleteResponse response;
    try {
      response = blockingStub.delete(request);
    } catch (StatusRuntimeException e) {
      ClientLogger.error("Error from server: " + e.getMessage());
      return;
    }
    ClientLogger.info("Response from server: " + response.getStatus());
  }
}
