Oskari.clazz.define('Oskari.mapframework.publisher.tool.OpacityTool', function () {
},
{
    index: 1,
    group: 'data',
    allowedLocations: [],
    allowedSiblings: [],

    init: function (data) {
        var enabled = data &&
            Oskari.util.keyExists(data, 'configuration.statsgrid.conf') &&
            data.configuration.statsgrid.conf.transparent === true;
        this.setEnabled(enabled);
    },
    getTool: function (stateData) {
        var me = this;
        if (!me.__tool) {
            me.__tool = {
                id: 'Oskari.statistics.statsgrid.ClassificationPlugin',
                title: 'transparent',
                config: {
                    transparent: false
                }
            };
        }
        return me.__tool;
    },
    setEnabled: function (enabled) {
        var me = this;

        me.state.enabled = enabled;

        var stats = Oskari.getSandbox().findRegisteredModuleInstance('StatsGrid');
        if (!stats) {
            return;
        }
        if (!stats.classificationPlugin) {
            stats.createClassificationView(enabled);
        }
        if (enabled) {
            stats.classificationPlugin.makeTransparent(true);
        } else {
            stats.classificationPlugin.makeTransparent(false);
        }
    },
    getValues: function () {
        var me = this;
        var statsGridState = me.__sandbox.getStatefulComponents().statsgrid.getState();

        var statslayerOnMap = this._getStatsLayer();
        if (!statslayerOnMap || !statsGridState) {
            return null;
        }
        if (!me.state.enabled) {
            return null;
        }
        return {
            configuration: {
                statsgrid: {
                    state: statsGridState,
                    conf: {
                        transparent: me.state.enabled
                    }
                }
            }
        };
    },
    stop: function () {
    }
}, {
    'extend': ['Oskari.mapframework.publisher.tool.AbstractStatsPluginTool'],
    'protocol': ['Oskari.mapframework.publisher.Tool']
});
