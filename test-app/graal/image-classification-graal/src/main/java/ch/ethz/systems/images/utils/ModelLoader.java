package ch.ethz.systems.images.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class ModelLoader {

    public static ModelHolder toHolderFromFs() throws IOException {
        InputStream inputStream = ResourceUtils.getInputStream("tensorflow_inception_graph.pb");
        byte[] modelData = InputStreamUtils.getBytes(inputStream);

        inputStream = ResourceUtils.getInputStream("imagenet_comp_graph_label_strings.txt");

        List<String> labels = new ArrayList<>();
        try(BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while((line = reader.readLine()) != null) {
                labels.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new ModelHolder(modelData, labels);
    }
}
