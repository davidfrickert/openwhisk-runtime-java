package ch.ethz.systems.images.utils;

import ch.ethz.systems.images.models.inception.InceptionImageClassifier;

import java.util.List;

public class ModelHolder {
    private final byte[] data;
    private final List<String> labels;

    public ModelHolder(byte[] data, List<String> labels) {
        this.data = data;
        this.labels = labels;
    }

    public InceptionImageClassifier load() {
        try {
            InceptionImageClassifier classifier = new InceptionImageClassifier();
            //classifier.loadModel(data);
            //classifier.loadLabels(labels);
            return classifier;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
