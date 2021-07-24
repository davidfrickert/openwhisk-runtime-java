package ch.ethz.systems;

import org.graalvm.nativeimage.hosted.Feature;

public class ImageClassifierSingleton implements Feature {

    @Override
    public void afterRegistration(AfterRegistrationAccess access) {
            /*ModelHolder modelHolder = ModelLoader.toHolderFromFs();
            InceptionImageClassifier classifier = new InceptionImageClassifier();
            ImageSingletons.add(InceptionImageClassifier.class, classifier);

             */
    }
}
