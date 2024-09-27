package kvstore.server;

import kvstore.CatServiceGrpc;
import kvstore.CreateRequest;
import kvstore.CreateResponse;
import kvstore.PetRequest;
import kvstore.PetResponse;

import io.grpc.stub.StreamObserver;
import io.grpc.Status;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CatServiceImpl extends CatServiceGrpc.CatServiceImplBase {
  private final int capacity;
  private List<Cat> cats;

  public CatServiceImpl(int capacity) {
    this.capacity = capacity;
    this.cats = new ArrayList<>();
  }

  @Override
  public void create(CreateRequest request, StreamObserver<CreateResponse> responseObserver) {
    // Check that request is valid
    if (request.getAge() <= 0 || "".equals(request.getName())) {
      responseObserver.onError(
              Status.INVALID_ARGUMENT.withDescription("Age and name must be set!").asRuntimeException());
      return;
    }

    // Check we have enough room
    if (cats.size() >= capacity) {
      responseObserver.onError(
              Status.INVALID_ARGUMENT.withDescription("Too many cats!").asRuntimeException());
      return;
    }

    // Process request
    Cat cat = new Cat(request.getName(), request.getAge());
    cats.add(cat);

    // Send response
    responseObserver.onNext(CreateResponse.newBuilder().setResult(true).build());
    responseObserver.onCompleted();
  }

  @Override
  public void pet(PetRequest request, StreamObserver<PetResponse> responseObserver) {
    // Check that request is valid
    if (request.getTimes() <= 0) {
      responseObserver.onError(
              Status.INVALID_ARGUMENT.withDescription("Times must be positive number").asRuntimeException());
      return;
    }

    // Check we have some cats
    if (cats.size() == 0) {
      responseObserver.onError(
              Status.INVALID_ARGUMENT.withDescription("Sorry but we don't have any cats!").asRuntimeException());
      return;
    }

    // Process request
    Random r = new Random();
    List<String> petResponses = new ArrayList<>(request.getTimes());
    for (int i = 0; i < request.getTimes(); i++) {
      Cat cat = cats.get(r.nextInt(cats.size()));
      petResponses.add(String.format("Meow! My name is %s, my age is %d, Meow~", cat.name, cat.age));
    }

    // Send response
    responseObserver.onNext(PetResponse.newBuilder().addAllResults(petResponses).build());
    responseObserver.onCompleted();
  }

  private static class Cat {
    public String name;
    public int age;
    public Cat(String name, int age) {
      this.name = name;
      this.age = age;
    }
  }
}