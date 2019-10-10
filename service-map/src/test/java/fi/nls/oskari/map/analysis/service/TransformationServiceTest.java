package fi.nls.oskari.map.analysis.service;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xml.sax.SAXException;

import fi.nls.oskari.service.ServiceException;
import fi.nls.oskari.util.IOHelper;

public class TransformationServiceTest {

    private static final String UUID = "test-uuid";
    private static final long ANALYSIS_ID = 1L;
    private static final String GEOMETRY_PROPERTY = "geom";
    private static final String NS_PREFIX = "feature";
    private static final String TEST_RESOURCE_FOLDER = "fi/nls/oskari/map/analysis/service/";
    private static final String INPUT_FILE_PATH_BUFFER = TEST_RESOURCE_FOLDER
            + "TransformationServiceTest-wps-input-buffer.xml";
    private static final String EXPECTED_FILE_PATH_BUFFER = TEST_RESOURCE_FOLDER
            + "TransformationServiceTest-expected-wfst-result-buffer.xml";
    private static final String INPUT_FILE_PATH_DESCRIPTIVE_STATISTICS = TEST_RESOURCE_FOLDER
            + "TransformationServiceTest-wps-input-descriptive-statistic.xml";
    private static final String EXPECTED_FILE_PATH_DESCRIPTIVE_STATISTICS = TEST_RESOURCE_FOLDER
            + "TransformationServiceTest-expected-wfst-result-descriptive-statistic.xml";
    
    private static final String INPUT_FILE_PATH_MERGE_PROPERTIES_TO_FEATURES = TEST_RESOURCE_FOLDER
            + "TransformationServiceTest-merge-properties-to-features-input-featureset.xml";
    private static final String EXPECTED_FILE_PATH_MERGE_PROPERTIES_TO_FEATURES = TEST_RESOURCE_FOLDER
            + "TransformationServiceTest-merge-properties-to-features-expected.xml";
    
    private static final String MERGE_PROPERTIES_TO_FEATURES_AGGREGATE_RESULTS = 
            "{\"lkmhapa\":{\"Pienin arvo\":0,\"Suurin arvo\":1,\"Keskiarvo\":0.625,\"Mediaani\":1,\"Keskihajonta\":0.4841229182759271,\"Summa\":5,\"Kohteiden lukumäärä\":8},\"lkmlaka\":{\"Pienin arvo\":0,\"Suurin arvo\":2,\"Keskiarvo\":0.375,\"Mediaani\":0,\"Keskihajonta\":0.6959705453537527,\"Summa\":3,\"Kohteiden lukumäärä\":8},\"lkmmo\":{\"Pienin arvo\":0,\"Suurin arvo\":1,\"Keskiarvo\":0.25,\"Mediaani\":0,\"Keskihajonta\":0.4330127018922193,\"Summa\":2,\"Kohteiden lukumäärä\":8},\"lkmmp\":{\"Pienin arvo\":0,\"Suurin arvo\":1,\"Keskiarvo\":0.25,\"Mediaani\":0,\"Keskihajonta\":0.43301270189221935,\"Summa\":2,\"Kohteiden lukumäärä\":8},\"x\":{\"Pienin arvo\":273615.5613,\"Suurin arvo\":284578.2304,\"Keskiarvo\":276509.3736875,\"Mediaani\":275350.70005,\"Keskihajonta\":3385.6776776142774,\"Summa\":2212074.9895,\"Kohteiden lukumäärä\":8},\"kkonn\":{\"Pienin arvo\":1,\"Suurin arvo\":10,\"Keskiarvo\":6,\"Mediaani\":6,\"Keskihajonta\":2.7386127875258306,\"Summa\":48,\"Kohteiden lukumäärä\":8},\"y\":{\"Pienin arvo\":6638539.3841,\"Suurin arvo\":6644676.5617,\"Keskiarvo\":6640326.4626625,\"Mediaani\":6639571.472,\"Keskihajonta\":2022.9991372563804,\"Summa\":5.31226117013E7,\"Kohteiden lukumäärä\":8},\"lkmpp\":{\"Pienin arvo\":0,\"Suurin arvo\":1,\"Keskiarvo\":0.25,\"Mediaani\":0,\"Keskihajonta\":0.4330127018922193,\"Summa\":2,\"Kohteiden lukumäärä\":8},\"lkmjk\":{\"Pienin arvo\":0,\"Suurin arvo\":1,\"Keskiarvo\":0.125,\"Mediaani\":0,\"Keskihajonta\":0.33071891388307384,\"Summa\":1,\"Kohteiden lukumäärä\":8},\"lkmmuukulk\":{\"Pienin arvo\":0,\"Suurin arvo\":0,\"Keskiarvo\":0,\"Mediaani\":0,\"Keskihajonta\":0,\"Summa\":0,\"Kohteiden lukumäärä\":8}}";
    
