package org.oskari.permissions.model;

public enum PermissionType {
    VIEW_LAYER(true),
    VIEW_PUBLISHED(true),
    PUBLISH("publish", true),
    DOWNLOAD("download", true),
    // false because UI breaks...
    EDIT_LAYER("edit", false),
    // false because UI breaks...
    EDIT_LAYER_CONTENT,
    EXECUTE,
    ADD_MAPLAYER;

    // name of the permission if it's different than the name in layer JSON
    private String jsonKey;
    // true to list this permission type on admin UI per layer
    private boolean layerSpecific = false;

    PermissionType() {
        jsonKey = this.name();
    }
    PermissionType(boolean layerSpecific) {
        this();
        this.layerSpecific  = layerSpecific;
    }
    PermissionType(String jsonName, boolean layerSpecific) {
        this(layerSpecific);
        jsonKey = jsonName;
    }

    public String getJsonKey() {
        return jsonKey;
    }
    public boolean isLayerSpecific() {
        return layerSpecific;
    }
}
