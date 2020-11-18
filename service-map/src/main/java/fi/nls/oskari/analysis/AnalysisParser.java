package fi.nls.oskari.analysis;

import fi.nls.oskari.domain.User;
import fi.nls.oskari.domain.map.OskariLayer;
import fi.nls.oskari.domain.map.wfs.WFSLayerAttributes;
import fi.nls.oskari.domain.map.wfs.WFSLayerCapabilities;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.map.analysis.domain.*;
import fi.nls.oskari.map.analysis.service.AnalysisDataService;
import fi.nls.oskari.map.analysis.service.TransformationService;
import fi.nls.oskari.map.layer.OskariLayerService;
import fi.nls.oskari.map.layer.OskariLayerServiceMybatisImpl;
import fi.nls.oskari.service.OskariComponentManager;
import fi.nls.oskari.service.ServiceException;
import fi.nls.oskari.service.ServiceRuntimeException;
import fi.nls.oskari.service.ServiceUnauthorizedException;
import fi.nls.oskari.util.ConversionHelper;
import fi.nls.oskari.util.JSONHelper;
import fi.nls.oskari.util.PropertyUtil;
import fi.nls.oskari.wfs.WFSFilterBuilder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;
import org.oskari.permissions.PermissionService;
import org.oskari.permissions.model.PermissionType;
import org.oskari.permissions.model.ResourceType;
import org.oskari.service.user.UserLayerService;

import java.util.*;


public class AnalysisParser {

    private static final Logger LOG = LogFactory
            .getLogger(AnalysisParser.class);
    private AnalysisDataService analysisDataService = new AnalysisDataService();
    private static final TransformationService transformationService = new TransformationService();

    private OskariLayerService mapLayerService = new OskariLayerServiceMybatisImpl();


    private static final List<String> HIDDEN_FIELDS = Arrays.asList("ID",
            "__fid", "metaDataProperty", "description", "boundedBy", "name",
            "location", "__centerX", "__centerY", "geometry", "geom", "the_geom", "created", "updated", "uuid");


    public static final String ANALYSIS_LAYER_PREFIX = "analysis_";
    public static final String MYPLACES_LAYER_PREFIX = "myplaces_";
    public static final String USERLAYER_PREFIX = "userlayer_";

    private static final Set<String> KNOWN_LAYER_PREFIXES = ConversionHelper.asSet(ANALYSIS_LAYER_PREFIX, MYPLACES_LAYER_PREFIX, USERLAYER_PREFIX);

    private static final String DEFAULT_OUTPUT_FORMAT = "text/xml; subtype=gml/3.1.1";
    private static final int DEFAULT_OPACITY = 80;

    private static final String FILTER_ID_TEMPLATE1 = "{\"filters\":[{\"caseSensitive\":false,\"attribute\":\"{propertyName}\",\"operator\":\"=\",\"value\":\"{propertyValue}\"}]}";
    private static final String FILTER_ID_TEMPLATE2 = "{\"caseSensitive\":false,\"attribute\":\"{propertyName}\",\"operator\":\"=\",\"value\":\"{propertyValue}\"}";
    private static final String FILTER_ID_TEMPLATE3 = "[{\"caseSensitive\":false,\"attribute\":\"{propertyName}\",\"operator\":\"=\",\"value\":\"{propertyValue}\"}]";

    private static final String ANALYSIS_INPUT_TYPE_WFS = "wfs";
    private static final String ANALYSIS_INPUT_TYPE_GS_VECTOR = "gs_vector";
    private static final String ANALYSIS_INPUT_TYPE_GEOJSON = "geojson";
    private static final String ANALYSIS_BASELAYER_ID = "analysis.baselayer.id";

    private static final String ANALYSIS_RENDERING_ELEMENT = "analysis.rendering.element";
    private static final String ANALYSIS_WPS_ELEMENT_LOCALNAME = "analysis_data";
    private static final String ANALYSIS_PROPERTY_NAME = "analysis_id";
    private static final String WPS_INPUT_TYPE = "input_type";

    private static final String MYPLACES_BASELAYER_ID = "myplaces.baselayer.id";
    private static final String MYPLACES_PROPERTY_NAME = "category_id";

    private static final String USERLAYER_BASELAYER_ID = "userlayer.baselayer.id";
    private static final String USERLAYER_PROPERTY_NAME = "user_layer_id";

    private static final String ANALYSIS_WFST_GEOMETRY = "feature:geometry>";
    private static final String ANALYSIS_WPS_UNION_GEOM = "gml:geom>";
    private static final String ANALYSIS_GML_PREFIX = "gml:";

    private static final String ANALYSIS_WFST_PREFIX = "feature:";

    private static final List<String> SPATIALJOIN_AGGREGATE_FIELDS = new ArrayList<String>(Arrays.asList(new String[]{"count","min","max","sum","avg","stddev"}));
    public static final String BUFFER = "buffer";
    public static final String INTERSECT = "intersect";
    public static final String SPATIAL_JOIN = "spatial_join";
    public static final String SPATIAL_JOIN_STATISTICS = "spatial_join_statistics";
    public static final String DIFFERENCE = "difference";
    public static final String AGGREGATE = "aggregate";
    public static final String UNION = "union";
    public static final String LAYER_UNION = "layer_union";
    public static final String ZONESECTOR = "areas_and_sectors";
    private static final String FUNC_NODATACOUNT = "NoDataCnt";
    private static final String FUNC_COUNT = "Count";
    private static final String DELTA_FIELD_NAME = "Muutos_t2-t1";
    private static final String JSONKEY_OVERRIDE_SLD = "override_sld";
    private static final String NUMERIC_FIELD_TYPE = "numeric";

    private static final String JSON_KEY_METHOD = "method";
    private static final String JSON_KEY_METHODPARAMS = "methodParams";
    private static final String JSON_KEY_LAYERID = "layerId";
    private static final String JSON_KEY_FUNCTIONS = "functions";
    private static final String JSON_KEY_FILTERS = "filters";
    private static final String JSON_KEY_LAYERS = "layers";
    private static final String JSON_KEY_FIELDTYPES = "fieldTypes";
    private static final String JSON_KEY_FEATURES = "features";
    private static final String JSON_KEY_NAME = "name";
    private static final String JSON_KEY_OPERATOR = "operator";
    private static final String JSON_KEY_DISTANCE = "distance";
    private static final String JSON_KEY_AREADISTANCE = "areaDistance";
    private static final String JSON_KEY_AREACOUNT = "areaCount";
    private static final String JSON_KEY_SECTORCOUNT = "sectorCount";
    private static final String JSON_KEY_NO_DATA = "no_data";
    private static final String JSON_KEY_LOCALE = "locale";

    final String analysisBaseLayerId = PropertyUtil.get(ANALYSIS_BASELAYER_ID);
    final String myplacesBaseLayerId = PropertyUtil.get(MYPLACES_BASELAYER_ID);
    final String userlayerBaseLayerId = PropertyUtil.get(USERLAYER_BASELAYER_ID);
    final String analysisRenderingUrl = AnalysisHelper.getAnalysisRenderingUrl(); //PropertyUtil.get(ANALYSIS_RENDERING_URL);
    final String analysisRenderingElement = PropertyUtil.get(ANALYSIS_RENDERING_ELEMENT);

    private PermissionService getPermissionService() {
        return OskariComponentManager.getComponentOfType(PermissionService.class);
    }

