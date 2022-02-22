package pt.ulisboa.ist;

import com.dfrickert.simpleminioclient.SimpleMinioClient;
import com.dfrickert.simpleminioclient.auth.Credentials;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.ContentType;
import org.apache.http.util.EntityUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Iconify {
    private static ObjectMapper mapper;
    private static SimpleMinioClient client;

    private static final float MAX_WIDTH = 100;
    private static final float MAX_HEIGHT = 100;
    private static final String JPG_TYPE = "jpg";
    private static final String JPG_MIME = "image/jpeg";
    private static final String PNG_TYPE = "png";
    private static final String PNG_MIME = "image/png";

    private static synchronized void initialize() {
        if (client == null) {
            mapper = new ObjectMapper();
            client = new SimpleMinioClient("http://146.193.41.231:8999", new Credentials("minio", "minio123"));
            // Thread to cleanup resources when interrupt is called on all active threads
            new Thread() {
                @Override
                public void interrupt() {
                    super.interrupt();
                    client.close();
                    client = null;
                }

                @Override
                public void run() {
                    try {
                        sleep(Long.MAX_VALUE);
                    } catch (InterruptedException ignored) { }
                }
            }.start();
        }
    }

    public static ObjectNode main(JsonNode args) {
        final ObjectNode response = mapper.createObjectNode();

        try {
            String filename = args.get("filename").asText();

            Matcher matcher = Pattern.compile(".*\\.([^\\.]*)").matcher(filename);
            if (!matcher.matches()) {
                response.put("error", "unable to infer file type");
                return response;
            }
            String imageType = matcher.group(1);
            if (!(JPG_TYPE.equals(imageType)) && !(PNG_TYPE.equals(imageType))) {
                response.put("error", "only PNG or JPG supported.");
                return response;
            }

            CloseableHttpResponse getResponse = client.get("files",  filename);

            if (getResponse.getStatusLine().getStatusCode() >= 400) {
                response.put("internalErrorCode", getResponse.getStatusLine().getStatusCode())
                        .put("detailedError", EntityUtils.toString(getResponse.getEntity()));
                return response;
            }
            InputStream imageIS = getResponse.getEntity().getContent();
            BufferedImage srcImage = ImageIO.read(imageIS);

            int srcHeight = srcImage.getHeight();
            int srcWidth = srcImage.getWidth();
            // Infer the scaling factor to avoid stretching the image
            // unnaturally
            float scalingFactor = Math.min(MAX_WIDTH / srcWidth, MAX_HEIGHT
                    / srcHeight);
            int width = (int) (scalingFactor * srcWidth);
            int height = (int) (scalingFactor * srcHeight);

            BufferedImage resizedImage = new BufferedImage(width, height,
                    BufferedImage.TYPE_INT_RGB);
            Graphics2D g = resizedImage.createGraphics();
            // Fill with white before applying semi-transparent (alpha) images
            g.setPaint(Color.white);
            g.fillRect(0, 0, width, height);
            // Simple bilinear resize
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(srcImage, 0, 0, width, height, null);
            g.dispose();

            // Re-encode image to target format
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            ImageIO.write(resizedImage, imageType, os);
            InputStream is = new ByteArrayInputStream(os.toByteArray());

            CloseableHttpResponse putResponse = client.put("files/icons", filename, is);

            if (putResponse.getStatusLine().getStatusCode() >= 400) {
                HttpEntity entity = putResponse.getEntity();

                response
                        .put("status", "error")
                        .put("internalErrorCode", putResponse.getStatusLine().getStatusCode());
                if (entity != null) {
                    ContentType receivedContentType = ContentType.get(entity);
                    String body = EntityUtils.toString(entity);
                    if (ContentType.APPLICATION_JSON.equals(receivedContentType)) {
                        response.set("detailedError", mapper.readTree(body));
                    } else {
                        response.put("detailedError", body);
                    }
                }
                return response;
            }

            return response
                    .put("status", "success")
                    .put("output", "files/icons/" + filename);

        } catch (Exception e) {
            e.printStackTrace();
            return response
                    .put("status", "error")
                    .put("errorMessage", e.getMessage())
                    .put("detailedErrorMessage", ExceptionUtils.getStackTrace(e));
        }
    }
}
