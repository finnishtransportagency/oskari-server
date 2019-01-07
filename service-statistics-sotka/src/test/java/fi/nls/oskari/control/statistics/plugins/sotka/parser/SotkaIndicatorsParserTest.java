package fi.nls.oskari.control.statistics.plugins.sotka.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import fi.nls.oskari.control.statistics.data.StatisticalIndicator;
import fi.nls.oskari.control.statistics.plugins.sotka.SotkaConfig;
import fi.nls.oskari.util.JSONHelper;
import fi.nls.test.util.TestHelper;

import fi.nls.oskari.control.statistics.data.IndicatorValueType;
import fi.nls.oskari.util.PropertyUtil;
import fi.nls.test.util.ResourceHelper;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.NamingException;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class SotkaIndicatorsParserTest {
    private static String testResponse = ResourceHelper.readStringResource("SotkaIndicators.json",
            SotkaIndicatorsParserTest.class);
    private static String testExpectedIndicator = ResourceHelper.readStringResource("Sotka-expected.json",
            SotkaIndicatorsParserTest.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @BeforeClass
    public static void init() throws NamingException, IllegalArgumentException,
        IllegalAccessException {
        PropertyUtil.loadProperties("/oskari-ext.properties");
    }
    @AfterClass
    public static void tearDown() {
        PropertyUtil.clearProperties();
    }

    @Test
    public void testParseIndicators() throws Exception {
        org.junit.Assume.assumeTrue(TestHelper.canDoHttp());
        org.junit.Assume.assumeTrue(TestHelper.redisAvailable());

        Map<String, Long> layerMap = new HashMap<>();
        layerMap.put("kunta", 9l);
        layerMap.put("maakunta", 10l);
        layerMap.put("maa", 11l);

        List<StatisticalIndicator> parsedObject = new ArrayList<>();
        SotkaConfig config = new SotkaConfig(new JSONObject(), 1);
        JSONArray responseJSON = new JSONArray(testResponse);
        SotkaIndicatorParser parser = new SotkaIndicatorParser(config);
        for (int i = 0; i < responseJSON.length(); i++) {
            StatisticalIndicator indicator = parser.createIndicator(responseJSON.getJSONObject(i), layerMap);
            if (indicator != null) {
                parsedObject.add(indicator);
            }
        }

        String json = MAPPER.writeValueAsString(parsedObject.get(0));
        assertTrue("The parsed object did not match the expected first objects.",
                JSONHelper.isEqual(JSONHelper.createJSONObject(json), JSONHelper.createJSONObject(testExpectedIndicator)));
        assertEquals(2373, parsedObject.size());
        assertEquals("245", parsedObject.get(40).getId());
        assertEquals(3, parsedObject.get(40).getLayers().size());
        assertEquals(IndicatorValueType.FLOAT, parsedObject.get(40).getLayers().get(2).getIndicatorValueType());
        assertEquals(11, parsedObject.get(40).getLayers().get(2).getOskariLayerId());
        assertEquals("{fi=Syöpäindeksi, ikävakioitu, sv=Cancerindex, åldersstandardiserat, en=Cancer index, age-standardised}",
                parsedObject.get(40).getName().toString());
        assertEquals("{fi=Terveyden ja hyvinvoinnin laitos (THL), sv=Institutet för hälsa och välfärd (THL), " +
                "en=Institute for Health and Welfare (THL)}",
                parsedObject.get(40).getSource().toString());
        // Note that the selectors are empty here, because this indicator has no allowed values for "sex".
        //assertEquals("{[{ id: year, value: null, allowedValues: [2001, 2002, 2003, 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011]}]}", parsedObject.get(40).getDataModel().toString());

    }
}