    /**
     * Parses method parameters to WPS execute xml syntax
     * definition
     *
     * @param json
     *            method parameters and layer info from the front
     * @param baseUrl
     *            Url for Geoserver WPS reference input (input
     *            FeatureCollection)
     * @param user
     *            User identification
     * @return AnalysisLayer parameters for WPS execution
     ************************************************************************/
    public AnalysisLayer parseAnalysisLayer(JSONObject json, String filter1, String filter2, String baseUrl, User user) throws ServiceException {

        // GeoJson input data
        final String geojson = getGeoJSONInput(json, json.optString(JSON_KEY_NAME,"feature"));
        AnalysisLayer analysisLayer = getLayerBasedOnInput(json, geojson, user);

        String analysisMethod = getAnalysisMethod(json);
        analysisLayer.setMethod(analysisMethod);

        // --- WFS layer is analysis input
        int id = analysisLayer.getId();
        if (id == -1) {
            throw new ServiceException("AnalysisInAnalysis parameters are invalid");
        }

        final OskariLayer wfsLayer = mapLayerService.find(id);
        if (wfsLayer == null) {
            throw new ServiceException("Input layer not found");
        }
        LOG.debug("got wfs layer", wfsLayer);

        analysisLayer.setMinScale(wfsLayer.getMinScale());
        analysisLayer.setMaxScale(wfsLayer.getMaxScale());

        // Set WFS input type, other than analysis_ and myplaces and geojson - default is REFERENCE
        WFSLayerAttributes attrs = new WFSLayerAttributes(wfsLayer.getAttributes());
        analysisLayer.setInputType(getWpsInputLayerType(attrs.getWpsParams(), analysisLayer.getInputType()));

        // Extract parameters for analysis methods from layer

        List<String> fields = getFields(json, analysisLayer.getFieldtypeMap());
        if (fields.isEmpty() && DIFFERENCE.equals(analysisMethod)) {
            throw new ServiceException(getRequiredErrorMsgFor("fields"));
        }
        analysisLayer.setFields(fields);
        analysisLayer.setAggreFunctions(null);
        analysisLayer.setMergeAnalysisLayers(null);

        JSONObject analyseMethodParams = json.optJSONObject(JSON_KEY_METHODPARAMS);
        if (analyseMethodParams == null) {
            throw new ServiceException(getRequiredErrorMsgFor(JSON_KEY_METHODPARAMS));
        }

        //------------------LAYER_UNION -----------------------
        if (LAYER_UNION.equals(analysisMethod)) {
            JSONArray layerIds = analyseMethodParams.optJSONArray(JSON_KEY_LAYERS);
            if (layerIds == null) {
                throw new ServiceException(getRequiredErrorMsgFor(JSON_KEY_LAYERS));
            }
            // only works for combining user content layers? Why?
            setupUnionAnalyseParams(layerIds, analysisLayer);
        }
        //------------------ BUFFER -----------------------
        else if (BUFFER.equals(analysisMethod)) {
            // when WPS method is vec:BufferFeatureCollection

            // Set params for WPS execute
            BufferMethodParams method = createBufferParams(analyseMethodParams, json.optJSONObject("bbox"));
            analysisLayer.setAnalysisMethodParams(method);

            parseCommonParams(wfsLayer, method, baseUrl);
            method.setGeojson(geojson);
            method.setWps_reference_type(analysisLayer.getInputType());

            // WFS filter
            method.setFilter(createWFSQueryFilter(wfsLayer, filter1, analysisLayer
                            .getInputAnalysisId(), analysisLayer.getInputCategoryId(), analysisLayer.getInputUserdataId()));
            // WFS Query properties
            method.setProperties(
                    createPartialWFSQueryForAttributes(analysisLayer.getFields(), "oskari", getGeometryField(wfsLayer)));
        }
        //------------------ ZONESECTOR ------------------------------------------
        else if (ZONESECTOR.equals(analysisMethod)) {
            // when WPS method is gs:ZoneSectorFeatureCollection

            // Set params for WPS execute

            ZoneSectorMethodParams method = this.parseZoneSectorParams(wfsLayer, json, geojson,
                    baseUrl);
            analysisLayer.setAnalysisMethodParams(method);

            method.setWps_reference_type(analysisLayer.getInputType());

            // Set override style
            analysisLayer.setOverride_sld(json.optString(JSONKEY_OVERRIDE_SLD));

            // WFS filter
            method.setFilter(
                    this.createWFSQueryFilter(wfsLayer, filter1, analysisLayer
                            .getInputAnalysisId(), analysisLayer.getInputCategoryId(), analysisLayer.getInputUserdataId()));
            // WFS Query properties
            method.setProperties(
                    this.createPartialWFSQueryForAttributes(analysisLayer.getFields(), "oskari", getGeometryField(wfsLayer)));
        }
        //------------------ INTERSECT -----------------------
        else if (INTERSECT.equals(analysisMethod)) {

            final String geojson2 = getGeoJSONInput(analyseMethodParams, json.optString(JSON_KEY_NAME, "feature"));
            final String sid = JSONHelper.getStringFromJSON(analyseMethodParams,JSON_KEY_LAYERID, "");
            final Boolean isJsonData = (geojson2 != null && !geojson2.isEmpty());
            OskariLayer lc2 = this.getWfsLayerConfiguration(sid, isJsonData );


            // Set params for WPS execute
            IntersectMethodParams method = this.parseIntersectParams(wfsLayer, lc2,
                    json, geojson, geojson2, baseUrl);
            analysisLayer.setAnalysisMethodParams(method);

            //TODO: better input type mapping
            method.setWps_reference_type(analysisLayer.getInputType());
            if (sid.indexOf(ANALYSIS_LAYER_PREFIX) == 0 || sid.indexOf(MYPLACES_LAYER_PREFIX) == 0 || sid.indexOf(USERLAYER_PREFIX) == 0) {
                method.setWps_reference_type2(ANALYSIS_INPUT_TYPE_GS_VECTOR);
                method.setLayer_id2(this.getAnalysisInputId(analyseMethodParams));
            } else if (isWpsInputLayerType(getWPSParams(lc2))) {
                method.setWps_reference_type2(ANALYSIS_INPUT_TYPE_GS_VECTOR);
            } else if (isJsonData) {
                method.setWps_reference_type2(ANALYSIS_INPUT_TYPE_GEOJSON);
            } else {
                method.setWps_reference_type2(ANALYSIS_INPUT_TYPE_WFS);
            }
            // Set WFS input type, other than analysis_ , myplaces_ and -userlayer - default is REFERENCE
            analysisLayer.setInputType(getWpsInputLayerType(getWPSParams(wfsLayer), analysisLayer.getInputType()));

            // Set mode intersect or contains
            method.setIntersection_mode(JSONHelper.getStringFromJSON(analyseMethodParams, JSON_KEY_OPERATOR, "intersect"));

            // WFS filter

            method.setFilter(this.createWFSQueryFilter(wfsLayer, filter1, analysisLayer
                    .getInputAnalysisId(), analysisLayer.getInputCategoryId(), analysisLayer.getInputUserdataId()));

            if (sid.indexOf(MYPLACES_LAYER_PREFIX) == 0) {
                method.setFilter2(this.createWFSQueryFilter(lc2, filter2, null, this
                        .getAnalysisInputId(analyseMethodParams), null));
            } else if (sid.indexOf(USERLAYER_PREFIX) == 0) {
                method.setFilter2(this.createWFSQueryFilter(lc2, filter2, null, null, this
                        .getAnalysisInputId(analyseMethodParams)));
            } else {
                method.setFilter2(this.createWFSQueryFilter(lc2, filter2, this
                        .getAnalysisInputId(analyseMethodParams), null, null));
            }
            // WFS Query properties
            method.setProperties(
                    this.createPartialWFSQueryForAttributes(analysisLayer.getFields(), "oskari", getGeometryField(wfsLayer)));

        }
        //------------------ SPATIAL_JOIN -----------------------
        else if (SPATIAL_JOIN.equals(analysisMethod)) {

            final String geojson2 = getGeoJSONInput(analyseMethodParams, json.optString(JSON_KEY_NAME, "feature"));
            final String sid = JSONHelper.getStringFromJSON(analyseMethodParams,JSON_KEY_LAYERID, "");
            final Boolean isJsonData = (geojson2 != null && !geojson2.isEmpty());
            OskariLayer lc2 = this.getWfsLayerConfiguration(sid, isJsonData );


            // Set params for WPS execute

            IntersectJoinMethodParams method = this.parseIntersectJoinParams(wfsLayer, lc2,
                    json, geojson, geojson2, baseUrl);
            analysisLayer.setAnalysisMethodParams(method);

            //TODO: better input type mapping
            method.setWps_reference_type(analysisLayer.getInputType());
            if (sid.indexOf(ANALYSIS_LAYER_PREFIX) == 0 || sid.indexOf(MYPLACES_LAYER_PREFIX) == 0 || sid.indexOf(USERLAYER_PREFIX) == 0) {
                method.setWps_reference_type2(ANALYSIS_INPUT_TYPE_GS_VECTOR);
                method.setLayer_id2(this.getAnalysisInputId(analyseMethodParams));
            } else if (isWpsInputLayerType(getWPSParams(lc2))) {
                method.setWps_reference_type2(ANALYSIS_INPUT_TYPE_GS_VECTOR);
            } else if (isJsonData) {
                method.setWps_reference_type2(ANALYSIS_INPUT_TYPE_GEOJSON);
            } else {
                method.setWps_reference_type2(ANALYSIS_INPUT_TYPE_WFS);
            }
            // Set WFS input type, other than analysis_ , myplaces_ and -userlayer - default is REFERENCE
            analysisLayer.setInputType(getWpsInputLayerType(getWPSParams(wfsLayer), analysisLayer.getInputType()));

            // Set mode intersect or contains
            method.setIntersection_mode(JSONHelper.getStringFromJSON(analyseMethodParams, JSON_KEY_OPERATOR, "intersect"));

            // WFS filter

            method.setFilter(this.createWFSQueryFilter(wfsLayer, filter1, analysisLayer
                    .getInputAnalysisId(), analysisLayer.getInputCategoryId(), analysisLayer.getInputUserdataId()));

            if (sid.indexOf(MYPLACES_LAYER_PREFIX) == 0) {
                method.setFilter2(this.createWFSQueryFilter(lc2, filter2, null, this
                        .getAnalysisInputId(analyseMethodParams), null));
            } else if (sid.indexOf(USERLAYER_PREFIX) == 0) {
                method.setFilter2(this.createWFSQueryFilter(lc2, filter2, null, null, this
                        .getAnalysisInputId(analyseMethodParams)));
            } else {
                method.setFilter2(this.createWFSQueryFilter(lc2, filter2, this
                        .getAnalysisInputId(analyseMethodParams), null, null));
            }
            // WFS Query properties
            method.setProperties(this.createPartialWFSQueryForAttributes(
                    analysisLayer.getFields(), "oskari", getGeometryField(wfsLayer)));

        }
        //------------------ SPATIAL_JOIN_STATISTICS (WPS method gs:VectorZonalStatistics) -----------------------
        else if (SPATIAL_JOIN_STATISTICS.equals(analysisMethod)) {

            final String geojson2 = getGeoJSONInput(analyseMethodParams, json.optString(JSON_KEY_NAME, "feature"));
            final String sid = JSONHelper.getStringFromJSON(analyseMethodParams,JSON_KEY_LAYERID, "");
            final Boolean isJsonData = (geojson2 != null && !geojson2.isEmpty());
            OskariLayer lc2 = this.getWfsLayerConfiguration(sid, isJsonData );


            // Set params for WPS execute

            SpatialJoinStatisticsMethodParams method = this.parseSpatialJoinStatisticsParams(wfsLayer, lc2,
                    json, geojson, geojson2, baseUrl);
            analysisLayer.setAnalysisMethodParams(method);

            //TODO: better input type mapping
            method.setWps_reference_type(analysisLayer.getInputType());
            if (sid.indexOf(ANALYSIS_LAYER_PREFIX) == 0 || sid.indexOf(MYPLACES_LAYER_PREFIX) == 0 || sid.indexOf(USERLAYER_PREFIX) == 0) {
                method.setWps_reference_type2(ANALYSIS_INPUT_TYPE_GS_VECTOR);
                method.setLayer_id2(this.getAnalysisInputId(analyseMethodParams));
            } else if (isWpsInputLayerType(getWPSParams(lc2))) {
                method.setWps_reference_type2(ANALYSIS_INPUT_TYPE_GS_VECTOR);
            } else if (isJsonData) {
                method.setWps_reference_type2(ANALYSIS_INPUT_TYPE_GEOJSON);
            } else {
                method.setWps_reference_type2(ANALYSIS_INPUT_TYPE_WFS);
            }
            // Set WFS input type, other than analysis_ , myplaces_ and -userlayer - default is REFERENCE
            analysisLayer.setInputType(getWpsInputLayerType(getWPSParams(wfsLayer), analysisLayer.getInputType()));


            // WFS filter

            method.setFilter(this.createWFSQueryFilter(wfsLayer, filter1, analysisLayer
                    .getInputAnalysisId(), analysisLayer.getInputCategoryId(), analysisLayer.getInputUserdataId()));

            if (sid.indexOf(MYPLACES_LAYER_PREFIX) == 0) {
                method.setFilter2(this.createWFSQueryFilter(lc2, filter2, null, this
                        .getAnalysisInputId(analyseMethodParams), null));
            } else if (sid.indexOf(USERLAYER_PREFIX) == 0) {
                method.setFilter2(this.createWFSQueryFilter(lc2, filter2, null, null, this
                        .getAnalysisInputId(analyseMethodParams)));
            } else {
                method.setFilter2(this.createWFSQueryFilter(lc2, filter2, this
                        .getAnalysisInputId(analyseMethodParams), null, null));
            }
            // WFS Query properties
            method.setProperties(
                    this.createPartialWFSQueryForAttributes(analysisLayer.getFields(), "oskari", getGeometryField(wfsLayer)));
        }
        //------------------ DIFFERENCE (WPS not used - result made by WFS 2.0 GetFeature) -----------------------
        else if (DIFFERENCE.equals(analysisMethod)) {

            final String geojson2 = getGeoJSONInput(analyseMethodParams, json.optString(JSON_KEY_NAME, "feature"));

            OskariLayer lc2 = null;
            int id2 = 0;
            String sid = "";
            try {
                sid = analyseMethodParams.getString(JSON_KEY_LAYERID);
                // Input is wfs layer or analaysis layer or geojson
                if (sid.indexOf(ANALYSIS_LAYER_PREFIX) == 0) {
                    // Analysislayer is input
                    // eg. analyse_216_340
                    id2 = ConversionHelper.getInt(analysisBaseLayerId, 0);

                } else if (sid.indexOf(MYPLACES_LAYER_PREFIX) == 0) {
                    // Myplaces is input
                    id2 = ConversionHelper.getInt(myplacesBaseLayerId, 0);
                } else if (sid.indexOf(USERLAYER_PREFIX) == 0) {
                    // user data layer is input
                    id2 = ConversionHelper.getInt(userlayerBaseLayerId, 0);
                } else if (geojson2 != null && !geojson2.isEmpty()) {
                    // GeoJson is input - use analysis base layer metadata
                    id2 = ConversionHelper.getInt(analysisBaseLayerId, 0);
                } else {
                    // Wfs layer id
                    id2 = ConversionHelper.getInt(sid, -1);
                }
            } catch (JSONException e) {
                throw new ServiceException(
                        "AnalysisInAnalysis parameters are invalid");
            }

            // Get wfs layer configuration for union input 2
            lc2 = mapLayerService.find(id2);

            // Set params for WPS execute

            DifferenceMethodParams method = this.parseDifferenceParams(wfsLayer, lc2,
                    json, geojson, geojson2, baseUrl);
            analysisLayer.setAnalysisMethodParams(method);

            // Layers must be under same wfs service
            method.setWps_reference_type(analysisLayer.getInputType());
            method.setWps_reference_type2(ANALYSIS_INPUT_TYPE_WFS);

            // New field types for difference data (string is default

            analysisLayer.getFieldtypeMap().put(DELTA_FIELD_NAME, NUMERIC_FIELD_TYPE);

            // Set override style
            analysisLayer.setOverride_sld(json.optString(JSONKEY_OVERRIDE_SLD));


            // WFS is BBOX filter in use
            method.setBbox(true);
        }
        //------------------ AGGREGATE -----------------------
        else if (AGGREGATE.equals(analysisMethod)) {

            // aggregate fields
            String aggre_field = null;
            // Is "Count" function in input parameters
            Boolean isCountFunc = false;
            analysisLayer.setNodataCount(false);

          /*      aggre_field = json.getJSONObject(JSON_KEY_METHODPARAMS)
                        .optString(JSON_KEY_AGGRE_ATTRIBUTE);
                if (analysisLayer.getInputType().equals(
                        ANALYSIS_INPUT_TYPE_GS_VECTOR))
                {
                    if(analysisLayer.getInputAnalysisId() != null)
                    {
                        aggre_field = analysisDataService
                                .SwitchField2AnalysisField(aggre_field,
                                        analysisLayer.getInputAnalysisId());
                    }
                } */
                aggre_field = fields.get(0);
                JSONArray aggre_func_in = analyseMethodParams.optJSONArray(JSON_KEY_FUNCTIONS);
                List<String> aggre_funcs = new ArrayList<>();
                if (aggre_func_in == null) {
                    throw new ServiceException(
                            "Aggregate functions missing.");
                } else {
                    try {
                        for (int i = 0; i < aggre_func_in.length(); i++) {
                            if(aggre_func_in.getString(i).equals(FUNC_NODATACOUNT)){
                                // Don't put NóDataCount to WPS aggregate, it is not in WPS
                                analysisLayer.setNodataCount(true);
                            }
                            else {
                                aggre_funcs.add(aggre_func_in.getString(i));
                            }
                            if(aggre_func_in.getString(i).equals(FUNC_COUNT)){
                                isCountFunc = true;
                            }
                        }
                    } catch (JSONException e) {
                        throw new ServiceException(
                                "Aggregate functions missing.");
                    }
                    analysisLayer.setAggreFunctions(aggre_funcs);


                }


            // Set params for WPS execute
            if (aggre_field == null) {
                throw new ServiceException(
                        "Aggregate field parameter missing.");
            }

            AggregateMethodParams method = this.parseAggregateParams(wfsLayer, json, geojson,
                        baseUrl, aggre_field, analysisLayer.getAggreFunctions());
            analysisLayer.setAnalysisMethodParams(method);

            method.setWps_reference_type(analysisLayer.getInputType());

            // Filter out text type fields, if there is no count-function computation requested
            if(!isCountFunc){
                this.removeTextTypeFields(analysisLayer);
            }
            // WFS filter

            method.setFilter(
                    this.createWFSQueryFilter(wfsLayer, filter1, analysisLayer
                            .getInputAnalysisId(), analysisLayer.getInputCategoryId(), analysisLayer.getInputUserdataId()));
            //------------------ UNION -----------------------
        } else if (UNION.equals(analysisMethod)) {

            // Set params for WPS execute

            UnionMethodParams method = this.parseUnionParams(wfsLayer, json, geojson, baseUrl);
            analysisLayer.setAnalysisMethodParams(method);

            method.setWps_reference_type(analysisLayer.getInputType());

            // WFS filter

            method.setFilter(this.createWFSQueryFilter(wfsLayer, filter1, analysisLayer
                    .getInputAnalysisId(), analysisLayer.getInputCategoryId(), analysisLayer.getInputUserdataId()));

        } else {
            throw new ServiceException("Method parameters missing.");
        }

        return analysisLayer;
    }

