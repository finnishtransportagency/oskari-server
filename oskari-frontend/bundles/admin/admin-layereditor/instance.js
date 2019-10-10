import LayerEditorFlyout from './view/Flyout';
const BasicBundle = Oskari.clazz.get('Oskari.BasicBundle');

Oskari.clazz.defineES('Oskari.admin.admin-layereditor.instance',
    class AdminLayerEditor extends BasicBundle {
        constructor () {
            super();
            this.loc = Oskari.getMsg.bind(null, 'admin-layereditor');
            this.eventHandlers = {
                'MapLayerEvent': (event) => {
                    if (event.getOperation() !== 'add') {
                        // only handle add layer
                        return;
                    }
                    if (event.getLayerId()) {
                        this._addTool(event.getLayerId());
                    } else { // initial layer load
                        this._setupLayerTools();
                    }
                }
            };
        }
        _startImpl () {
            this._setupLayerTools();
        }

        /**
         * Fetches reference to the map layer service
         * @return {Oskari.mapframework.service.MapLayerService}
         */
        _getLayerService () {
            return this.sandbox.getService('Oskari.mapframework.service.MapLayerService');
        }

        /**
         * Adds tools for all layers
         */
        _setupLayerTools () {
            // add tools for feature data layers
            const service = this._getLayerService();
            const layers = service.getAllLayers();
            layers.forEach(layer => {
                this._addTool(layer, true);
            });
            // update all layers at once since we suppressed individual events
            const event = Oskari.eventBuilder('MapLayerEvent')(null, 'tool');
            this.sandbox.notifyAll(event);
        }

        /**
         * Adds the layer edit tool for layer
         * @method  @private _addTool
         * @param  {String| Number} layerId layer to process
         * @param  {Boolean} suppressEvent true to not send event about updated layer (optional)
         */
        _addTool (layer, suppressEvent) {
            const service = this._getLayerService();
            if (typeof layer !== 'object') {
                // detect layerId and replace with the corresponding layerModel
                layer = service.findMapLayer(layer);
            }
            if (!layer || layer.getLayerType() !== 'wfs' || layer.getVersion() !== '1.1.0') {
                return;
            }

            // add feature data tool for layer
            const tool = Oskari.clazz.create('Oskari.mapframework.domain.Tool');
            tool.setName('layer-editor');
            tool.setIconCls('icon-info-area');
            tool.setTooltip(this.loc('editor-tool'));
            tool.setTypes(['layerList']);

            tool.setCallback(() => {
                this._showEditor(layer.getId());
            });

            service.addToolForLayer(layer, tool, suppressEvent);
        }
        /**
         * @private @method _showEditor
         * Opens flyout with layer editor for given layerId
         * @param {Number} layerId
         */
        _showEditor (layerId) {
            const flyout = this._getFlyout();
            flyout.setLayerId(layerId);
            flyout.show();
        }

        /**
         * @private @method _getFlyout
         * Ensure flyout exists and return it
         * @return {LayerEditorFlyout}
         */
        _getFlyout () {
            if (!this.flyout) {
                const xPosition = jQuery('#mapdiv').position().left;
                const offset = 150;

                this.flyout = new LayerEditorFlyout(this.loc('flyout-title'));
                this.flyout.move(xPosition + offset, 15, true);
                this.flyout.makeDraggable({
                    handle: '.oskari-flyouttoolbar',
                    scroll: false
                });
            }
            return this.flyout;
        }
    }
);
