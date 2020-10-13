package com.grpc.fileservice.client;

import com.grpc.protobuf.DataChunk;
import com.grpc.protobuf.DownloadFileRequest;
import com.grpc.protobuf.FileServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

@Service
public class ImageStreamingClient {
    
    private final Logger LOG = Logger.getLogger(ImageStreamingClient.class.getName());
    private final FileServiceGrpc.FileServiceStub nonBlockingStub;
    
    @Autowired
    public ImageStreamingClient(@Value("${fileservice.grpc.host:localhost}") String host, @Value("${fileservice.grpc.port:7000}") int port) {
        
        this(ManagedChannelBuilder.forAddress(host, port)
                .keepAliveTime(60, TimeUnit.SECONDS)
                .keepAliveWithoutCalls(true)
                .usePlaintext());
    }
    
    private ImageStreamingClient(ManagedChannelBuilder channelBuilder) {
        
        ManagedChannel channel = channelBuilder.build();
        nonBlockingStub = FileServiceGrpc.newStub(channel);
    }
    
    public ByteArrayOutputStream downloadFile(String fileUrl) {
        
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final CountDownLatch finishLatch = new CountDownLatch(1);
        final AtomicBoolean completed = new AtomicBoolean(false);
        
        StreamObserver<DataChunk> streamObserver = new StreamObserver<DataChunk>() {
            
            @Override
            public void onNext(DataChunk dataChunk) {
                try {
                    baos.write(dataChunk.getData().toByteArray());
                } catch (IOException e) {
                    LOG.info("error on write to byte array stream");
                    onError(e);
                }
            }
            
            @Override
            public void onCompleted() {
                LOG.info("downloadFile() has been completed!");
                completed.compareAndSet(false, true);
                finishLatch.countDown();
            }
            
            @Override
            public void onError(Throwable t) {
                LOG.info("downloadFile() error");
                finishLatch.countDown();
            }
        };

        try {
            DownloadFileRequest.Builder builder = DownloadFileRequest
                    .newBuilder()
                    .setUrl(fileUrl);

            nonBlockingStub.downloadFile(builder.build(), streamObserver);
            finishLatch.await(5, TimeUnit.MINUTES);
    
            if (!completed.get()) {
                throw new Exception("The downloadFile() method did not complete");
            }
        } catch (Exception e) {
            LOG.info("The downloadFile() method did not complete");
        }
        
        return baos;
    }
}