    private void setupUnionAnalyseParams(JSONArray layerIds, AnalysisLayer layer) throws ServiceException {
        if (layerIds == null) {
            throw new ServiceException(getRequiredErrorMsgFor(JSON_KEY_LAYERS));
        }
        // Loop merge layers - get analysis ids
        List<Long> ids = new ArrayList<>();
        List<String> mergelays = new ArrayList<>();
        for (int i = 0; i < layerIds.length(); i++) {
            String id = layerIds.optString(i);
            String userContentId = getUserContentAnalysisInputId(id);
            if (userContentId == null) {
                continue;
            }

            ids.add(ConversionHelper.getLong(userContentId, -1));
            mergelays.add(id);
        }

        // Merge analysis Ids
        layer.setMergeAnalysisIds(ids);
        // Merge analysis Layers
        layer.setMergeAnalysisLayers(mergelays);
    }

    /** Returns the final wps method id
     *  In certain cases requested method is changed here to an other wps method because of oskari analyse UI
     * @param json Analyse json send from oskari front
     * @return
     */
    private String getAnalysisMethod(JSONObject json) {
        String method = json.optString(JSON_KEY_METHOD);
        if(SPATIAL_JOIN.equals(method)){
            //If spatial join operator is aggregate, use SPATIAL_JOIN_STATISTICS method
            JSONObject params = json.optJSONObject(JSON_KEY_METHODPARAMS);
            if(params == null) {
                return method;
            }
            String operator = params.optString(JSON_KEY_OPERATOR);
            if (AGGREGATE.equals(operator)) {
                return SPATIAL_JOIN_STATISTICS;
            }
        }
        return method;
    }

