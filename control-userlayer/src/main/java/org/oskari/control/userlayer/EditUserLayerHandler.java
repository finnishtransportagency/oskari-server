package org.oskari.control.userlayer;

import fi.nls.oskari.control.*;
import org.oskari.log.AuditLog;
import org.json.JSONException;
import org.json.JSONObject;

import fi.nls.oskari.annotation.OskariActionRoute;
import fi.nls.oskari.domain.map.userlayer.UserLayer;
import fi.nls.oskari.domain.map.UserDataStyle;
import fi.nls.oskari.service.OskariComponentManager;
import fi.nls.oskari.util.JSONHelper;
import fi.nls.oskari.util.ResponseHelper;
import org.oskari.map.userlayer.service.UserLayerDataService;
import org.oskari.map.userlayer.service.UserLayerDbService;

/**
 * Expects to get layer id as http parameter "id".
 */
@OskariActionRoute("EditUserLayer")
public class EditUserLayerHandler extends RestActionHandler {

    private static final String PARAM_DESC = "desc";
    private static final String PARAM_NAME = "name";
    private static final String PARAM_SOURCE = "source";
    private static final String PARAM_STYLE = "style";

    private UserLayerDbService userLayerDbService;

    @Override
    public void init() {
        userLayerDbService = OskariComponentManager.getComponentOfType(UserLayerDbService.class);
    }

    @Override
    public void handlePost(ActionParameters params) throws ActionException {
        String mapSrs = params.getHttpParam(ActionConstants.PARAM_SRS);
        final UserLayer userLayer = UserLayerHandlerHelper.getUserLayer(userLayerDbService, params);
        userLayer.setLayer_name(params.getRequiredParam(PARAM_NAME));
        userLayer.setLayer_desc(params.getHttpParam(PARAM_DESC, userLayer.getLayer_desc()));
        userLayer.setLayer_source(params.getHttpParam(PARAM_SOURCE, userLayer.getLayer_source()));
        final UserDataStyle style = userLayer.getStyle();
        updateStyleProperties(style, params.getHttpParam(PARAM_STYLE));

        userLayerDbService.updateUserLayerCols(userLayer);
        userLayerDbService.updateUserLayerStyleCols(style);

        AuditLog.user(params.getClientIp(), params.getUser())
                .withParam("id", userLayer.getId())
                .updated(AuditLog.ResourceType.USERLAYER);

        JSONObject ulayer = UserLayerDataService.parseUserLayer2JSON(userLayer, mapSrs);
        JSONObject permissions = UserLayerHandlerHelper.getPermissions();
        JSONHelper.putValue(ulayer, "permissions", permissions);

        ResponseHelper.writeResponse(params, ulayer);
    }

    private void updateStyleProperties(UserDataStyle style, String styleJSON) throws ActionParamsException {
        try {
            JSONObject stylejs = JSONHelper.createJSONObject(styleJSON);
            style.populateFromOskariJSON(stylejs);
        } catch (JSONException e) {
            throw new ActionParamsException("Unable to populate style from JSON", e);
        }
    }

}
