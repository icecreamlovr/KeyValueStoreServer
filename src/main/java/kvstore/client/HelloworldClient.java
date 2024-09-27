package kvstore.client;

import kvstore.CatServiceGrpc;
import kvstore.CreateRequest;
import kvstore.CreateResponse;
import kvstore.PetRequest;
import kvstore.PetResponse;

import io.grpc.Channel;
import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import java.util.concurrent.TimeUnit;

public class HelloworldClient {
  private final CatServiceGrpc.CatServiceBlockingStub blockingStub;

  /** Construct client for accessing HelloWorld server using the existing channel. */
  public HelloworldClient(Channel channel) {
    this.blockingStub = CatServiceGrpc.newBlockingStub(channel);
  }

  public static void main(String[] args) throws InterruptedException {
    String target = "localhost:13579";
    ManagedChannel channel =
            Grpc.newChannelBuilder(target, InsecureChannelCredentials.create()).build();
    try {
      HelloworldClient client = new HelloworldClient(channel);
      client.addCat("咪咪", 3);
      client.addCat("大花", 2);
      client.addCat("胖橘", 1);
      client.addCat("黑黑", 3);
      client.addCat("奶油", 4);
      client.addCat("小花", 5);
      client.petCat(0);
      client.petCat(2);
    } finally {
      // ManagedChannels use resources like threads and TCP connections. To prevent leaking these
      // resources the channel should be shut down when it will no longer be used. If it may be used
      // again leave it running.
      channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
    }
  }

  /** Create a cat. */
  public void addCat(String name, int age) {
    System.out.println("Will try to create a cat " + name + " ...");
    CreateRequest request = CreateRequest.newBuilder().setName(name).setAge(age).build();
    CreateResponse response;
    try {
      response = blockingStub.create(request);
    } catch (StatusRuntimeException e) {
      System.err.println("Error from server: " + e.getMessage());
      return;
    }
    System.out.println("Response from server: " + response.getResult());
  }

  /** Pet cats. */
  public void petCat(int times) {
    System.out.println("Will try to pet " + times + " cat(s)...");
    PetRequest request = PetRequest.newBuilder().setTimes(times).build();
    PetResponse response;
    try {
      response = blockingStub.pet(request);
    } catch (StatusRuntimeException e) {
      System.err.println("Error from server: " + e.getMessage());
      return;
    }
    System.out.println("Response from server: " + response.getResultsList().toString());
  }
}