    private String getRequiredErrorMsgFor(String field) {
        return "Required param missing '" + field + "'";
    }

    public String getSourceLayerId(JSONObject json) {
        if (json == null) {
            return null;
        }
        return json.optString(JSON_KEY_LAYERID);
    }

    private AnalysisLayer getLayerBasedOnInput(JSONObject json, String geojsonInput, User user) throws ServiceException {
        AnalysisLayer layer = new AnalysisLayer();

        String name = json.optString("name");
        if (name == null || name.isEmpty()) {
            throw new ServiceException(getRequiredErrorMsgFor("name"));
        }
        layer.setName(name);
        layer.setWpsUrl(analysisRenderingUrl);
        // analysis element name
        layer.setWpsName(analysisRenderingElement);
        // Analysis input property types
        layer.setFieldtypeMap(getFieldtypeMap(json));

        String style = json.optString("style");
        if (style == null || style.isEmpty()) {
            throw new ServiceException(getRequiredErrorMsgFor("style"));
        }
        layer.setStyle(style);

        int opacity = json.optInt("opacity");
        if (opacity == 0) {
            opacity = DEFAULT_OPACITY;
        }
        layer.setOpacity(opacity);

        // Input is wfs layer or analysis layer or my places or geojson
        layer.setInputAnalysisId(null);
        // check if we have geojson as input
        if (geojsonInput != null && !geojsonInput.isEmpty() ) {
            // GeoJson is input
            layer.setId(ConversionHelper.getInt(analysisBaseLayerId, 0));
            layer.setInputType(ANALYSIS_INPUT_TYPE_GEOJSON);
            return layer;
        }
        String layerId = getSourceLayerId(json);
        if (layerId == null) {
            throw new ServiceException(getRequiredErrorMsgFor(JSON_KEY_LAYERID));
        }

        String usercontentId = getUserContentAnalysisInputId(layerId);
        if (usercontentId == null) {
            // Normal WFS layer
            layer.setInputType(ANALYSIS_INPUT_TYPE_WFS);
            layer.setId(ConversionHelper.getInt(layerId, -1));
            if (!userHasPermission(user, layerId)) {
                throw new ServiceUnauthorizedException("User doesn't have permission to layer: " + layerId);
            }
            return layer;
        }
        // user content -> input type and id
        if(!userHasPermissionForUserContent(user, layerId)) {
            throw new ServiceUnauthorizedException("User doesn't have permission to layer: " + layerId);
        }

        layer.setInputType(ANALYSIS_INPUT_TYPE_GS_VECTOR);

        // check if we have analysis layer as input
        if (layerId.startsWith(ANALYSIS_LAYER_PREFIX)) {
            // Analysislayer is input
            layer.setId(ConversionHelper.getInt(analysisBaseLayerId, 0));
            // TODO: setInputAnalysisId(), setInputCategoryId(), setInputUserdataId() -> setUserContentId()
            layer.setInputAnalysisId(usercontentId);
            return layer;
        }

        if (layerId.startsWith(MYPLACES_LAYER_PREFIX)) {
            // myplaces is input
            layer.setId(ConversionHelper.getInt(myplacesBaseLayerId, 0));
            layer.setInputCategoryId(usercontentId);
            return layer;
        }
        if (layerId.startsWith(USERLAYER_PREFIX)) {
            // user data layer is input
            layer.setId(ConversionHelper.getInt(userlayerBaseLayerId, 0));
            layer.setInputUserdataId(usercontentId);
            return layer;
        }
        throw new ServiceException("Couldn't determine input for analysis");
    }

    protected boolean userHasPermission(User user, String layerId) {
        return getPermissionService().findResource(ResourceType.maplayer, layerId)
                .filter(r -> r.hasPermission(user, PermissionType.VIEW_LAYER)).isPresent();
    }

    protected boolean userHasPermissionForUserContent(User user, String layerId) {
        Map<String, UserLayerService> wfsHelpers =  OskariComponentManager.getComponentsOfType(UserLayerService.class);
        boolean hasPermissions = wfsHelpers.values()
                .stream()
                .filter(s -> s.isUserContentLayer(layerId))
                .findFirst()
                .map(s -> s.hasViewPermission(layerId, user))
                .orElse(false);
        return hasPermissions;
    }

    private List<String> getFields(JSONObject json, Map<String,String> fieldTypeMap) {
        JSONArray fields_in = getWfsInitFields(json, fieldTypeMap);
        List<String> returnValue = new ArrayList<>();
        if (fields_in == null) {
            return returnValue;
        }

        // Remove internal fields
        for (int i = 0; i < fields_in.length(); i++) {
            String field = fields_in.optString(i);
            if (field == null) {
                continue;
            }
            if (!HIDDEN_FIELDS.contains(field)) {
                returnValue.add(field);
            }
        }
        return returnValue;
    }

    /**
     * Get WFS service field names for case no fields
     * There should be one propertety in filter - in other case all properties are retreaved by WPS
     *
     * @param json
     *            analysis input data
     * @param map
     *            analysislayer fieldTypeMap
     *
     * @return field names
     */
    private JSONArray getWfsInitFields(JSONObject json, Map<String,String> map) {
        JSONArray fields_in = json.optJSONArray("fields");
        if (fields_in == null) {
            return null;
        }
        // Add one field of WFS service, if empty fields mode on
        // If no properties in filter --> return is all properties
        if (fields_in.length() != 0) {
            return fields_in;
        }
        // Special case
        final JSONObject params = JSONHelper.getJSONObject(json, JSON_KEY_METHODPARAMS);
        if(params != null && params.has("featuresA1")) {
            return JSONHelper.getJSONArray(params,"featuresA1");
        }

        JSONArray fields = new JSONArray();
        if (map == null) {
            return fields;
        }
        map.keySet().stream()
                .filter(key -> !HIDDEN_FIELDS.contains(key))
                .findFirst()
                .ifPresent(key -> fields.put(key));
        return fields;
    }

