package kvstore.server;

import io.grpc.Grpc;
import io.grpc.InsecureServerCredentials;
import io.grpc.Server;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class RPCServer {
  private final Server server;
  private final int serverPort;

  public RPCServer() {
    this.serverPort = 32000;
    server = Grpc.newServerBuilderForPort(serverPort, InsecureServerCredentials.create())
            .addService(new KeyValueStoreImpl()).build();
  }
  public RPCServer(int serverPort) {
    this.serverPort = serverPort;
    server = Grpc.newServerBuilderForPort(serverPort, InsecureServerCredentials.create())
            .addService(new KeyValueStoreImpl()).build();
  }

  public void start() {
    try {
      server.start();
      ServerLogger.info("Server is listening on: " + serverPort);
    } catch (IOException e) {
      ServerLogger.error("Failed in starting RPC server: " + e.getMessage());
      System.exit(1);
    }

    Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        // Use stderr here since the logger may have been reset by its JVM shutdown hook.
        ServerLogger.error("*** shutting down gRPC server since JVM is shutting down");
        try {
          RPCServer.this.stop();
        } catch (InterruptedException e) {
          ServerLogger.error(e.getMessage());
        }
        ServerLogger.error("*** server shut down");
      }
    });
  }

  public void stop() throws InterruptedException {
    if (server != null) {
      server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
    }
  }

  /**
   * Await termination on the main thread since the grpc library uses daemon threads.
   */
  public void blockUntilShutdown() throws InterruptedException {
    if (server != null) {
      server.awaitTermination();
    }
  }
}
