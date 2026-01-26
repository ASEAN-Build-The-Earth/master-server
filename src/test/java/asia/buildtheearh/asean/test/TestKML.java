package asia.buildtheearh.asean.test;

import asia.buildtheearth.asean.geotools.kml.store.KMLFeatureReader;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.kml.KML;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.locationtech.jts.geom.Geometry;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

@DisplayName("Test KML file")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public final class TestKML {
    public static final class TestFiles extends ResourceProvider {
        public TestFiles() {
            super(Arguments.argumentSet("Test KML file", "kml-test-01.kml"));
        }
    }

    @Order(1)
    @DisplayName("Read KML file features")
    @ParameterizedTest
    @ArgumentsSource(TestFiles.class)
    public void readKML(TemporaryTestFile temp, @TempDir Path directory) throws IOException {

        File file = temp.apply(directory);
        System.out.print(file.getPath());

        try(KMLFeatureReader reader = new KMLFeatureReader(file, KML.Placemark)) {
            System.out.println("has feature " + reader.hasNext());

            // Write each feature into geojson file
            while(reader.hasNext()) {
                SimpleFeature feature = reader.next();

                Object defaultGeometry = feature.getDefaultGeometry();

                Assertions.assertNotNull(defaultGeometry, "All Placemark should have geometry");

                if(defaultGeometry instanceof Geometry geometry) {
                    System.out.println(geometry.getClass()); // Note: toString doesn't work on minified library

                }
                else Assertions.fail("Geometry should be of org.locationtech.jts.geom.Geometry");
            }
        }
    }
}