    private void setupBBox(AnalysisMethodParams params, final JSONObject bbox, boolean isRequired) throws ServiceException {
        if (bbox == null) {
            if (isRequired) {
                throw new ServiceException(getRequiredErrorMsgFor("bbox"));
            }
            return;
        }
        params.setX_lower(bbox.optString("left"));
        params.setY_lower(bbox.optString("bottom"));
        params.setX_upper(bbox.optString("right"));
        params.setY_upper(bbox.optString("top"));
    }
    /**
     * Parses BUFFER method parameters for WPS execute xml variables
     *
     * @param methodParams to get distance from
     * @return BufferMethodParams parameters for WPS execution
     ************************************************************************/
    private BufferMethodParams createBufferParams(JSONObject methodParams, JSONObject bbox) throws ServiceException {
        final BufferMethodParams method = new BufferMethodParams();
        method.setDistance(methodParams.optString(JSON_KEY_DISTANCE));

        setupBBox(method, bbox, true);
        if (method.getDistance() == null) {
            // it's possible to use negative buffer so just checking that you can copy a feature
            throw new ServiceException(getRequiredErrorMsgFor(JSON_KEY_DISTANCE));
        }

        return method;
    }
    /**
     * Parses ZONESECTOR method parameters for WPS execute xml variables
     *
     * @param lc
     *            WFS layer configuration
     * @param json
     *            Method parameters and layer info from the front
     * @param baseUrl
     *            Url for Geoserver WPS reference input (input
     *            FeatureCollection)
     * @return ZoneSectorMethodParams parameters for WPS execution
     ************************************************************************/
    private ZoneSectorMethodParams parseZoneSectorParams(OskariLayer lc,
                                                 JSONObject json, String geojson, String baseUrl) throws ServiceException {
        final ZoneSectorMethodParams method = new ZoneSectorMethodParams();
        //
        try {
            parseCommonParams(lc, method, baseUrl);

            final JSONObject params = json.getJSONObject(JSON_KEY_METHODPARAMS);
            setupBBox(method, json.optJSONObject("bbox"), true);

            method.setDistance(params.optString(JSON_KEY_AREADISTANCE));
            method.setZone_count(params.optString(JSON_KEY_AREACOUNT));
            method.setSector_count(params.optString(JSON_KEY_SECTORCOUNT));

            method.setGeojson(geojson);

        } catch (JSONException e) {
            throw new ServiceException("Method parameters missing.");
        }

        return method;
    }

    private void parseCommonParams(OskariLayer layer, AnalysisMethodParams params, String baseUrl) {
        params.setOutputFormat(DEFAULT_OUTPUT_FORMAT);
        params.setSrsName(getSRS(layer));
        if(layer == null) {
            return;
        }
        params.setLayer_id(layer.getId());
        params.setServiceUrl(layer.getUrl());
        params.setServiceUser(layer.getUsername());
        params.setServicePw(layer.getPassword());
        params.setHref(baseUrl + layer.getId());
        params.setTypeName(layer.getName());

        WFSLayerAttributes attrs = new WFSLayerAttributes(layer.getAttributes());
        params.setMaxFeatures(String.valueOf(attrs.getMaxFeatures()));
        params.setVersion(layer.getVersion());
        params.setXmlns("xmlns:" + getNamespacePrefix(layer) + "=\"" + getNamespaceURL(layer) + "\"");
        params.setGeom(getGeometryField(layer));
    }

    public String getNamespacePrefix(OskariLayer layer) {
        if (layer == null) {
            return "oskari";
        }
        String name = layer.getName();
        if (name == null || name.isEmpty()) {
            return getNamespacePrefix(null);
        }
        String[] split = name.split(":");
        if (split.length == 1) {
            return getNamespacePrefix(null);
        }
        return split[0];
    }
    /**
     * Parses AGGREGATE method parameters for WPS execute xml variables
     *
     * @param lc
     *            WFS layer configuration
     * @param json
     *            Method parameters and layer info from the front
     * @param baseUrl
     *            Url for Geoserver WPS reference input (input
     *@param aggre_field
     *            Field name for aggregate function
     * @return AggregateMethodParams parameters for WPS execution
     ************************************************************************/
    private AggregateMethodParams parseAggregateParams(
            OskariLayer lc, JSONObject json, String geojson, String baseUrl,
            String aggre_field, List<String> aggre_funcs)
            throws ServiceException {
        AggregateMethodParams method = new AggregateMethodParams();
        parseCommonParams(lc, method, baseUrl);
        method.setGeojson(geojson);
        final JSONObject params = json.optJSONObject(JSON_KEY_METHODPARAMS);
        method.setNoDataValue(params.optString(JSON_KEY_NO_DATA, null));
        setupBBox(method, json.optJSONObject("bbox"), true);

        // TODO: loop fields - current solution only for 1st field
        method.setAggreField1(aggre_field);
        method.setAggreFunctions(aggre_funcs);

        return method;
    }

    /**
     * Parses UNION method parameters for WPS execute xml variables
     * Originally vec:UnionFeatureCollection
     * Changed to geom union (gs:feature + subprocess gs:CollectGeometries
     *
     * @param lc
     *            WFS layer configuration
     * @param json
     *            Method parameters and layer info from the front
     * @param baseUrl
     *            Url for Geoserver WPS reference input (input
     *            FeatureCollection)
     * @return UnionMethodParams parameters for WPS execution
     ************************************************************************/
    private UnionMethodParams parseUnionParams(OskariLayer lc,
                                               JSONObject json, String geojson, String baseUrl) throws ServiceException {
        UnionMethodParams method = new UnionMethodParams();
        // General variable input and variable input of union input 1
        parseCommonParams(lc, method, baseUrl);
        method.setGeojson(geojson);
        setupBBox(method, json.optJSONObject("bbox"), true);
        return method;
    }

    /**
     * Parses INTERSECT method parameters for WPS execute xml variables
     *
     * @param lc
     *            WFS layer configuration
     * @param json
     *            Method parameters and layer info from the front
     * @param baseUrl
     *            Url for Geoserver WPS reference input (input
     *            FeatureCollection)
     * @return IntersectMethodParams parameters for WPS execution
     ************************************************************************/
    private IntersectMethodParams parseIntersectParams(
            OskariLayer lc, OskariLayer lc2,
            JSONObject json, String gjson, String gjson2, String baseUrl) throws ServiceException {
        IntersectMethodParams method = new IntersectMethodParams();

        try {

            parseMethodParams( method, lc, json, gjson, baseUrl);

            // Variable values of  input 2
            method.setHref2(baseUrl.replace("&", "&amp;") + String.valueOf(lc2.getId()));
            method.setTypeName2(lc2.getName());
            method.setXmlns2("xmlns:" + getNamespacePrefix(lc2) + "=\"" + getNamespaceURL(lc2) + "\"");
            method.setGeom2(getGeometryField(lc2));
            method.setGeojson2(gjson2);
        } catch (Exception e) {
            throw new ServiceException("Intersect analysis parameters missing.");
        }

        return method;
    }
    /**
     * Parses SPATIAL_JOIN method parameters for WPS execute xml variables
     *
     * @param lc
     *            WFS layer configuration
     * @param json
     *            Method parameters and layer info from the front
     * @param baseUrl
     *            Url for Geoserver WPS reference input (input
     *            FeatureCollection)
     * @return IntersectJoinMethodParams parameters for WPS execution
     ************************************************************************/
    private IntersectJoinMethodParams parseIntersectJoinParams(
            OskariLayer lc, OskariLayer lc2,
            JSONObject json, String gjson, String gjson2, String baseUrl) throws ServiceException {
        IntersectJoinMethodParams method = new IntersectJoinMethodParams();

        try {

            parseMethodParams( method, lc, json, gjson, baseUrl);
            // Variable values of  input 2
            method.setHref2(baseUrl.replace("&", "&amp;") + String.valueOf(lc2.getId()));
            method.setTypeName2(lc2.getName());
            method.setXmlns2("xmlns:" + getNamespacePrefix(lc2) + "=\"" + getNamespaceURL(lc2) + "\"");
            method.setGeom2(getGeometryField(lc2));
            method.setGeojson2(gjson2);



            // Intersect join retain columns
            final JSONObject params = json.getJSONObject(JSON_KEY_METHODPARAMS);
            // A layer
            method.setRetainfieldsA(params.getJSONArray("featuresB1").toString().replace("[","").replace("]","").replace("\"",""));
            // B layer
            method.setRetainfieldsB(params.getJSONArray("featuresA1").toString().replace("[", "").replace("]", "").replace("\"", ""));

        } catch (Exception e) {
            throw new ServiceException("Intersect join analysis parameters missing.");
        }

        return method;
    }
    /**
     * Parses SPATIAL_JOIN_STATISTICS method parameters for WPS execute xml variables
     *
     * @param lc
     *            1st WFS layer configuration (point layer)
     * @param lc2
     *            2nd WFS layer configuration (polygon layer)
     * @param json
     *            Method parameters and layer info from the front
     * @param gjson
     *            1st layer geometry, if input is geojson
     * @param gjson2
     *            2nd layer geometry, if input is geojson
     * @param baseUrl
     *            Url for Geoserver WPS reference input (input
     *            FeatureCollection)
     * @return IntersectJoinMethodParams parameters for WPS execution
     ************************************************************************/
    private  SpatialJoinStatisticsMethodParams  parseSpatialJoinStatisticsParams(
            OskariLayer lc, OskariLayer lc2,
            JSONObject json, String gjson, String gjson2, String baseUrl) throws ServiceException {
        SpatialJoinStatisticsMethodParams method = new SpatialJoinStatisticsMethodParams();

        try {

            parseMethodParams( method, lc, json, gjson, baseUrl);
            final JSONObject params = json.getJSONObject(JSON_KEY_METHODPARAMS);
            method.setNoDataValue(params.optString(JSON_KEY_NO_DATA, null));
            String geometryField = getGeometryField(lc2);
            // Variable values of  input 2
            method.setHref2(baseUrl.replace("&", "&amp;") + String.valueOf(lc2.getId()));
            method.setTypeName2(lc2.getName());
            method.setXmlns2("xmlns:" + getNamespacePrefix(lc2) + "=\"" + getNamespaceURL(lc2) + "\"");
            method.setGeom2(geometryField);
            method.setGeojson2(gjson2);



            // attribute field name of point layer for to which to compute statistics
            // A layer (point layer)
            String dataAttribute = (params.getJSONArray("featuresA1").toString().replace("[","").replace("]","").replace("\"",""));
            // Only one attribute is allowed and its type must be numeric
            method.setDataAttribute(dataAttribute.split(",")[0]);
            // 2nd layer properties to retain in analysis layer
            String [] layer2_properties = (params.getJSONArray("featuresB1").toString().replace("[","").replace("]","").replace("\"","")).split(",");
            // WFS Query properties
            method.setProperties2(this.createPartialWFSQueryForAttributes(
                    Arrays.asList(layer2_properties), "oskari", geometryField));

        } catch (Exception e) {
            throw new ServiceException("Spatial-join-statistics  analysis parameters missing.");
        }

        return method;
    }


