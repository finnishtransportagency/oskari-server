package fi.nls.oskari.control.layer;

import fi.mml.map.mapwindow.service.wms.LayerNotFoundInCapabilitiesException;
import fi.mml.map.mapwindow.service.wms.WebMapService;
import fi.mml.map.mapwindow.service.wms.WebMapServiceParseException;
import fi.mml.map.mapwindow.util.OskariLayerWorker;
import fi.nls.oskari.annotation.OskariActionRoute;
import fi.nls.oskari.cache.JedisManager;
import fi.nls.oskari.control.*;
import fi.nls.oskari.domain.map.DataProvider;
import fi.nls.oskari.domain.map.OskariLayer;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.map.layer.DataProviderService;
import fi.nls.oskari.map.layer.OskariLayerService;
import fi.nls.oskari.map.layer.group.link.OskariLayerGroupLink;
import fi.nls.oskari.map.layer.group.link.OskariLayerGroupLinkService;
import fi.nls.oskari.map.view.ViewService;
import fi.nls.oskari.map.view.util.ViewHelper;
import fi.nls.oskari.service.ServiceException;
import fi.nls.oskari.service.capabilities.CapabilitiesCacheService;
import fi.nls.oskari.service.capabilities.OskariLayerCapabilitiesHelper;
import fi.nls.oskari.util.*;
import org.oskari.admin.LayerCapabilitiesHelper;
import org.oskari.log.AuditLog;
import fi.nls.oskari.wfs.GetGtWFSCapabilities;
import fi.nls.oskari.wmts.WMTSCapabilitiesParser;
import fi.nls.oskari.wmts.domain.WMTSCapabilities;
import org.json.JSONArray;
import org.json.JSONObject;
import org.oskari.permissions.model.*;
import org.oskari.service.util.ServiceFactory;
import org.oskari.service.wfs3.WFS3Service;

import javax.servlet.http.HttpServletRequest;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

import static fi.nls.oskari.control.ActionConstants.PARAM_SRS;

/**
 * Admin insert/update of WMS map layer
 */
@OskariActionRoute("SaveLayer")
public class SaveLayerHandler extends AbstractLayerAdminHandler {

    private class SaveResult {
        long layerId = -1;
        boolean capabilitiesUpdated = false;
    }

    private OskariLayerService mapLayerService = ServiceFactory.getMapLayerService();
    private ViewService viewService = ServiceFactory.getViewService();
    private DataProviderService dataProviderService = ServiceFactory.getDataProviderService();
    private OskariLayerGroupLinkService layerGroupLinkService = ServiceFactory.getOskariLayerGroupLinkService();
    private CapabilitiesCacheService capabilitiesService = ServiceFactory.getCapabilitiesCacheService();
    private static final Logger LOG = LogFactory.getLogger(SaveLayerHandler.class);

