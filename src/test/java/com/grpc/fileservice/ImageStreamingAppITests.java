package com.grpc.fileservice;

import com.grpc.fileservice.client.ImageStreamingClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.lognet.springboot.grpc.context.LocalRunningGrpcPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import sun.awt.image.ToolkitImage;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

import static org.junit.Assert.*;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;


@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = RANDOM_PORT, properties = {"grpc.port=0"})
public class ImageStreamingAppITests {
    
    private final Logger LOG = Logger.getLogger(ImageStreamingAppITests.class.getName());
    
    @LocalRunningGrpcPort
    private int port;
    
    private ImageStreamingClient fileServiceClient;
    private final Map<String, int[]> map = new LinkedHashMap<>();
    
    @Before
    public void setup() {
        fileServiceClient = new ImageStreamingClient("localhost", port);
        populateMapData();
    }
    
    private void populateMapData() {
        map.put("https://static01.nyt.com/images/2020/10/04/world/04london-dispatch-top/merlin_177483009_99edefd2-bf5f-4eb6-98b8-02d143e8181d-jumbo.jpg?quality=90&auto=webp", new int[]{1024, 644});
        map.put("https://static01.nyt.com/images/2020/09/25/well/00well-teen-wellness/00well-teen-wellness-jumbo.jpg?quality=90&auto=webp", new int[]{1024, 683});
    }
    
    @Test
    public void test_downloadImageFile() throws IOException {
        
        Iterator it = map.entrySet().iterator();
        int index = 0;
        
        while (it.hasNext()) {
            Map.Entry<String, int[]> pair = (Map.Entry) it.next();
            
            String url = pair.getKey();
            int[] dimensions = pair.getValue();
            
            BufferedImage bufferedImage = getFileFromInternet(url);
            
            File file = new File("download/test_" + index + ".png");
            write(file, bufferedImage);
            
            assertNotNull(file);
            assertEquals("Image width needs to be same", bufferedImage.getWidth(), dimensions[0]);
            assertEquals("Image height needs to be same", bufferedImage.getHeight(), dimensions[1]);
            
            index++;
            it.remove();
        }
    }
    
    private BufferedImage getFileFromInternet(String fileUrl) {
        
        BufferedImage bufferedImage = null;
        try {
            
            ByteArrayOutputStream imageOutputStream = fileServiceClient.downloadFile(fileUrl);
            
            byte[] bytes = imageOutputStream.toByteArray();
            
            ImageIcon imageIcon = new ImageIcon(bytes);
            Image image = imageIcon.getImage();
            int width = ((ToolkitImage) image).getWidth();
            int height = ((ToolkitImage) image).getHeight();
            
            bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
            Graphics g = bufferedImage.getGraphics();
            g.drawImage(image, 0, 0, null);
            
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        
        return bufferedImage;
    }
    
    private void write(File tmpFile, BufferedImage bi) throws IOException {
        
        ImageIO.write(bi, "png", tmpFile);
        LOG.info("File has been downloaded: " + tmpFile.getAbsolutePath());
    }
}