    /**
     * Parses DIFFERENCE method parameters for WFS GetFeature xml template variables
     * TODO: There is a lot of unused source because of copy/paste
     * @param lc
     *            WFS layer configuration
     * @param json
     *            Method parameters and layer info from the front
     * @param baseUrl
     *            Url for Geoserver WFS-T (insert
     *            FeatureCollection)
     * @return DifferenceMethodParams parameters for WPS execution
     ************************************************************************/

    private DifferenceMethodParams parseDifferenceParams(
            OskariLayer lc, OskariLayer lc2,
            JSONObject json, String gjson, String gjson2, String baseUrl) throws ServiceException {
        DifferenceMethodParams method = new DifferenceMethodParams();
        // General variable input and variable input of union input 1
        parseCommonParams(lc, method, baseUrl);
        method.setGeojson(gjson);

        // Variable values of Union input 2
        baseUrl = baseUrl.replace("&", "&amp;");
        method.setHref2(baseUrl + String.valueOf(lc2.getId()));
        method.setTypeName2(lc2.getName());
        method.setXmlns2("xmlns:" + getNamespacePrefix(lc2) + "=\"" + getNamespaceURL(lc2) + "\"");

        method.setGeom2(getGeometryField(lc2));
        final JSONObject params = json.optJSONObject(JSON_KEY_METHODPARAMS);
        method.setNoDataValue(params.optString(JSON_KEY_NO_DATA, null));

        JSONObject bbox = json.optJSONObject("bbox");
        setupBBox(method, bbox, false);
        method.setBbox((bbox != null));

        // A layer field to compare
        method.setFieldA1(params.optString("fieldA1"));
        // B layer field to compare to A layer filed
        method.setFieldB1(params.optString("fieldB1"));
        // A layer key field to join
        method.setKeyA1(params.optString("keyA1"));
        // B layer key field to join
        method.setKeyB1(params.optString("keyB1"));
        // GML encode namespace prefix in result featureCollection
        method.setResponsePrefix("null");


        return method;
    }

    /**
     * Parses AGGREGATE results for Oskari front
     *
     * @param response
     *            WPS vec:aggregate execute results
     * @param analysisLayer
     *            analysis layer params (field/columns info)
     * @return JSON.toSting() eg. aggregate WPS results
     ************************************************************************/
    public String parseAggregateResults(String response,
                                        AnalysisLayer analysisLayer) {

        try {

            // convert xml/text String to JSON

            final JSONObject json = XML.toJSONObject(response); // all
            JSONObject aggreResult = new JSONObject();

            // Loop aggregate fields
            JSONArray results = json.optJSONArray("fieldResult");
            if (results != null) {
                for (int i = 0; i < results.length(); i++) {

                    JSONObject result = results.optJSONObject(i);
                    if (result != null) {
                        String fieldName = result.optString("field");

                        if (fieldName != null) {
                            if (analysisLayer.getInputAnalysisId() != null) {
                                fieldName = analysisDataService
                                        .SwitchField2OriginalField(fieldName,
                                                analysisLayer.getInputAnalysisId());
                            }
                            JSONObject aggreresult = result.optJSONObject("AggregationResults");
                            // If NoDataCount, append it to result
                            String noDataCount = result.optString("fieldNoDataCount", null);

                            if (noDataCount != null) {
                                if(aggreresult == null) aggreresult = new JSONObject();
                                aggreresult.put(FUNC_NODATACOUNT, this.getNoDataCount(noDataCount));
                            }

                            if(aggreresult != null) aggreResult.put(fieldName, aggreresult);
                        }
                    }
                }
            } else {
                JSONObject result = json.optJSONObject("fieldResult");
                if (result != null) {
                    String fieldName = result.optString("field");
                    if (fieldName != null) {
                        if (analysisLayer.getInputAnalysisId() != null) {
                            fieldName = analysisDataService
                                    .SwitchField2OriginalField(fieldName,
                                            analysisLayer.getInputAnalysisId());
                        }
                        // Special localisation for Aggregate result
                        //JSONObject locale_result = localeResult(result.optJSONObject("AggregationResults"), analysisLayer);
                        JSONObject aggreresult = result.optJSONObject("AggregationResults");
                        // If NoDataCount, append it to result
                        String noDataCount = result.optString("fieldNoDataCount", null);

                        if (noDataCount != null) {
                            if(aggreresult == null) aggreresult = new JSONObject();
                            aggreresult.put(FUNC_NODATACOUNT, this.getNoDataCount(noDataCount));
                        }

                        if(aggreresult != null) aggreResult.put(fieldName, aggreresult);
                    }
                }
            }

            return aggreResult.toString();

        } catch (JSONException e) {
            LOG.error(e, "XML to JSON failed", response);
        }

        return "{}";
    }

    /**
     * Parses WFS filter
     *
     * @param lc
     *            WFS layer configuration
     * @param filter
     *            WFS filter params
     * @param analysisId
     *            Analysis id when input is analysislayer, in other case null
     * @return String WFS filter xml
     * @throws fi.nls.oskari.service.ServiceException
     ************************************************************************/
    private String createWFSQueryFilter(OskariLayer lc, String filter,
                                        String analysisId, String categoryId, String userdataId) {

        JSONObject filter_js = null;
        try {
            if (filter == null) {
                String idfilter = null;
                if (analysisId != null) {
                    // Add analysis id filter when analysis in analysis
                    idfilter = FILTER_ID_TEMPLATE1.replace("{propertyName}", ANALYSIS_PROPERTY_NAME);
                    idfilter = idfilter.replace("{propertyValue}", analysisId);
                } else if (categoryId != null) {
                    // Add category id filter when myplaces in analysis
                    idfilter = FILTER_ID_TEMPLATE1.replace("{propertyName}", MYPLACES_PROPERTY_NAME);
                    idfilter = idfilter.replace("{propertyValue}", categoryId);
                } else if (userdataId != null) {
                    // Add user_data_layer id filter when user data layer in analysis
                    idfilter = FILTER_ID_TEMPLATE1.replace("{propertyName}", USERLAYER_PROPERTY_NAME);
                    idfilter = idfilter.replace("{propertyValue}", userdataId);
                }

                if (idfilter != null) filter_js = JSONHelper.createJSONObject(idfilter);

            } else {
                filter_js = JSONHelper.createJSONObject(filter);
                // Add analysis id filter when analysis in analysis
                if (filter_js.has(JSON_KEY_FILTERS)) {
                    String idfilter = null;
                    if (analysisId != null) {
                        // Add analysis id filter when analysis in analysis
                        idfilter = FILTER_ID_TEMPLATE2.replace("{propertyName}", ANALYSIS_PROPERTY_NAME);
                        idfilter = idfilter.replace("{propertyValue}", analysisId);
                    } else if (categoryId != null) {
                        // Add category id filter when myplaces in analysis
                        idfilter = FILTER_ID_TEMPLATE2.replace("{propertyName}", MYPLACES_PROPERTY_NAME);
                        idfilter = idfilter.replace("{propertyValue}", categoryId);
                    } else if (userdataId != null) {
                        // Add user_data_layer id filter when user data in analysis
                        idfilter = FILTER_ID_TEMPLATE2.replace("{propertyName}", USERLAYER_PROPERTY_NAME);
                        idfilter = idfilter.replace("{propertyValue}", userdataId);
                    }

                    if (idfilter != null) {
                        JSONObject analysis_id_filter = JSONHelper
                                .createJSONObject(idfilter);
                        filter_js.getJSONArray(JSON_KEY_FILTERS).put(
                                analysis_id_filter);
                    }

                } else {
                    String idfilter = null;
                    if (analysisId != null) {
                        // Add analysis id filter when analysis in analysis
                        idfilter = FILTER_ID_TEMPLATE3.replace("{propertyName}", ANALYSIS_PROPERTY_NAME);
                        idfilter = idfilter.replace("{propertyValue}", analysisId);
                    } else if (categoryId != null) {
                        // Add category id filter when myplaces in analysis
                        idfilter = FILTER_ID_TEMPLATE3.replace("{propertyName}", MYPLACES_PROPERTY_NAME);
                        idfilter = idfilter.replace("{propertyValue}", categoryId);
                    } else if (userdataId != null) {
                        // Add user_data_layer id filter when user data in analysis
                        idfilter = FILTER_ID_TEMPLATE3.replace("{propertyName}", USERLAYER_PROPERTY_NAME);
                        idfilter = idfilter.replace("{propertyValue}", userdataId);
                    }
                    if (idfilter != null) {
                        JSONArray idfilter_js = JSONHelper
                                .createJSONArray(idfilter);
                        filter_js.put(JSON_KEY_FILTERS, idfilter_js);
                    }

                }
            }
        } catch (JSONException e) {
            LOG.warn(e, "JSON parse failed");
        }

        // Build filter
        return WFSFilterBuilder.parseWfsFilter(filter_js,
                getSRS(lc), getGeometryField(lc));
    }

