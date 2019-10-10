Oskari.clazz.define('Oskari.mapframework.publisher.tool.ClassificationTool', function () {
}, {
    index: 1,
    group: 'data',
    allowedLocations: ['top left', 'top right', 'bottom-right'],
    lefthanded: 'bottom right',
    righthanded: 'bottom right',

    allowedSiblings: [
        'Oskari.mapframework.bundle.mapmodule.plugin.MyLocationPlugin',
        'Oskari.mapframework.bundle.mapmodule.plugin.PanButtons',
        'Oskari.mapframework.bundle.mapmodule.plugin.Portti2Zoombar'
    ],
    init: function (pdata) {
        var stats = Oskari.getSandbox().findRegisteredModuleInstance('StatsGrid');
        if (stats) {
            stats.createClassficationView(true);
        }
        if (pdata && Oskari.util.keyExists(pdata, 'configuration.statsgrid.conf') && pdata.configuration.statsgrid.conf.allowClassification !== false) {
            this.setEnabled(true);
        } else {
            this.setEnabled(false);
        }
    },
    // required for dragndrop in publisher - also plugin needs to
    getPlugin: function () {
        var stats = Oskari.getSandbox().findRegisteredModuleInstance('StatsGrid');
        return stats.classificationPlugin;
    },
    getTool: function (pdata) {
        if (!this.__tool) {
            this.__tool = {
                id: 'Oskari.statistics.statsgrid.ClassificationPlugin',
                title: 'allowClassification',
                config: {
                    allowClassification: false
                }
            };
        }
        return this.__tool;
    },
    setEnabled: function (enabled) {
        if (typeof enabled !== 'boolean') {
            enabled = false;
        }

        this.state.enabled = enabled;
        this.getPlugin().enableClassification(enabled);
    },
    getValues: function () {
        var me = this;
        var statsGridState = me.__sandbox.getStatefulComponents().statsgrid.getState();
        // just to make sure if user removes the statslayer while in publisher
        // if there is no statslayer on map -> don't setup publishedgrid
        // otherwise always return the state even if grid is not selected so
        //  publishedgrid gets the information it needs to render map correctly
        var statslayerOnMap = this._getStatsLayer();
        if (statslayerOnMap && statsGridState) {
            // without view = true -> the sidepanel is not shown when the statsgrid bundle starts
            statsGridState.view = me.state.enabled;
            return {
                configuration: {
                    statsgrid: {
                        conf: {
                            allowClassification: me.state.enabled,
                            legendLocation: 'bottom right'
                        }
                    }
                }
            };
        } else {
            return {};
        }
    },
    /**
    * Stop tool.
    * @method stop
    * @public
    */
    stop: function () {
        var stats = Oskari.getSandbox().findRegisteredModuleInstance('StatsGrid');
        if (stats) {
            stats.enableClassification(true);
            stats.createClassficationView(false);
        }
    }
}, {
    'extend': ['Oskari.mapframework.publisher.tool.AbstractStatsPluginTool'],
    'protocol': ['Oskari.mapframework.publisher.Tool']
});
