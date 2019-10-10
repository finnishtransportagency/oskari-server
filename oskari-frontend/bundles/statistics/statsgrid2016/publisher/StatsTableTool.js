Oskari.clazz.define('Oskari.mapframework.publisher.tool.StatsTableTool', function () {
}, {
    index: 0,
    group: 'data',
    allowedLocations: [],
    allowedSiblings: [],

    groupedSiblings: false,
    templates: {},
    id: 'table',
    /**
     * Initialize tool
     * @params {} state data
     * @method init
     * @public
     */
    init: function (pdata) {
        var enabled = pdata &&
            Oskari.util.keyExists(pdata, 'configuration.statsgrid.conf') &&
            pdata.configuration.statsgrid.conf.grid === true;
        this.setEnabled(enabled);
    },
    /**
    * Get tool object.
     * @params {}  pdta.configuration.publishedgrid.state
    * @method getTool
    * @private
    *
    * @returns {Object} tool
    */
    getTool: function (pdata) {
        var me = this;
        if (!me.__tool) {
            me.__tool = {
                id: 'Oskari.statistics.statsgrid.StatsGridBundleInstance',
                title: 'grid',
                config: {
                    grid: true
                }
            };
        }
        return me.__tool;
    },
    /**
    * Set enabled.
    * @method setEnabled
    * @public
    *
    * @param {Boolean} enabled is tool enabled or not
    */
    setEnabled: function (enabled) {
        var me = this;
        var changed = me.state.enabled !== enabled;
        me.state.enabled = enabled;

        var stats = Oskari.getSandbox().findRegisteredModuleInstance('StatsGrid');
        if (!stats || !changed) {
            return;
        }
        if (enabled) {
            stats.togglePlugin.addTool(this.id);
        } else {
            stats.togglePlugin.removeTool(this.id);
        }
    },
    getValues: function () {
        var me = this;
        var statsGridState = me.__sandbox.getStatefulComponents().statsgrid.getState();
        // just to make sure if user removes the statslayer while in publisher
        // if there is no statslayer on map -> don't setup statsgrid
        // otherwise always return the state even if grid is not selected so
        // statsgrid gets the information it needs to render map correctly
        var statslayerOnMap = this._getStatsLayer();
        if (!statslayerOnMap || !statsGridState) {
            return null;
        }
        return {
            configuration: {
                statsgrid: {
                    state: statsGridState,
                    conf: {
                        grid: me.state.enabled
                    }
                }
            }
        };
    },
    stop: function () {
        var stats = Oskari.getSandbox().findRegisteredModuleInstance('StatsGrid');
        if (stats) {
            stats.togglePlugin.removeTool(this.id);
        }
    }
}, {
    'extend': ['Oskari.mapframework.publisher.tool.AbstractStatsPluginTool'],
    'protocol': ['Oskari.mapframework.publisher.Tool']
});
