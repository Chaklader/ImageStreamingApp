package com.grpc.fileservice.server;

import com.google.protobuf.ByteString;
import com.grpc.protobuf.DataChunk;
import com.grpc.protobuf.DownloadFileRequest;
import com.grpc.protobuf.FileServiceGrpc;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.apache.commons.io.IOUtils;
import org.lognet.springboot.grpc.GRpcService;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;


@GRpcService
public class ImageStreamingServerImpl extends FileServiceGrpc.FileServiceImplBase {
    
    @Override
    public void downloadFile(DownloadFileRequest request, StreamObserver<DataChunk> responseObserver) {
        
        try {
            
            URL url = new URL(request.getUrl());
            InputStream inputStream = new BufferedInputStream(url.openStream());
            
            byte[] bytes = IOUtils.toByteArray(inputStream);
            BufferedInputStream imageStream = new BufferedInputStream(new ByteArrayInputStream(bytes));
            
            
            int length;
            int bufferSize = 1024;
            
            final byte[] buffer = new byte[bufferSize];
            
            while ((length = imageStream.read(buffer, 0, bufferSize)) != -1) {
                responseObserver.onNext(DataChunk.newBuilder()
                        .setData(ByteString.copyFrom(buffer, 0, length))
                        .setSize(bufferSize)
                        .build());
            }
            
            imageStream.close();
            responseObserver.onCompleted();
        } catch (Throwable e) {
            responseObserver.onError(Status.ABORTED
                    .withDescription("Unable to acquire the image " + request.getUrl())
                    .withCause(e)
                    .asException());
        }
    }
}