    private String getNamespaceURL(OskariLayer layer) {
        if (layer != null) {
            WFSLayerAttributes attr = new WFSLayerAttributes(layer.getAttributes());
            if (attr.getNamespaceURL() != null) {
                return attr.getNamespaceURL();
            }
        }
        LOG.info("Couldn't get namespace url from layer");
        return "http://oskari.org";
    }
    private String getWPSParams(OskariLayer layer) {
        if (layer != null) {
            WFSLayerAttributes attr = new WFSLayerAttributes(layer.getAttributes());
            return attr.getWpsParams();
        }
        return null;
    }

    private String getSRS(OskariLayer layer) {
        if (layer != null && layer.getSrs_name() != null) {
            return layer.getSrs_name();
        }
        LOG.info("Couldn't get srs from layer");
        // feature user has drawn
        return PropertyUtil.get("oskari.native.srs", "EPSG:3857");
    }

    private String getGeometryField(OskariLayer layer) {
        if (layer != null) {
            WFSLayerCapabilities caps = new WFSLayerCapabilities(layer.getCapabilities());
            if (caps.getGeometryAttribute() != null) {
                return caps.getGeometryAttribute();
            }
        }
        LOG.info("Couldn't get geometry name from layer");
        // feature user has drawn
        return "geometry";
    }

    private String createPartialWFSQueryForAttributes(List<String> props, String ns, String geom_prop) {
        try {
            return WFSFilterBuilder.parseProperties(props, ns, geom_prop);
        } catch (Exception e) {
            LOG.warn(e, "Properties parse failed");
        }
        return null;
    }


    /**
     * Use gs_vector input type, when wfs input layer is in the same server as WPS service
     * @param wps_params
     * @param defaultValue value to use if params don't suggest otherwise
     */
    private String getWpsInputLayerType(String wps_params, String defaultValue) {
        if (wps_params == null || wps_params.equals("{}")) {
            return defaultValue;
        }
        JSONObject json = JSONHelper.createJSONObject(wps_params);
        if (json == null) {
            return defaultValue;
        }
        if (ANALYSIS_INPUT_TYPE_GS_VECTOR.equals(json.optString(WPS_INPUT_TYPE))) {
            return ANALYSIS_INPUT_TYPE_GS_VECTOR;
        }
        return defaultValue;
    }
    /**
     * Is wfs layer gs_vector input type for WPS
     * @param wps_params  WFS layer configuration
     * @return true, if is
     */
    private boolean isWpsInputLayerType(String wps_params) {
        return ANALYSIS_INPUT_TYPE_GS_VECTOR.equals(getWpsInputLayerType(wps_params, null));
    }
    /**
     * Set analysis field types
     * @param json wps analysis parameters
     * @return false, if no id found
     */
    private Map<String, String> getFieldtypeMap(JSONObject json) {

        if (!json.has(JSON_KEY_FIELDTYPES)) {
            return Collections.emptyMap();
        }
        JSONObject ftypes = json.optJSONObject(JSON_KEY_FIELDTYPES);
        if (ftypes == null) {
            return Collections.emptyMap();
        }
        Map<String, String> typesMap = new HashMap<>();
        Iterator<?> keys = ftypes.keys();
        while (keys.hasNext()) {
            String key = (String) keys.next();
            typesMap.put(key, ftypes.optString(key));
        }
        return typesMap;
    }
    private String getAnalysisInputId(JSONObject json) {
        return getUserContentAnalysisInputId(json.optString(JSON_KEY_LAYERID));
    }
    /**
     * @param layerId
     *            wps analysis parameters
     * @return analysis id
     */
    protected static String getUserContentAnalysisInputId(String layerId) {
        if (layerId == null) {
            return null;
        }

        String prefixedId = KNOWN_LAYER_PREFIXES.stream()
                .filter(pre -> layerId.startsWith(pre))
                .findFirst()
                .orElse(null);
        if (prefixedId == null) {
            // layer id DIDN'T start with any of the known prefixes -> not a user content layer
            return null;
        }
        // split to get the actual id
        String sids[] = layerId.split("_");
        if (sids.length > 1) {
            // Old analysis is input for analysis or myplaces or user data layer
            // take the last part as there might be several ids referenced
            return sids[sids.length-1];
        }
        return null;
    }

    /**
     * Reform the featureset after WPS response for WFS-T
     * (fix prefixes, propertynames, etc)
     * @param featureSet
     * @param analysisLayer
     * @return
     */
    public String harmonizeElementNames(String featureSet, final AnalysisLayer analysisLayer) {
        try {
            final AnalysisMethodParams params = analysisLayer
                    .getAnalysisMethodParams();
            String[] enames = params.getTypeName().split(":");
            String ename = enames[0];
            if (enames.length > 1) {
                ename = enames[1];
            }
            String extraFrom = "gml:" + ename + "_";

            // Mixed perfixes to feature: prefix etc
            featureSet = featureSet.replace(extraFrom, ANALYSIS_WFST_PREFIX);

            extraFrom = ANALYSIS_GML_PREFIX + ename;
            String extraTo = ANALYSIS_WFST_PREFIX + ename;
            featureSet = featureSet.replace(extraFrom, extraTo);
            String[] geoms = params.getGeom().split(":");
            String geom = geoms[0];
            if (geoms.length > 1)
                geom = geoms[1];
            extraFrom = ANALYSIS_GML_PREFIX + geom + ">";
            featureSet = featureSet.replace(extraFrom, ANALYSIS_WFST_GEOMETRY);
            featureSet = featureSet.replace(ANALYSIS_WPS_UNION_GEOM,
                    ANALYSIS_WFST_GEOMETRY);
            featureSet = featureSet.replace(ANALYSIS_GML_PREFIX
                    + ANALYSIS_WPS_ELEMENT_LOCALNAME, ANALYSIS_WFST_PREFIX
                    + ANALYSIS_WPS_ELEMENT_LOCALNAME);
            featureSet = featureSet.replace(" NaN", "");
            featureSet = featureSet.replace("srsDimension=\"3\"","srsDimension=\"2\"");
        } catch (Exception e) {
            LOG.debug("Harmonizing element names failed: ", e);
        }
        return featureSet;
    }
    public AnalysisLayer parseSwitch2UnionLayer(AnalysisLayer analysisLayer, String analyse, String filter1,
                                               String filter2, String baseUrl, String outputFormat) {
        try {
            JSONObject json = JSONHelper.createJSONObject(analyse);
            // Switch to UNION method
            json.remove(JSON_KEY_METHOD);
            json.put(JSON_KEY_METHOD, "union");

            AnalysisLayer al2 = this.parseAnalysisLayer(json, filter1, filter2, baseUrl, null);
            // Set preused field definition
            al2.setFields(analysisLayer.getFields());
            // Aggregate results for to append to union result
            al2.setResult(analysisLayer.getResult());
            if(outputFormat != null) {
                ( (UnionMethodParams) al2.getAnalysisMethodParams()).setMimeTypeFormat(outputFormat);
            }
            return al2;
        } catch (Exception e) {
            LOG.debug("WPS method switch failed: ", e);
            return null;
        }

    }

    public String mergeAggregateResults2FeatureSet(String featureSet, AnalysisLayer analysisLayer, List<String> rowOrder, List<String> colOrder){
        try {
            // Add aggregate results to FeatureCollection ( only to one feature)
            featureSet = transformationService.mergePropertiesToFeatures(featureSet, analysisLayer.getResult(), rowOrder, colOrder);
        } catch (ServiceException e) {
            LOG.debug("Feature property insert to FeatureCollection failed: ", e);
        }
        return featureSet;
    }

