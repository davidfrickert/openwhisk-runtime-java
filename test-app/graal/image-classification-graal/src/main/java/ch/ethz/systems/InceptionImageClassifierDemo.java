package ch.ethz.systems;

import ch.ethz.systems.images.models.inception.InceptionImageClassifier;
import ch.ethz.systems.images.utils.ResourceUtils;
import com.dfrickert.simpleminioclient.SimpleMinioClient;
import com.dfrickert.simpleminioclient.auth.Credentials;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.graalvm.nativeimage.ImageInfo;
import org.graalvm.nativeimage.ImageSingletons;

import java.awt.image.BufferedImage;

import static ch.ethz.systems.images.utils.Constants.STORAGE;

public class InceptionImageClassifierDemo {
    private static InceptionImageClassifier classifier;

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final SimpleMinioClient minioClient = new SimpleMinioClient(STORAGE,
            new Credentials("minio", "minio123"));

    private static final boolean USE_PRELOADED_MODEL = false;

    private static synchronized void init() {
        System.setProperty("org.tensorflow.NativeLibrary.DEBUG", "1");
        if (classifier != null) return;

        if (USE_PRELOADED_MODEL && ImageInfo.inImageCode()) {
            classifier = ImageSingletons.lookup(InceptionImageClassifier.class);
        } else {
            try {
                classifier = new InceptionImageClassifier();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static ObjectNode predict(int index) {

        String[] image_names = new String[] { "tiger", "lion", "airplane", "eagle" };
        String file_name = image_names[index];
        String image_path = file_name + ".jpg";
        BufferedImage img = null;

        ObjectNode response = mapper.createObjectNode();

        try {
            img = ResourceUtils.getImage(minioClient.get("files/image-classification", image_path));
            String predicted_label = classifier.predict_image(img);

            response.put("predicted", predicted_label);

        } catch (Exception e) {
            response.put("error", e.getMessage());
        }


        return response;
    }

    /*
    public static void main(String[] args) throws IOException {
        Runtime rt = Runtime.getRuntime();
        int concurrency = 1;
        int number_of_tasks = 100;

        final long free = rt.freeMemory();

        long start = System.currentTimeMillis();
        init_classifier();
        long free_after_model = rt.freeMemory();
        System.out.println("Memory for the model: "+ Double.toString((free-free_after_model)/1000000.));
        long end = System.currentTimeMillis();
        System.out.println("Init time: " + (end - start) / 1000.);

        start = System.currentTimeMillis();
        ThreadPoolExecutor tpe = (ThreadPoolExecutor) Executors.newFixedThreadPool(concurrency);
        Future[] futures = new Future[number_of_tasks];
        for(int i = 0; i < number_of_tasks; i++){
            futures[i] = tpe.submit(() -> {
                predict(0);
                System.out.println("Memory for the model: "+ Double.toString((free - rt.freeMemory())/1000000.));
                return 0;
            });
        }
        for(int i = 0; i < number_of_tasks; i++){
            try {
                futures[i].get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        end = System.currentTimeMillis();
        System.out.println("Memory for the model: "+ Double.toString((free - rt.freeMemory())/1000000.));
        System.out.println("EXE time: " + (end - start) / 1000.);

        tpe.shutdown();
    }

     */

    public static ObjectNode main(JsonNode args) {
        init();
        return predict(args.get("index").intValue());
    }
}
