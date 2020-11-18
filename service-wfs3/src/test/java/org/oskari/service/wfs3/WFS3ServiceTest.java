package org.oskari.service.wfs3;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;
import org.oskari.service.wfs3.model.WFS3CollectionInfo;
import org.oskari.service.wfs3.model.WFS3Content;
import org.oskari.service.wfs3.model.WFS3ReqClasses;

public class WFS3ServiceTest {

    private WFS3Service service;

    @Before
    public void setup() throws Exception {
        WFS3ReqClasses reqClasses;
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("wfs3Conformance.json")) {
            reqClasses = WFS3Service.load(in, WFS3ReqClasses.class);
        }
        WFS3Content content;
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("wfs3Content.json")) {
            content = WFS3Service.load(in, WFS3Content.class);
        }
        service = new WFS3Service(reqClasses, content);
    }

    @Test
    public void testToJsonFromJson() throws IOException {
        byte[] json = WFS3Service.toJSON(service);
        assertEquals(service, WFS3Service.fromJSON(json));
    }

    @Test
    public void testCollectionsWereParsedCorrectly() throws Exception {
        List<String> expected = Stream.of("placenames", "places", "mapnames", "placenames_simple")
                .sorted()
                .collect(Collectors.toList());

        List<String> actual = service.getCollections().stream()
                .map(WFS3CollectionInfo::getId)
                .sorted()
                .collect(Collectors.toList());

        assertIterablesEquals(expected, actual);
    }

    @Test
    public void testGetSupportedCrsURIs() throws Exception {
        Set<String> expected = Stream.of(
                "http://www.opengis.net/def/crs/EPSG/0/3067",
                "http://www.opengis.net/def/crs/EPSG/0/4258",
                "http://www.opengis.net/def/crs/OGC/1.3/CRS84",
                "http://www.opengis.net/def/crs/EPSG/0/3046",
                "http://www.opengis.net/def/crs/EPSG/0/3047",
                "http://www.opengis.net/def/crs/EPSG/0/3048",
                "http://www.opengis.net/def/crs/EPSG/0/3873",
                "http://www.opengis.net/def/crs/EPSG/0/3874",
                "http://www.opengis.net/def/crs/EPSG/0/3875",
                "http://www.opengis.net/def/crs/EPSG/0/3876",
                "http://www.opengis.net/def/crs/EPSG/0/3877",
                "http://www.opengis.net/def/crs/EPSG/0/3878",
                "http://www.opengis.net/def/crs/EPSG/0/3879",
                "http://www.opengis.net/def/crs/EPSG/0/3880",
                "http://www.opengis.net/def/crs/EPSG/0/3881",
                "http://www.opengis.net/def/crs/EPSG/0/3882",
                "http://www.opengis.net/def/crs/EPSG/0/3883",
                "http://www.opengis.net/def/crs/EPSG/0/3884",
                "http://www.opengis.net/def/crs/EPSG/0/3885")
                .collect(Collectors.toSet());
        Set<String> actual = service.getSupportedCrsURIs("placenames");

        List<String> expectedList = new ArrayList<>(expected);
        List<String> actualList = new ArrayList<>(actual);

        Collections.sort(expectedList);
        Collections.sort(actualList);

        assertIterablesEquals(expectedList, actualList);
    }

    private <T> void assertIterablesEquals(Iterable<T> expected, Iterable<T> actual) {
        Iterator<T> expectedIter = expected.iterator();
        Iterator<T> actualIter = actual.iterator();
        while (expectedIter.hasNext()) {
            assertTrue(actualIter.hasNext());
            assertTrue(Objects.equals(expectedIter.next(), actualIter.next()));
        }
        assertFalse(actualIter.hasNext());
    }

}