    private static final String PARAM_LAYER_ID = "layer_id";
    private static final String PARAM_LAYER_NAME = "layerName";
    private static final String PARAM_LAYER_URL = "layerUrl";
    private static final String PARAM_SRS_NAME = "srs_name";
    private static final String PARAM_MAPLAYER_GROUPS = "maplayerGroups";
    private static final String PARAM_VIEW_PERMISSIONS = "viewPermissions";
    private static final String PARAM_PUBLISH_PERMISSIONS = "publishPermissions";
    private static final String PARAM_DOWNLOAD_PERMISSIONS = "downloadPermissions";
    private static final String PARAM_EMBEDDED_PERMISSIONS = "embeddedPermissions";
    private static final String PARAM_LAYER_TYPE ="layerType";
    private static final String PARAM_PARENT_ID ="parentId";
    private static final String PARAM_GROUP_ID ="groupId";
    private static final String PARAM_VERSION ="version";
    private static final String PARAM_IS_BASE ="isBase";
    private static final String PARAM_OPACITY ="opacity";
    private static final String PARAM_STYLE ="style";
    private static final String PARAM_MIN_SCALE ="minScale";
    private static final String PARAM_MAX_SCALE ="maxScale";
    private static final String PARAM_LEGEND_IMAGE ="legendImage";
    private static final String PARAM_METADATA_ID ="metadataId";
    private static final String PARAM_GFI_CONTENT ="gfiContent";
    private static final String PARAM_USERNAME ="username";
    private static final String PARAM_PASSWORD ="password";
    private static final String PARAM_CAPABILITIES_UPDATE_RATE_SEC ="capabilitiesUpdateRateSec";
    private static final String PARAM_ATTRIBUTES ="attributes";
    private static final String PARAM_PARAMS ="params";
    private static final String PARAM_OPTIONS ="options";
    private static final String PARAM_REALTIME ="realtime";
    private static final String PARAM_REFRESH_RATE ="refreshRate";
    private static final String PARAM_GML2_SEPARATOR = "GML2Separator";
    private static final String PARAM_GML_GEOMETRY_PROPERTY = "GMLGeometryProperty";
    private static final String PARAM_GML_VERSION = "GMLVersion";
    private static final String PARAM_WFS_VERSION = "WFSVersion";
    private static final String PARAM_FEATURE_ELEMENT = "featureElement";
    private static final String PARAM_FEATURE_NAMESPACE = "featureNamespace";
    private static final String PARAM_FEATURE_NAMESCAPE_URI = "featureNamespaceURI";
    private static final String PARAM_FEATURE_PARAMS_LOCALES = "featureParamsLocales";
    private static final String PARAM_FEATURE_TYPE = "featureType";
    private static final String PARAM_GEOMETRY_NAMESPACE_URI = "geometryNamespaceURI";
    private static final String PARAM_GEOMETRY_TYPE = "geometryType";
    private static final String PARAM_GET_FEATURE_INFO = "getFeatureInfo";
    private static final String PARAM_GET_HIGHLIGHT_IMAGE = "getHighlightImage";
    private static final String PARAM_GET_MAP_TILES = "getMapTiles";
    private static final String PARAM_MAX_FEATURES = "maxFeatures";
    private static final String PARAM_OUTPUT_FORMAT = "outputFormat";
    private static final String PARAM_SELECTED_FEATURE_PARAMS = "selectedFeatureParams";
    private static final String PARAM_TILE_BUFFER = "tileBuffer";
    private static final String PARAM_TILE_REQUEST = "tileRequest";
    private static final String PARAM_JOB_TYPE = "jobType";
    private static final String PARAM_REQUEST_TEMPLATE = "requestTemplate";
    private static final String PARAM_RESPONSE_TEMPLATE = "responseTemplate";
    private static final String PARAM_PARSE_CONFIG = "parseConfig";
    private static final String PARAM_TEMPLATE_NAME = "templateName";
    private static final String PARAM_TEMPLATE_TYPE = "templateType";
    private static final String PARAM_STYLE_SELECTION = "styleSelection";
    private static final String PARAM_XSLT = "xslt";
    private static final String PARAM_GFI_TYPE = "gfiType";
    private static final String PARAM_MANUAL_REFRESH = "manualRefresh";
    private static final String PARAM_RESOLVE_DEPTH = "resolveDepth";

    private static final String LAYER_NAME_PREFIX = "name_";
    private static final String LAYER_TITLE_PREFIX = "title_";

    private static final String ERROR_UPDATE_OR_INSERT_FAILED = "update_or_insert_failed";
    private static final String ERROR_NO_LAYER_WITH_ID = "no_layer_with_id:";
    private static final String ERROR_OPERATION_NOT_PERMITTED = "operation_not_permitted_for_layer_id:";
    private static final String ERROR_MANDATORY_FIELD_MISSING = "mandatory_field_missing:";
    private static final String ERROR_INVALID_FIELD_VALUE = "invalid_field_value:";
    private static final String ERROR_FE_PARSER_CONFIG_MISSING = "FE WFS feature parser config missing";

    private static final String OSKARI_FEATURE_ENGINE = "oskari-feature-engine";
    private static final String WFS1_1_0_VERSION = "1.1.0";
    private static final String WFS3_0_0_VERSION = "3.0.0";

