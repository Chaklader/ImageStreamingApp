package com.grpc.fileservice.controller;

import com.grpc.fileservice.client.ImageStreamingClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.ByteArrayOutputStream;
import java.util.logging.Logger;

@RestController
public class ImageStreamController {
    
    private final static Logger LOGGER = Logger.getLogger(ImageStreamController.class.getName());
    
    @Autowired
    private ImageStreamingClient streamingClient;
    
    @GetMapping(path = "/")
    public String getCallerAddress(HttpServletRequest request) {
        
        StringBuilder sb = new StringBuilder();
        sb.append("Application IP Address = ");
        
        if (request.getHeader("X-Forwarded-For") != null) {
            return sb.append(request.getHeader("X-Forwarded-For")).toString();
        } else {
            return sb.append(request.getRemoteAddr()).toString();
        }
    }
    
    @GetMapping(value = "/stream")
    @ResponseBody
    public byte[] getBarChartImage(String imageUrl) {
        
        ByteArrayOutputStream outputStream = streamingClient.downloadFile(imageUrl);
        byte[] bytes = outputStream.toByteArray();
        
        LOGGER.info("Image is created and receive the bytes array.");
        return bytes;
    }
}
