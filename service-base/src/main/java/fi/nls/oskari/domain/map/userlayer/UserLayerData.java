package fi.nls.oskari.domain.map.userlayer;


import org.json.JSONObject;

public class UserLayerData {

    private long id;
    private long user_layer_id;
    private String uuid;
    private String feature_id ;
    private JSONObject property_json;
    private String  geometry;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getUser_layer_id() {
        return user_layer_id;
    }

    public void setUser_layer_id(long user_layer_id) {
        this.user_layer_id = user_layer_id;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getFeature_id() {
        return feature_id;
    }

    public void setFeature_id(String feature_id) {
        this.feature_id = feature_id;
    }

    public JSONObject getProperty_json() {
        return property_json;
    }

    public void setProperty_json(JSONObject property_json) {
        if(property_json == null) {
            this.property_json = new JSONObject();
            return;
        }
        this.property_json = property_json;
    }

    public String getGeometry() {
        return geometry;
    }

    public void setGeometry(String geometry) {
        this.geometry = geometry;
    }
}
