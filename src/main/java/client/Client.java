package kvstore.client;


public interface Client {
  void prePopulateRequests();

  void handleUserRequests();
}
