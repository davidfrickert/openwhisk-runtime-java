package pt.ulisboa.ist;

import com.dfrickert.simpleminioclient.SimpleMinioClient;
import com.dfrickert.simpleminioclient.auth.Credentials;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.ContentType;
import org.apache.http.util.EntityUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Iconify {

    private static final float MAX_WIDTH = 100;
    private static final float MAX_HEIGHT = 100;
    private static final String JPG_TYPE = "jpg";
    private static final String JPG_MIME = "image/jpeg";
    private static final String PNG_TYPE = "png";
    private static final String PNG_MIME = "image/png";

    public static JsonObject main(JsonObject args, Map<String, Object> globals, int id) {
        final SimpleMinioClient client = new SimpleMinioClient("http://10.147.18.27:8999", new Credentials("minio", "minio123"));

        JsonObject response = new JsonObject();

        try {
            String filename = args.getAsJsonPrimitive("filename").getAsString();

            Matcher matcher = Pattern.compile(".*\\.([^\\.]*)").matcher(filename);
            if (!matcher.matches()) {
                response.addProperty("error", "unable to infer file type");
                return response;
            }
            String imageType = matcher.group(1);
            if (!(JPG_TYPE.equals(imageType)) && !(PNG_TYPE.equals(imageType))) {
                response.addProperty("error", "only PNG or JPG supported.");
                return response;
            }

            InputStream imageIS = client.get("files",  filename);
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

                response.addProperty("status", "error");
                response.addProperty("internalErrorCode", putResponse.getStatusLine().getStatusCode());

                if (entity != null) {
                    ContentType receivedContentType = ContentType.get(entity);
                    String body = EntityUtils.toString(entity);
                    response.addProperty("detailedError", body);
                }

            }

            response.addProperty("status", "success");
            response.addProperty("output", "files/icons/" + filename);

        } catch (Exception e) {
            e.printStackTrace();
            response.addProperty("status", "error");
            response.addProperty("errorMessage", e.getMessage());
            response.addProperty("detailedErrorMessage", ExceptionUtils.getStackTrace(e));
        }
        return response;
    }
}