    private static final List<String> MERGE_PROPERTIES_TO_FEATURES_ROW_ORDER  = 
            Arrays.asList("kkonn", "lkmhapa", "lkmjk", "lkmlaka", "lkmmo", "lkmmp", "lkmmuukulk", "lkmpp", "x", "y");
   
    private static final List<String> MERGE_PROPERTIES_TO_FEATURES_COLUMN_ORDER  = 
            Arrays.asList("Kohteiden lukumäärä", "Summa", "Pienin arvo", "Suurin arvo", "Keskiarvo", "Keskihajonta", "Mediaani");
    
    
    private TransformationService service = new TransformationService();
    
    @BeforeClass
    public static void setup() {
        XMLUnit.setIgnoreWhitespace(true);
    }
    
    @Test
    public void testWpsFeatureCollectionToWfstBuffer() throws ServiceException, SAXException, IOException {

        Map<String, String> fieldTypes = new HashMap<>();
        fieldTypes.put("onntyyppi", "string");
        fieldTypes.put("lkmpp", "numeric");
        fieldTypes.put("lkmjk", "numeric");
        fieldTypes.put("geom", "string");
        fieldTypes.put("lkmmuukulk", "numeric");
        fieldTypes.put("lkmhapa", "numeric");
        fieldTypes.put("lkmlaka", "numeric");
        fieldTypes.put("lkmmo", "numeric");
        fieldTypes.put("lkmmp", "numeric");
        fieldTypes.put("x", "numeric");
        fieldTypes.put("vvonn", "string");
        fieldTypes.put("kkonn", "numeric");
        fieldTypes.put("vakav", "string");
        fieldTypes.put("y", "numeric");
        
        List<String> fields = new ArrayList<String>();
        testWpsToWfs(INPUT_FILE_PATH_BUFFER, EXPECTED_FILE_PATH_BUFFER, fields, fieldTypes);
    }

    @Test
    public void testWpsFeatureCollectionToWfstDescriptiveStatistics()
            throws ServiceException, SAXException, IOException {

        Map<String, String> fieldTypes = new HashMap<>();
        fieldTypes.put("Mediaani", "numeric");
        fieldTypes.put("Pienin_arvo", "numeric");
        fieldTypes.put("Summa", "numeric");
        fieldTypes.put("Keskihajonta", "numeric");
        fieldTypes.put("Kohteiden_lukumäärä", "numeric");
        fieldTypes.put("Suurin_arvo", "numeric");
        fieldTypes.put("Keskiarvo", "numeric");
        
        List<String> fields = new ArrayList<String>();
        testWpsToWfs(INPUT_FILE_PATH_DESCRIPTIVE_STATISTICS, EXPECTED_FILE_PATH_DESCRIPTIVE_STATISTICS, fields, fieldTypes);
    }
    
    @Test
    public void testMergePropertiesToFeatures() throws ServiceException, IOException, SAXException {
        
        String featureSet = readResource(INPUT_FILE_PATH_MERGE_PROPERTIES_TO_FEATURES);
       
        String result = service.mergePropertiesToFeatures(featureSet, MERGE_PROPERTIES_TO_FEATURES_AGGREGATE_RESULTS, 
                MERGE_PROPERTIES_TO_FEATURES_ROW_ORDER, MERGE_PROPERTIES_TO_FEATURES_COLUMN_ORDER);
        
        String expected = readResource(EXPECTED_FILE_PATH_MERGE_PROPERTIES_TO_FEATURES);
        
        assertXmlIsValid(expected, result);
    }

    private void testWpsToWfs(String inputFilePath, String expectedFilePath, List<String> fields, Map<String, String> fieldTypes)
            throws ServiceException, IOException, SAXException {
        
        String wpsFeatures = readResource(inputFilePath);
        String expected = readResource(expectedFilePath);

        String result = service.wpsFeatureCollectionToWfst(wpsFeatures, UUID, ANALYSIS_ID, fields, fieldTypes,
                GEOMETRY_PROPERTY, NS_PREFIX);

        assertXmlIsValid(expected, result);
    }

    private String readResource(String p) throws IOException {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(p)) {
            return new String(IOHelper.readBytes(in), StandardCharsets.UTF_8);
        }
    }
    
    private void assertXmlIsValid(String expected, String actual) throws IOException, SAXException {
        
        Diff xmlDiff = new Diff(expected, actual);
        assertTrue(String.format("Result xml does not equal expected: %s.", xmlDiff.toString()), xmlDiff.similar());
    }
}
