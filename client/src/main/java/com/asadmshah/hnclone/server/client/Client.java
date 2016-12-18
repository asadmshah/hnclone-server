package com.asadmshah.hnclone.server.client;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class Client {

    public static void main(String[] args) {
        final ManagedChannel channel = ManagedChannelBuilder.forAddress("192.168.100.10", 50051)
                .usePlaintext(true)
                .build();

//        final UserServicesGrpc.UserServicesBlockingStub stub = UserServicesGrpc.newBlockingStub(channel);
//
//        int n = 100;
//        List<UserCreateRequest> requests = new ArrayList<>(n);
//        for (int i = 0; i < n; i++) {
//            requests.add(UserCreateRequest.newBuilder()
//                    .setName("Name " + i)
//                    .setPass("Pass " + i)
//                    .setAbout("About " + i)
//                    .build());
//        }
//
//        CountDownLatch counter = new CountDownLatch(n);
//        StreamObserver<User> observer = new StreamObserver<User>() {
//            @Override
//            public void onNext(User value) {
//                counter.countDown();
//                System.out.println("Created: " + value.getName());
//            }
//
//            @Override
//            public void onError(Throwable t) {
//                t.printStackTrace();
//            }
//
//            @Override
//            public void onCompleted() {
//                System.out.println("onCompleted");
//            }
//        };

//        long timeA = System.nanoTime();
//        for (UserCreateRequest request : requests) {
//            stub.create(request);
//            counter.countDown();
//            stub.create(request, observer);
//        }
//
//        try {
//            counter.await(30, TimeUnit.SECONDS);
//            long timeB = System.nanoTime();
//            long timeD = TimeUnit.NANOSECONDS.toSeconds(timeB - timeA);
//            System.out.println(String.format("Completed in: %d", timeD));
//            channel.shutdown();
//            System.out.println("Done");
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//            return;
//        }
    }

}