    @Override
    public void handlePost(ActionParameters params) throws ActionException {

        final SaveResult result = saveLayer(params);
        final int layerId = (int)result.layerId;
        final OskariLayer ml = mapLayerService.find(layerId);
        if(ml == null) {
            throw new ActionParamsException("Couldn't get the saved layer from DB - id:" + layerId);
        }

        // construct response as layer json
        final JSONObject layerJSON = OskariLayerWorker.getMapLayerJSON(ml, params.getUser(), params.getLocale().getLanguage(), params.getHttpParam(PARAM_SRS));
        if (layerJSON == null) {
            // handle error getting JSON failed
            throw new ActionException("Error constructing JSON for layer");
        }
        if(!result.capabilitiesUpdated) {
            // Cache update failed, no biggie
            JSONHelper.putValue(layerJSON, "warn", "metadataReadFailure");
            LOG.debug("Metadata read failure");
        }
        // Also add groupId

        List<OskariLayerGroupLink> groupLinks = layerGroupLinkService.findByLayerId(layerId);
        JSONArray groups = new JSONArray();
        for (OskariLayerGroupLink oskariLayerGroupLink:groupLinks) {
            groups.put(oskariLayerGroupLink.getGroupId());
        }
        JSONHelper.putValue(layerJSON, "groups", groups);
        ResponseHelper.writeResponse(params, layerJSON);
    }

    private SaveResult saveLayer(final ActionParameters params) throws ActionException {

        // layer_id can be string -> external id!
        final int layer_id = params.getHttpParam(PARAM_LAYER_ID, -1);
        SaveResult result = new SaveResult();

        try {
            // ************** UPDATE ************************
            if (layer_id != -1) {

                final OskariLayer ml = mapLayerService.find(layer_id);
                if (ml == null) {
                    // layer wasn't found
                    throw new ActionParamsException(ERROR_NO_LAYER_WITH_ID + layer_id);
                }

                if (!userHasEditPermission(params.getUser(), ml)) {
                    throw new ActionDeniedException(ERROR_OPERATION_NOT_PERMITTED + layer_id);
                }

                result.capabilitiesUpdated = handleRequestToMapLayer(params, ml);

                ml.setUpdated(new Date(System.currentTimeMillis()));
                mapLayerService.update(ml);

                AuditLog.user(params.getClientIp(), params.getUser())
                        .withParam("id", ml.getId())
                        .withParam("uiName", ml.getName(PropertyUtil.getDefaultLanguage()))
                        .withParam("url", ml.getUrl())
                        .withParam("name", ml.getName())
                        .updated(AuditLog.ResourceType.MAPLAYER);

                String maplayerGroups = params.getHttpParam(PARAM_MAPLAYER_GROUPS);
                if (maplayerGroups != null) {
                    int[] groupIds = getMaplayerGroupIds(maplayerGroups);
                    List<OskariLayerGroupLink> links = getMaplayerGroupLinks(ml.getId(), groupIds);
                    layerGroupLinkService.deleteLinksByLayerId(ml.getId());
                    layerGroupLinkService.insertAll(links);
                }

                LOG.debug(ml);
                result.layerId = ml.getId();
                return result;
            }

            // ************** INSERT ************************
            else {

                if (!userHasAddPermission(params.getUser())) {
                    throw new ActionDeniedException(ERROR_OPERATION_NOT_PERMITTED + layer_id);
                }

                final OskariLayer ml = new OskariLayer();
                final Date currentDate = new Date(System.currentTimeMillis());
                ml.setCreated(currentDate);
                ml.setUpdated(currentDate);
                result.capabilitiesUpdated = handleRequestToMapLayer(params, ml);

                int id = mapLayerService.insert(ml);
                ml.setId(id);

                AuditLog.user(params.getClientIp(), params.getUser())
                        .withParam("id", ml.getId())
                        .withParam("uiName", ml.getName(PropertyUtil.getDefaultLanguage()))
                        .withParam("url", ml.getUrl())
                        .withParam("name", ml.getName())
                        .added(AuditLog.ResourceType.MAPLAYER);

                String maplayerGroups = params.getHttpParam(PARAM_MAPLAYER_GROUPS);
                if (maplayerGroups != null && !maplayerGroups.isEmpty()) {
                    int[] groupIds = getMaplayerGroupIds(maplayerGroups);
                    List<OskariLayerGroupLink> links = getMaplayerGroupLinks(ml.getId(), groupIds);
                    layerGroupLinkService.insertAll(links);
                }

                if(ml.isCollection()) {
                    // update the name with the id for permission mapping
                    ml.setName(ml.getId() + "_group");
                    mapLayerService.update(ml);
                }

                addPermissionsForRoles(ml,
                        getPermissionSet(params.getHttpParam(PARAM_VIEW_PERMISSIONS)),
                        getPermissionSet(params.getHttpParam(PARAM_PUBLISH_PERMISSIONS)),
                        getPermissionSet(params.getHttpParam(PARAM_DOWNLOAD_PERMISSIONS)),
                        getPermissionSet(params.getHttpParam(PARAM_EMBEDDED_PERMISSIONS)));

                // update keywords
                GetLayerKeywords glk = new GetLayerKeywords();
                glk.updateLayerKeywords(id, ml.getMetadataId());

                result.layerId = ml.getId();
                return result;
            }

        } catch (Exception e) {
            if (e instanceof ActionException) {
                throw (ActionException) e;
            } else {
                throw new ActionException(ERROR_UPDATE_OR_INSERT_FAILED, e);
            }
        }
    }

