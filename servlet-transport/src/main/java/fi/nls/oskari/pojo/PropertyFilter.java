package fi.nls.oskari.pojo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Handles user's active WFS feature filtering
 * 
 * Used for storing filter in SessionStore (not saved).
 * 
 * @see fi.nls.oskari.pojo.SessionStore
 */
public class PropertyFilter {
	private static final Logger log = LogFactory.getLogger(PropertyFilter.class);

	private JSONObject propertyFilter;
    private String layerId;

	/**
	 * Gets raw property filter data
	 * 
	 * @return propertyFilter
	 */
	@JsonIgnore
	public JSONObject getPropertyFilter() {
		return propertyFilter;
	}

	/**
	 * Sets raw property filter data
	 * 
	 * @param propertyFilter
	 */
	public void setPropertyFilter(JSONObject propertyFilter) {
		this.propertyFilter = propertyFilter;
	}

    public String getLayerId() {
        return layerId;
    }

    public void setLayerId(String layerId) {
        this.layerId = layerId;
    }

    /**
	 * Transforms parameters JSON String to object
	 * 
	 * @param json
	 * @return object
	 */
	@JsonIgnore
	public static PropertyFilter setParamsJSON(String json) {
        PropertyFilter filter = new PropertyFilter();

        JSONObject data;
		try {
			data = new JSONObject(json);
		} catch (JSONException e) {
			log.error(e, "Reading JSON data failed");
			return filter;
		}

		try {
			filter.setPropertyFilter( (JSONObject) data.get("filters"));
			filter.setLayerId(data.get("layer_id").toString());
		} catch (Exception e) {
			log.error(e, "Reading JSON data data property filters failed");
			return null;
		}

		return filter;
	}
}
