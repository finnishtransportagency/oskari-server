package fi.nls.oskari.map.layer.formatters;

import fi.mml.map.mapwindow.service.wms.WebMapService;
import fi.nls.oskari.domain.map.OskariLayer;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.util.IOHelper;
import fi.nls.oskari.util.JSONHelper;
import fi.nls.oskari.util.PropertyUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;
import static fi.nls.oskari.service.capabilities.CapabilitiesConstants.*;
/**
 * Created with IntelliJ IDEA.
 * User: SMAKINEN
 * Date: 17.12.2013
 * Time: 15:37
 * To change this template use File | Settings | File Templates.
 */
public class LayerJSONFormatterWMS extends LayerJSONFormatter {

    private static Logger log = LogFactory.getLogger(LayerJSONFormatterWMS.class);
    public static final String KEY_GFICONTENT = "gfiContent";
    public static final String KEY_ATTRIBUTES = "attributes";


    public JSONObject getJSON(OskariLayer layer,
                              final String lang,
                              final boolean isSecure,
                              final String crs) {

        final JSONObject layerJson = getBaseJSON(layer, lang, isSecure, crs);
        JSONHelper.putValue(layerJson, KEY_STYLE, layer.getStyle());
        JSONHelper.putValue(layerJson, KEY_GFICONTENT, layer.getGfiContent());

        if (layer.getGfiType() != null && !layer.getGfiType().isEmpty()) {
            // setup default if saved
            JSONObject formats = layerJson.optJSONObject(KEY_FORMATS);
            if(formats == null) {
                // create formats node if not found
                formats = JSONHelper.createJSONObject(KEY_VALUE, layer.getGfiType());
                JSONHelper.putValue(layerJson, KEY_FORMATS, formats);
            }
            else {
                JSONHelper.putValue(formats, KEY_VALUE, layer.getGfiType());
            }
        }
        try {
            JSONHelper.putValue(layerJson, KEY_STYLES, createStylesJSON(layer, isSecure));
        } catch (Exception e) {
            log.warn(e, "Populating layer styles failed for id: " + layer.getId());
        }
        includeCapabilitiesInfo(layerJson, layer, layer.getCapabilities());
        return layerJson;
    }

    public JSONObject getJSON(OskariLayer layer,
                              final String lang,
                              final boolean isSecure,
                              final String crs,
                              final WebMapService capabilities) {

        final JSONObject layerJson = getJSON(layer, lang, isSecure, crs);
        final JSONObject capsJSON = createCapabilitiesJSON(capabilities);
        includeCapabilitiesInfo(layerJson, layer, capsJSON);

        return layerJson;
    }

    /**
     * Populate JSON with capabilities values
     * @param layerJson
     * @param layer
     * @param capabilities
     */
    private void includeCapabilitiesInfo(final JSONObject layerJson,
                                         final OskariLayer layer,
                                         final JSONObject capabilities) {
        if(capabilities == null) {
            return;
        }

        JSONHelper.putValue(layerJson, KEY_FORMATS, capabilities.optJSONObject(KEY_FORMATS));

        final JSONObject attrs = layer.getAttributes();
        if(attrs != null && attrs.has(KEY_ISQUERYABLE)) {
            // attributes can be used to force GFI for layer even if capabilities allow it or enable it not
            JSONHelper.putValue(layerJson, KEY_ISQUERYABLE, attrs.optBoolean(KEY_ISQUERYABLE));
        } else {
            JSONHelper.putValue(layerJson, KEY_ISQUERYABLE, capabilities.optBoolean(KEY_ISQUERYABLE));
        }

        // Do not override version, if already available
        if(!layerJson.has(KEY_VERSION)) {
            JSONHelper.putValue(layerJson, KEY_VERSION, JSONHelper.getStringFromJSON(capabilities, KEY_VERSION, null));
        }

        // copy time from capabilities to attributes
        // timedata is merged into attributes  (times:{start:,end:,interval:}  or times: []
        // only reason for this is that admin can see the values offered by service
        if(capabilities.has(KEY_TIMES) && isTimeseriesLayer(layer)) {
            JSONHelper.putValue(layerJson, KEY_ATTRIBUTES, JSONHelper.merge(
                    JSONHelper.getJSONObject(layerJson, KEY_ATTRIBUTES),
                    JSONHelper.createJSONObject(KEY_TIMES, JSONHelper.get(capabilities, KEY_TIMES))));
        }

    }