    private static int[] getMaplayerGroupIds(String maplayerGroups) {
        return Arrays.stream(maplayerGroups.split(","))
                .mapToInt(gid -> ConversionHelper.getInt(gid, -1))
                .filter(gid -> gid >= 0)
                .toArray();
    }

    private List<OskariLayerGroupLink> getMaplayerGroupLinks(final int layerId, final int[] groupIds) {
        if (groupIds.length == 0) {
            return Collections.emptyList();
        }
        return Arrays.stream(groupIds)
                .mapToObj(groupId -> new OskariLayerGroupLink(layerId, groupId))
                .collect(Collectors.toList());
    }

    /**
     * Treats the param as comma-separated list. Splits to individual values and
     * returns a set of values that could be converted to Long
     * @param param
     * @return
     */
    private Set<Integer> getPermissionSet(final String param) {
        if(param == null) {
            return Collections.emptySet();
        }
        final Set<Integer> set = new HashSet<>();
        final String[] roleIds = param.split(",");
        for (String externalId : roleIds) {
            final int extId = ConversionHelper.getInt(externalId, -1);
            if (extId != -1) {
                set.add(extId);
            }
        }
        return set;
    }

    private boolean handleRequestToMapLayer(final ActionParameters params, OskariLayer ml) throws ActionException {

        HttpServletRequest request = params.getRequest();

        if(ml.getId() == -1) {
            // setup type and parent for new layers only
            ml.setType(params.getHttpParam(PARAM_LAYER_TYPE));
            ml.setParentId(params.getHttpParam(PARAM_PARENT_ID, -1));
        }

        // organization id
        final DataProvider dataProvider = dataProviderService.find(params.getHttpParam(PARAM_GROUP_ID, -1));
        ml.addDataprovider(dataProvider);

        // get names and descriptions
        final Enumeration<String> paramNames = request.getParameterNames();
        while (paramNames.hasMoreElements()) {
            String paramName = paramNames.nextElement();
            if (paramName.startsWith(LAYER_NAME_PREFIX)) {
                String lang = paramName.substring(LAYER_NAME_PREFIX.length()).toLowerCase();
                String name = params.getHttpParam(paramName);
                ml.setName(lang, name);
            } else if (paramName.startsWith(LAYER_TITLE_PREFIX)) {
                String lang = paramName.substring(LAYER_TITLE_PREFIX.length()).toLowerCase();
                String title = params.getHttpParam(paramName);
                ml.setTitle(lang, title);
            }
        }

        ml.setVersion(params.getHttpParam(PARAM_VERSION, ""));
        ml.setBaseMap(ConversionHelper.getBoolean(params.getHttpParam(PARAM_IS_BASE), false));

        if(ml.isCollection()) {
            // ulr is needed for permission mapping, name is updated after we get the layer id
            ml.setUrl(ml.getType());
            // the rest is not relevant for collection layers
            return true;
        }

        ml.setName(params.getRequiredParam(PARAM_LAYER_NAME, ERROR_MANDATORY_FIELD_MISSING + PARAM_LAYER_NAME));
        final String url = params.getRequiredParam(PARAM_LAYER_URL, ERROR_MANDATORY_FIELD_MISSING + PARAM_LAYER_URL);
        ml.setUrl(url);
        validateUrl(ml.getSimplifiedUrl(true));

        ml.setOpacity(params.getHttpParam(PARAM_OPACITY, ml.getOpacity()));
        ml.setStyle(params.getHttpParam(PARAM_STYLE, ml.getStyle()));
        ml.setMinScale(ConversionHelper.getDouble(params.getHttpParam(PARAM_MIN_SCALE), ml.getMinScale()));
        ml.setMaxScale(ConversionHelper.getDouble(params.getHttpParam(PARAM_MAX_SCALE), ml.getMaxScale()));

        ml.setLegendImage(params.getHttpParam(PARAM_LEGEND_IMAGE, ml.getLegendImage()));
        ml.setMetadataId(params.getHttpParam(PARAM_METADATA_ID, ml.getMetadataId()));

        final String gfiContent = request.getParameter(PARAM_GFI_CONTENT);
        if (gfiContent != null) {
            // Clean GFI content
            final String[] tags = PropertyUtil.getCommaSeparatedList("gficontent.whitelist");
            HashMap<String,String[]> attributes = new HashMap<String, String[]>();
            HashMap<String[],String[]> protocols = new HashMap<String[], String[]>();
            String[] allAttributes = PropertyUtil.getCommaSeparatedList("gficontent.whitelist.attr");
            if (allAttributes.length > 0) {
                attributes.put(":all",allAttributes);
            }
            List<String> attrProps = PropertyUtil.getPropertyNamesStartingWith("gficontent.whitelist.attr.");
            for (String attrProp : attrProps) {
                String[] parts = attrProp.split("\\.");
                if (parts[parts.length-2].equals("protocol")) {
                    protocols.put(new String[]{parts[parts.length-3],parts[parts.length-1]},PropertyUtil.getCommaSeparatedList(attrProp));
                } else {
                    attributes.put(parts[parts.length-1],PropertyUtil.getCommaSeparatedList(attrProp));
                }
            }
            ml.setGfiContent(RequestHelper.cleanHTMLString(gfiContent, tags, attributes, protocols));
        }

        ml.setUsername(params.getHttpParam(PARAM_USERNAME, ml.getUsername()));
        ml.setPassword(params.getHttpParam(PARAM_PASSWORD, ml.getPassword()));

        ml.setCapabilitiesUpdateRateSec(params.getHttpParam(PARAM_CAPABILITIES_UPDATE_RATE_SEC, 0));

        String attributes = params.getHttpParam(PARAM_ATTRIBUTES);
        if (attributes != null && !attributes.isEmpty()) {
            ml.setAttributes(JSONHelper.createJSONObject(attributes));
        }

        String parameters = params.getHttpParam(PARAM_PARAMS);
        if (parameters != null && !parameters.isEmpty()) {
            ml.setParams(JSONHelper.createJSONObject(parameters));
        }

        String options = params.getHttpParam(PARAM_OPTIONS);
        if (options != null && !options.isEmpty()) {
            ml.setOptions(JSONHelper.createJSONObject(options));
        }

        ml.setSrs_name(params.getHttpParam(PARAM_SRS_NAME, ml.getSrs_name()));
        ml.setVersion(params.getHttpParam(PARAM_VERSION,ml.getVersion()));

        ml.setRealtime(ConversionHelper.getBoolean(params.getHttpParam(PARAM_REALTIME), ml.getRealtime()));
        ml.setRefreshRate(ConversionHelper.getInt(params.getHttpParam(PARAM_REFRESH_RATE), ml.getRefreshRate()));

        final Set<String> systemCRSs;
        try {
            systemCRSs = ViewHelper.getSystemCRSs(viewService);
        } catch (ServiceException e) {
            throw new ActionException("Failed to retrieve system CRSs", e);
        }

        switch (ml.getType()) {
        case OskariLayer.TYPE_WMS:
            return handleWMSSpecific(params, ml, systemCRSs);
        case OskariLayer.TYPE_WMTS:
            return handleWMTSSpecific(params, ml, systemCRSs);
        case OskariLayer.TYPE_WFS:
            handleWFSSpecific(params, ml, systemCRSs); // fallthrough
        default:
            // no capabilities to update, return true
            return true;
        }
    }

