import olSourceBingMaps from 'ol/source/BingMaps';
import olLayerTile from 'ol/layer/Tile';
import BingMapsLayerModelBuilder from './BingMapsLayerModelBuilder';

const AbstractMapLayerPlugin = Oskari.clazz.get('Oskari.mapping.mapmodule.AbstractMapLayerPlugin');

class BingMapsLayerPlugin extends AbstractMapLayerPlugin {
    constructor (config) {
        super(config);
        this.__name = 'BingMapsLayerPlugin';
        this._clazz = 'Oskari.mapframework.mapmodule.BingMapsLayerPlugin';
        this.layertype = 'bingmaps';
    }
    /**
     * @private @method _initImpl
     * Interface method for the module protocol.
     */
    _initImpl () {
        // register domain builder
        const mapLayerService = this.getSandbox().getService('Oskari.mapframework.service.MapLayerService');
        if (mapLayerService) {
            mapLayerService.registerLayerModel(this.layertype + 'layer', this._getLayerModelClass());
            mapLayerService.registerLayerModelBuilder(this.layertype + 'layer', this._getModelBuilder());
        }
    }
    /**
     * @private @method _getLayerModelClass
     * Returns class to be used as mapLayerService layer model
     */
    _getLayerModelClass () {
        return 'Oskari.mapframework.mapmodule.BingMapsLayer';
    }
    /**
     * @private @method _getModelBuilder
     * Returns object to be used as mapLayerService layer model builder
     */
    _getModelBuilder () {
        return new BingMapsLayerModelBuilder();
    }
    /**
     * @private @method _createPluginEventHandlers
     * Called by superclass to create event handlers
     */
    _createPluginEventHandlers () {
        return {
            AfterChangeMapLayerStyleEvent (event) {
                this._updateLayerStyle(event.getMapLayer());
            }
        };
    }
    /**
     * @private @method _updateLayerStyle
     * @param {Oskari.mapframework.mapmodule.BingMapsLayer} oskariLayer
     */
    _updateLayerStyle (oskariLayer) {
        const olLayers = this.getOLMapLayers(oskariLayer);
        if (olLayers && olLayers.length > 0) {
            const lyrWithOldSource = olLayers[0];
            const lyrIndex = this.mapModule.getMap().getLayers().getArray().indexOf(lyrWithOldSource);
            // Create a new layer with the selected style and replace the old one.
            const lyrWithNewSource = this._createOlLayer(oskariLayer);
            this.mapModule.getMap().getLayers().insertAt(lyrIndex + 1, lyrWithNewSource);
            this.setOLMapLayers(oskariLayer.getId(), lyrWithNewSource);
            // Keep the old layer in the background till the new layer has had time to load.
            const removeOldLyrTimeout = 3000;
            setTimeout(() => {
                this.mapModule.getMap().removeLayer(lyrWithOldSource);
            }, removeOldLyrTimeout);
        }
    }
    /**
     * Checks if the layer can be handled by this plugin
     * @method  isLayerSupported
     * @param  {Oskari.mapframework.domain.AbstractLayer} layer
     * @return {Boolean}       true if this plugin handles the type of layers
     */
    isLayerSupported (layer) {
        if (!layer) {
            return false;
        }
        return layer.isLayerOfType(this.layertype);
    }
    /**
     * @method addMapLayerToMap
     * @private
     * @param {Oskari.mapframework.domain.BingMapsLayer} layer
     * @param {Boolean} keepLayerOnTop
     * @param {Boolean} isBaseMap
     */
    addMapLayerToMap (layer, keepLayerOnTop, isBaseMap) {
        const bingMapsLayer = this._createOlLayer(layer);
        this.mapModule.addLayer(bingMapsLayer, !keepLayerOnTop);
        this.setOLMapLayers(layer.getId(), bingMapsLayer);
    }

    /**
     * @private
     * @method _createOlLayer Creates a new ol.layer.Tile layer with a Bing Maps source
     * @param {Oskari.mapframework.domain.BingMapsLayer} layer
     */
    _createOlLayer (layer) {
        const bingMapsLayer = new olLayerTile({
            opacity: layer.getOpacity() / 100,
            source: this._createSource(layer)
        });
        return bingMapsLayer;
    }

    _createSource (oskariLayer) {
        const style = oskariLayer.getCurrentStyle();
        const opts = {
            key: oskariLayer.getApiKey(),
            imagerySet: style ? style.getName() : 'RoadOnDemand',
            maxZoom: 19
        };
        return new olSourceBingMaps(opts);
    }
}

Oskari.clazz.defineES('Oskari.mapframework.mapmodule.BingMapsLayerPlugin', BingMapsLayerPlugin,
    {
        /**
         * @property {String[]} protocol array of superclasses as {String}
         * @static
         */
        'protocol': ['Oskari.mapframework.module.Module', 'Oskari.mapframework.ui.module.common.mapmodule.Plugin']
    }
);

export default BingMapsLayerPlugin;
