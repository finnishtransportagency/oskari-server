package fi.nls.oskari.control.statistics.data;

public class RegionValue {
    private String region;
    private IndicatorValue value;

    public RegionValue(String region, IndicatorValue value) {
        this.region = region;
        this.value = value;
    }

    public String getRegion() {
        return region;
    }

    public IndicatorValue getValue() {
        return value;
    }

}