    private boolean handleWMSSpecific(final ActionParameters params, OskariLayer ml, Set<String> systemCRSs) {
        // Do NOT modify the 'xslt' parameter
        HttpServletRequest request = params.getRequest();
        final String xslt = request.getParameter(PARAM_XSLT);
        if(xslt != null) {
            // TODO: some validation of XSLT data
            ml.setGfiXslt(xslt);
        }
        ml.setGfiType(params.getHttpParam(PARAM_GFI_TYPE, ml.getGfiType()));

        try {
            String data = CapabilitiesCacheService.getFromService(ml);
            WebMapService wms = OskariLayerCapabilitiesHelper.parseWMSCapabilities(data, ml);
            OskariLayerCapabilitiesHelper.setPropertiesFromCapabilitiesWMS(wms, ml, systemCRSs);
            capabilitiesService.save(ml, data);
            return true;
        } catch (ServiceException | WebMapServiceParseException | LayerNotFoundInCapabilitiesException ex) {
            LOG.error(ex, "Failed to set capabilities for layer", ml);
            return false;
        }
    }

    private boolean handleWMTSSpecific(final ActionParameters params, OskariLayer ml, Set<String> systemCRSs) {
        try {
            String currentCrs = params.getHttpParam(PARAM_SRS_NAME, ml.getSrs_name());
            String data = CapabilitiesCacheService.getFromService(ml);
            WMTSCapabilities caps = WMTSCapabilitiesParser.parseCapabilities(data);
            OskariLayerCapabilitiesHelper.setPropertiesFromCapabilitiesWMTS(caps, ml, systemCRSs);
            capabilitiesService.save(ml, data);
            return true;
        } catch (Exception ex) {
            LOG.error(ex, "Failed to set capabilities for layer", ml);
            return false;
        }
    }

