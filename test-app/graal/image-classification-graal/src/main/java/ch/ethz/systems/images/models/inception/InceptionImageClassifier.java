package ch.ethz.systems.images.models.inception;

import ai.djl.MalformedModelException;
import ai.djl.inference.Predictor;
import ai.djl.modality.Classifications;
import ai.djl.modality.cv.BufferedImageFactory;
import ai.djl.modality.cv.Image;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.tensorflow.zoo.TfModelZoo;
import ai.djl.translate.TranslateException;
import ch.ethz.systems.images.utils.ImageUtils;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class InceptionImageClassifier implements AutoCloseable {

    private ZooModel<Image, Classifications> model;
    private List<String> labels = new ArrayList<>();

    public InceptionImageClassifier() throws ModelNotFoundException, MalformedModelException, IOException {
        model = new TfModelZoo()
                .getModelLoader("resnet")
                .loadModel(Criteria.builder().setTypes(Image.class, Classifications.class).build());
    }

    /*
    public void loadModel(byte[] modelData) {
        graph.importGraphDef(modelData);
    }

    public void loadLabels(List<String> labels) {
        this.labels.clear();
        this.labels.addAll(labels);
    }
     */

    public String predict_image(BufferedImage image) throws TranslateException {
        return predict_image(image, 224, 224);
    }

    public String predict_image(BufferedImage image, int imgWidth, int imgHeight) throws TranslateException {
        System.out.println("starting predict");
        image = ImageUtils.resizeImage(image, imgWidth, imgHeight);

        Predictor<Image, Classifications> predictor = model.newPredictor();

        return predictor.predict(new BufferedImageFactory().fromImage(image)).toString();
    }

    @Override
    public void close() throws Exception {
        model.close();
    }
}