    /**
     * Reorder rows (keys) and cols (values) of json object
     * JSONArrays are used for to keep the key order in json
     * @param jsaggregate input json
     * @param rowOrder  new order of 1st level keys (key names)
     * @param colOrder  new order of sub json keys  (key names)
     * @return  e.g.  [{"vaesto": [{"Kohteiden lukumäärä": "324"}, {"Tietosuojattujen kohteiden lukumäärä": "0"},..}]},{"miehet":[..
     */
    public JSONArray reorderAggregateResult(JSONObject jsaggregate, List<String> rowOrder, List<String> colOrder) {
        JSONArray jsona = new JSONArray();
        try {

            // Col order values
            Map<String, String> colvalues = new HashMap<String, String>();
            for (String col : colOrder) {
                colvalues.put(col, null);
            }
            for (int i = 0; i < rowOrder.size(); i++) {
                // Put properties to result featureset in predefined order
                String currow = i < rowOrder.size() ? rowOrder.get(i) : null;
                JSONArray subjsOrdereda = new JSONArray();
                Iterator<?> keys = jsaggregate.keys();

                while (keys.hasNext()) {
                    String key = (String) keys.next();
                    if (jsaggregate.get(key) instanceof JSONObject) {
                        if (currow != null && key.equals(currow)) {
                            JSONObject subjs = jsaggregate.getJSONObject(key);
                            Iterator<?> subkeys = subjs.keys();
                            while (subkeys.hasNext()) {
                                String subkey = (String) subkeys.next();
                                colvalues.put(subkey, subjs.get(subkey).toString());
                            }
                            for (int j = 0; j < colvalues.size(); j++) {
                                if(colvalues.get(colOrder.get(j)) != null) {
                                    JSONObject subjsOrdered = new JSONObject();
                                    JSONHelper.putValue(subjsOrdered, colOrder.get(j), colvalues.get(colOrder.get(j)));
                                    subjsOrdereda.put(subjsOrdered);
                                }
                            }
                        }
                    }
                }
                JSONObject json = new JSONObject();
                JSONHelper.putValue(json, rowOrder.get(i), subjsOrdereda);
                jsona.put(json);
            }
            return jsona;
        } catch (Exception e) {
            LOG.debug("Json resultset reordering failed: ", e);
        }
        return jsona;
    }

    protected static String getGeoJSONInput(JSONObject json, String id) {
        if (json == null || !json.has(JSON_KEY_FEATURES)) {
            return null;
        }
        JSONArray geofeas = json.optJSONArray(JSON_KEY_FEATURES);
        if (geofeas == null) {
            return null;
        }
        JSONArray response = new JSONArray();
        // Loop array
        for (int i = 0; i < geofeas.length(); i++) {
            JSONObject feature = geofeas.optJSONObject(i);
            if (feature == null) {
                continue;
            }
            JSONHelper.putValue(feature, "id", id + "." + i);
            feature.remove("crs");   // WPS töks, töks to crs
            response.put(feature);
        }
        // GeoServer expects an object with "features" key containing an array of features.
        JSONObject collection = new JSONObject();
        JSONHelper.putValue(collection, "type", "FeatureCollection");
        JSONHelper.putValue(collection, JSON_KEY_FEATURES, response);
        return JSONHelper.getStringFromJSON(collection, null);
    }

    /**
     * Get Count value
     * @param noDataCount  ({"AggregationResults":{"Count":10}})
     * @return  Count value
     */
    private int getNoDataCount(final String noDataCount) {
        if (noDataCount == null) {
            return 0;
        }
        JSONObject countResult = JSONHelper.createJSONObject(noDataCount);
        if (countResult == null) {
            return 0;
        }
        JSONObject result = countResult.optJSONObject("AggregationResults");
        if (result == null) {
            return 0;
        }
        return result.optInt("Count",0);
    }

    /**
     * Get WFS-layer configuration
     * @param sid {String} layer id
     * @param isData  {Boolean} input is geojson data, if true
     * @return
     */
    private OskariLayer getWfsLayerConfiguration(final String sid, final Boolean isData) {
        int id2 = 0;
        try {
            // Input is wfs layer or analaysis layer or geojson
            if (sid.indexOf(ANALYSIS_LAYER_PREFIX) == 0) {
                // Analysislayer is input
                // eg. analyse_216_340
                id2 = ConversionHelper.getInt(analysisBaseLayerId, 0);

            } else if (sid.indexOf(MYPLACES_LAYER_PREFIX) == 0) {
                // Myplaces is input
                id2 = ConversionHelper.getInt(myplacesBaseLayerId, 0);
            } else if (sid.indexOf(USERLAYER_PREFIX) == 0) {
                // user data layer is input
                id2 = ConversionHelper.getInt(userlayerBaseLayerId, 0);
            } else if (isData) {
                // GeoJson is input - use analysis base layer metadata
                id2 = ConversionHelper.getInt(analysisBaseLayerId, 0);
            } else {
                // Wfs layer id
                id2 = ConversionHelper.getInt(sid, -1);
            }
        } catch (Exception e) {
           return null;
        }

        // Get wfs layer configuration for union input 2
        return mapLayerService.find(id2);
    }
    private void parseMethodParams( AnalysisMethodParams method, OskariLayer lc, JSONObject json, String gjson, String baseUrl) {
        parseCommonParams(lc, method, baseUrl);
        method.setGeojson(gjson);
        try {
            setupBBox(method, json.optJSONObject("bbox"), false);
        } catch (ServiceException ignored) {
            // bbox is optional so exception is never thrown
        }
    }

    /**
     * Manage new fieldnames names and types generated by wps for analysis storage
     * - WPS method might generate new fields for the features in WPS resultset
     * @param analysisLayer
     */
    public void fixTypeNames(AnalysisLayer analysisLayer, JSONObject analysejs) {
        AnalysisMethodParams params = analysisLayer.getAnalysisMethodParams();
        if (DIFFERENCE.equals(params.getMethod())) {
            // For time being 1st numeric value is used for rendering
            Map<String, String> fieldTypes = new HashMap<String, String>();
            fieldTypes.put(AnalysisParser.DELTA_FIELD_NAME, NUMERIC_FIELD_TYPE);
            analysisLayer.setFieldtypeMap(fieldTypes);
            return;
        }

        if (!SPATIAL_JOIN_STATISTICS.equals(params.getMethod())) {
            return;
        }
        if (!(params instanceof SpatialJoinStatisticsMethodParams)) {
            throw new ServiceRuntimeException("Got the wrong type of params (expected SpatialJoinStatisticsMethodParams)");
        }
        SpatialJoinStatisticsMethodParams spparams = (SpatialJoinStatisticsMethodParams) params;
        spparams.setLocalemap(constructLocale(analysejs.optJSONObject(JSON_KEY_METHODPARAMS)));

        Map<String, String> fieldTypes = new HashMap<>(SPATIALJOIN_AGGREGATE_FIELDS.size());
        SPATIALJOIN_AGGREGATE_FIELDS.stream()
                .forEach(field -> fieldTypes.put(field, NUMERIC_FIELD_TYPE));
        Map<String, String> existingTypes = analysisLayer.getFieldtypeMap();
        if (existingTypes == null) {
            existingTypes = fieldTypes;
        } else {
            existingTypes.putAll(fieldTypes);
        }

        // TODO: is this correct? build fieldsMap based on fieldTypeMap?
        analysisLayer.setFieldsMap(existingTypes);
    }

    private Map<String, String> constructLocale(JSONObject methodParams) throws ServiceRuntimeException {
        Map<String, String> localemap = new HashMap<>();
        JSONArray locales = methodParams.optJSONArray(JSON_KEY_LOCALE);
        if (locales == null) {
            return localemap;
        }
        try {
            for (int i = 0; i < locales.length(); i++) {
                JSONObject locale = locales.getJSONObject(i);
                String id = locale.getString("id");
                String label = locale.getString("label");
                SPATIALJOIN_AGGREGATE_FIELDS.stream()
                        .filter(field -> id.toLowerCase().indexOf(field) > -1)
                        .forEach(field -> localemap.put(field, label));
            }
            return localemap;
        } catch (JSONException e) {
            throw new ServiceRuntimeException("Unable to parse locale from method params", e);
        }
    }
    /**
     * Removes text type fields out of input parameters
     * @param analysisLayer
     */
    public void removeTextTypeFields(AnalysisLayer analysisLayer) {
        List<String> fields = analysisLayer.getFields();
        Map<String,String> fieldTypes = analysisLayer.getFieldtypeMap();
        if (fields == null || fieldTypes  == null ) {
            // nothing to do
            return;
        }

        try {
            List<String> newfields = new ArrayList<>();
            for (int i = 0; i < fields.size(); i++) {
                String fieldType = fieldTypes.get(fields.get(i));
                if (NUMERIC_FIELD_TYPE.equals(fieldType)) {
                    newfields.add(fields.get(i));
                }
            }
            analysisLayer.setFields(newfields);
        } catch (Exception e) {
            LOG.warn("Remove text type input fields  failed ", e);
        }
    }
}
