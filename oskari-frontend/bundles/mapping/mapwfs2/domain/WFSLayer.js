/**
 * @class Oskari.mapframework.bundle.mapwfs2.domain.WFSLayer
 *
 * MapLayer of type WFS
 */
Oskari.clazz.define('Oskari.mapframework.bundle.mapwfs2.domain.WFSLayer',

    /**
     * @method create called automatically on construction
     * @static
     */

    function () {
        /* Layer Type */
        this._layerType = 'WFS';
        this._featureData = true;
        this._fields = []; // property names
        this._locales = []; // property name locales
        this._activeFeatures = []; // features on screen
        this._selectedFeatures = []; // filtered features
        this._clickedFeatureIds = []; // clicked feature ids (map)
        this._clickedFeatureListIds = []; // clicked feature ids (list)
        this._clickedGeometries = []; // clicked feature geometries [[id, geom]..]
        this._propertyTypes = {}; // name and describeFeatureType type (hashmap, json) (Analysis populates)
        this._wpsLayerParams = {}; // wfs/wps analysis layer params (hashmap, json)    (Analysis populates)
        this._styles = []; /* Array of styles that this layer supports */
        this._customStyle = null;
        this._filterJson = null;
        this._internalOpened = false;

        this.localization = Oskari.getLocalization('MapWfs2');
    }, {
        /* Layer type specific functions */

        /**
         * @method getFields
         * @return {String[]} fields
         */
        getFields: function () {
            return this._fields;
        },

        /**
         * @method setFields
         * @param {String[]} fields
         */
        setFields: function (fields) {
            this._fields = fields;
        },

        /**
         * @method getLocales
         * @return {String[]} locales
         */
        getLocales: function () {
            return this._locales;
        },

        /**
         * @method setLocales
         * @param {String[]} locales
         */
        setLocales: function (locales) {
            this._locales = locales;
        },

        /**
         * @method getActiveFeatures
         * @return {Object[]} features
         */
        getActiveFeatures: function () {
            return this._activeFeatures;
        },

        /**
         * @method setActiveFeature
         * @param {Object} feature
         */
        setActiveFeature: function (feature) {
            this._activeFeatures.push(feature);
        },

        /**
         * @method setActiveFeatures
         * @param {Object[]} features
         */
        setActiveFeatures: function (features) {
            this._activeFeatures = features;
        },

        /**
         * @method getSelectedFeatures
         * @return {Object[]} features
         */
        getSelectedFeatures: function () {
            return this._selectedFeatures;
        },

        /**
         * @method setSelectedFeature
         * @param {Object} feature
         */
        setSelectedFeature: function (feature) {
            this._selectedFeatures.push(feature);
        },

        /**
         * @method setSelectedFeatures
         * @param {Object[]} features
         */
        setSelectedFeatures: function (features) {
            this._selectedFeatures = features;
        },

        /**
         * @method getClickedFeatureIds
         * @return {String[]} featureIds
         */
        getClickedFeatureIds: function () {
            return this._clickedFeatureIds;
        },

        /**
         * @method setClickedFeatureId
         * @param {String} id
         */
        setClickedFeatureId: function (id) {
            this._clickedFeatureIds.push(id);
        },

        /**
         * @method setClickedFeatureIds
         * @param {String[]} features
         */
        setClickedFeatureIds: function (ids) {
            this._clickedFeatureIds = ids;
        },

        /**
         * @method getClickedFeatureListIds
         * @return {String[]} featureIds
         */
        getClickedFeatureListIds: function () {
            return this._clickedFeatureListIds;
        },

        /**
         * @method setClickedFeatureListId
         * @param {String} id
         */
        setClickedFeatureListId: function (id) {
            this._clickedFeatureListIds.push(id);
        },

        /**
         * @method setClickedFeatureListIds
         * @param {String} id
         */
        setClickedFeatureListIds: function (ids) {
            this._clickedFeatureListIds = ids;
        },
        /**
         * @method getClickedGeometries
         * @return {String[[]..]} featureId, geometry
         */
        getClickedGeometries: function () {
            return this._clickedGeometries;
        },

        /**
         * @method setClickedGeometries
         * @param {String[[]..]} id,geom
         */
        setClickedGeometries: function (fgeom) {
            this._clickedGeometries = fgeom;
        },
        /**
         * @method addClickedGeometries
         * @param {[]} id,geom
         */
        addClickedGeometries: function (fgeom) {
            for (var j = 0; j < fgeom.length; ++j) {
                this._clickedGeometries.push(fgeom[j]);
            }
        },
        /**
         * @method setPropertyTypes
         * @param {json} propertyTypes
         */
        setPropertyTypes: function (propertyTypes) {
            this._propertyTypes = propertyTypes;
        },

        /**
         * @method getPropertyTypes
         * @return {json} propertyTypes
         */
        getPropertyTypes: function () {
            return this._propertyTypes;
        },
        /**
         * @method setWpsLayerParams
         * @param {json} wpsLayerParams
         */
        setWpsLayerParams: function (wpsLayerParams) {
            this._wpsLayerParams = wpsLayerParams;
        },

        /**
         * @method getWpsLayerParams
         * @return {json} wpsLayerParams
         */
        getWpsLayerParams: function () {
            return this._wpsLayerParams;
        },

        /**
         * @method getFilterJson
         * @return {Object[]} filterJson
         */
        getFilterJson: function () {
            return this._filterJson;
        },

        /**
         * @method setFilterJson
         * @param {Object} filterJson
         */
        setFilterJson: function (filterJson) {
            this._filterJson = filterJson;
        },

        /**
         * Overriding getLegendImage for WFS
         *
         * @method getLegendImage
         * @return {String} URL to a legend image
         */
        getLegendImage: function () {
            return null;
        },

        /**
         * @method setCustomStyle
         * @param {json} customStyle
         */
        setCustomStyle: function (customStyle) {
            this._customStyle = customStyle;
        },

        /**
         * @method getCustomStyle
         * @return {json} customStyle
         */
        getCustomStyle: function () {
            return this._customStyle;
        },

        /**
         * @method getStyles
         * @return {Oskari.mapframework.domain.Style[]}
         * Gets layer styles
         */
        getStyles: function () {
            if (this.getCustomStyle()) {
                var locOwnStyle = this.localization['own-style'];
                var style = Oskari.clazz.create('Oskari.mapframework.domain.Style');
                style.setName('oskari_custom');
                style.setTitle(locOwnStyle);
                style.setLegend('');
                return this._styles.concat([style]);
            }
            return this._styles;
        },
        /**
         * @method getStyleDef
         * @param {String} styleName
         * @return {Object}
         */
        getStyleDef (styleName) {
            if (styleName === 'oskari_custom') {
                return {[this._layerName]: {featureStyle: this.getCustomStyle()}};
            }
            if (this._options.styles) {
                return this._options.styles[styleName];
            }
        },
        /**
         * @method setWMSLayerId
         * @param {String} id
         * Unique identifier for map layer used to reference the WMS layer,
         * which is linked to WFS layer for to use for rendering
         */
        setWMSLayerId: function (id) {
            this._WMSLayerId = id;
        },
        /**
         * @method getWMSLayerId
         * @return {String}
         * Unique identifier for map layer used to reference the WMS layer,
         * which is linked to WFS layer for to use for rendering
         * (e.g. MapLayerService)
         */
        getWMSLayerId: function () {
            return this._WMSLayerId;
        },
        /**
         * @method getTileGrid
         * Returns tile grid from options or default tile grid (EPSG:3067)
         */
        getTileGrid: function () {
            return this._options.tileGrid || {
                origin: [-548576, 8388608],
                resolutions: [8192, 4096, 2048, 1024, 512, 256, 128, 64, 32, 16, 8, 4, 2, 1, 0.5, 0.25],
                tileSize: [256, 256]
            };
        },
        /**
         * @method getLayerUrl
         * Superclass override
         */
        getLayerUrl: function () {
            return Oskari.urls.getRoute('GetWFSVectorTile') + `&id=${this.getId()}&srs={epsg}&z={z}&x={x}&y={y}`;
        }
    }, {
        'extend': ['Oskari.mapframework.mapmodule.VectorTileLayer']
    });