    private Boolean isTimeseriesLayer(final OskariLayer layer) {
        JSONObject options = layer.getOptions();
        if(options != null && options.has("timeseries")) {
            JSONObject timeseriesOptions = options.optJSONObject("timeseries");
            if(timeseriesOptions != null && timeseriesOptions.has("ui")) {
                String ui = timeseriesOptions.optString("ui");
                if(ui != null && ui.equals("none")) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * @deprecated
     * use {@link LayerJSONFormatterWMS#createCapabilitiesJSON(WebMapService, Set)}
     */
    @Deprecated
    public static JSONObject createCapabilitiesJSON(final WebMapService wms) {
        return createCapabilitiesJSON(wms, null);
    }

    public static JSONObject createCapabilitiesJSON(final WebMapService wms,
            Set<String> systemCRSs) {
        JSONObject capabilities = new JSONObject();
        if(wms == null) {
            return capabilities;
        }
        JSONHelper.putValue(capabilities, KEY_ISQUERYABLE, wms.isQueryable());
        List<JSONObject> styles = createStylesArray(wms);
        JSONHelper.putValue(capabilities, KEY_STYLES, new JSONArray(styles));

        JSONObject formats = getFormatsJSON(wms);
        JSONHelper.putValue(capabilities, KEY_FORMATS, formats);
        JSONHelper.putValue(capabilities, KEY_VERSION, wms.getVersion());
        JSONHelper.putValue(capabilities, KEY_LAYER_COVERAGE, wms.getGeom());

        final Set<String> capabilitiesCRSs = getCRSs(wms);
        final Set<String> crss = getCRSsToStore(systemCRSs, capabilitiesCRSs);
        JSONHelper.putValue(capabilities, KEY_SRS, new JSONArray(crss));

        capabilities = JSONHelper.merge(capabilities, formatTime(wms.getTime()));
        return capabilities;
    }

    public static List<JSONObject> createStylesArray(final WebMapService capabilities) {
        final List<JSONObject> styles = new ArrayList<>();
        final Map<String, String> stylesMap = capabilities.getSupportedStyles();
        for (String styleName : stylesMap.keySet()) {
            String legend = capabilities.getLegendForStyle(styleName);
            styles.add(createStylesJSON(styleName, stylesMap.get(styleName), legend));
        }
        return styles;
    }

    private static JSONObject formatTime(List<String> times) throws IllegalArgumentException {
        if (times == null || times.isEmpty()) {
            return new JSONObject();
        }

        // TODO: Fix logic, currently we only support one TimeRange or one or more singular values
        // An interesting solution would be to "unroll" the time ranges to a list of singular
        // values -- then KEY_TIMES would always be an array of ISO timestamps as strings

        JSONObject wrapper = new JSONObject();

        // Check if the first value is a TimeRange
        String time = times.get(0);
        int i = time.indexOf('/');
        if (i < 0) {
            // Singular value(s)
            JSONHelper.put(wrapper, KEY_TIMES, new JSONArray(times));
        } else {
            // First one is potentially a TimeRange
            JSONHelper.putValue(wrapper, KEY_TIMES, parseTimeRange(time, i));
            if (times.size() > 1) {
                log.info("Handled only one (1) TimeRange out of", times.size());
            }
        }

        return wrapper;
    }

    private static JSONObject parseTimeRange(String time, int i)
            throws IllegalArgumentException {
        // TimeRange format: timeMin/timeMax/interval
        int j = time.indexOf('/', i + 1);
        if (j < 0 // Second slash doesn't exist
                || j == time.length() - 1 // Second slash is last char
                || time.indexOf('/', j + 1) > 0) { // Third slash exists
            throw new IllegalArgumentException("Invalid time range");
        }
        // Interval
        String start = time.substring(0, i);
        String end = time.substring(i + 1, j);
        String interval = time.substring(j + 1);

        // Instead of writing this as a range consider "unrolling"
        // the timeRange to a list of singular values
        JSONObject timeRange = new JSONObject();
        JSONHelper.putValue(timeRange, "start", start);
        JSONHelper.putValue(timeRange, "end", end);
        JSONHelper.putValue(timeRange, "interval", interval);
        return timeRange;
    }

    /**
     * Constructs a formats json containing the most preferred supported format
     *
     * @param wms WebMapService
     * @return JSONObject containing the most preferred supported format
     */
    public static JSONObject getFormatsJSON(WebMapService wms) {
        final Set<String> formats = new HashSet<String>(Arrays.asList(wms.getFormats()));
        return getFormatsJSON(formats);
    }



    /**
     * Constructs a unique set of coordinate ref systems supported by the WMS service
     *
     * @param wms WebMapService
     * @return Set<String> containing the supported coordinate ref systems of the WMS service
     */
    public static Set<String> getCRSs(WebMapService wms) {
        String[] crss = wms.getCRSs();
        if (crss == null || crss.length == 0) {
            return Collections.emptySet();
        }
        Set<String> set = new HashSet<>(crss.length);
        for (String crs : crss) {
            set.add(crs);
        }
        return set;
    }
}