    private void handleWFSSpecific(final ActionParameters params, OskariLayer ml, Set<String> systemCRSs) throws ActionException {
        // These are only in insert
        ml.setSrs_name(params.getHttpParam(PARAM_SRS_NAME, ml.getSrs_name()));
        ml.setVersion(params.getHttpParam(PARAM_VERSION, params.getHttpParam(PARAM_WFS_VERSION, ml.getVersion())));

        try {
            LayerCapabilitiesHelper.updateCapabilities(ml);
        } catch (Exception e) {
            LOG.warn("Couldn't update capabilities for WFS (" + ml.getVersion() + ") layer:", ml.getName(), e.getMessage());
        }
        ml.setCapabilitiesLastUpdated(new Date());
    }


    private String validateUrl(final String url) throws ActionParamsException {
        try {
            // check that it's a valid url by creating an URL object...
            new URL(url);
        } catch (MalformedURLException e) {
            throw new ActionParamsException(ERROR_INVALID_FIELD_VALUE + PARAM_LAYER_URL);
        }
        return url;
    }

    private void addPermissionsForRoles(final OskariLayer ml,
                                        final Set<Integer> viewRoleIds,
                                        final Set<Integer> publishRoleIds,
                                        final Set<Integer> downloadRoleIds,
                                        final Set<Integer> viewEmbeddedRoleIds) {
        Resource res = new Resource();
        res.setType(ResourceType.maplayer);
        res.setMapping(Integer.toString(ml.getId()));
        // insert permissions
        LOG.debug("Adding permission", PermissionType.VIEW_LAYER, "for roles:", viewRoleIds);
        for (int externalId : viewRoleIds) {
            Permission permission = new Permission();
            permission.setRoleId(externalId);
            permission.setType(PermissionType.VIEW_LAYER);
            res.addPermission(permission);
        }

        LOG.debug("Adding permission", PermissionType.PUBLISH, "for roles:", publishRoleIds);
        for (int externalId : publishRoleIds) {
            Permission permission = new Permission();
            permission.setRoleId(externalId);
            permission.setType(PermissionType.PUBLISH);
            res.addPermission(permission);
        }

        LOG.debug("Adding permission", PermissionType.DOWNLOAD, "for roles:", downloadRoleIds);
        for (int externalId : downloadRoleIds) {
            Permission permission = new Permission();
            permission.setRoleId(externalId);
            permission.setType(PermissionType.DOWNLOAD);
            res.addPermission(permission);
        }

        LOG.debug("Adding permission", PermissionType.VIEW_PUBLISHED, "for roles:", viewEmbeddedRoleIds);
        for (int externalId : viewEmbeddedRoleIds) {
            Permission permission = new Permission();
            permission.setRoleId(externalId);
            permission.setType(PermissionType.VIEW_PUBLISHED);
            res.addPermission(permission);
        }

        getPermissionsService().saveResource(res);
    }
}
