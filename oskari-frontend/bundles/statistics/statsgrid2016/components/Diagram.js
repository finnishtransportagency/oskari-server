Oskari.clazz.define('Oskari.statistics.statsgrid.Diagram', function (service, locale) {
    this.loc = locale;
    this.sb = service.getSandbox();
    this.service = service;
    this.element = null;
    this._chartInstance = Oskari.clazz.create('Oskari.userinterface.component.Chart');
    this._chartElement = null;
    this._renderState = {
        inProgress: false,
        repaint: false
    };
    this.events();
}, {
    _template: {
        container: jQuery('<div></div>')
    },
    /**
     * @method  @public render Render diagram
     * @param  {Object} el jQuery element
     */
    render: function (el, options) {
        var me = this;
        if (options) {
            me._chartInstance.setResizable(!!options.resizable);
        }
        if (this.element) {
            // already rendered, just move the element to new el when needed
            if (el !== this.element.parent()) {
                this.element.detach();
                el.append(this.element);
            }
            // update ui if diagram is resizable
            if (options && options.resizable) {
                me.updateUI(options);
            }
            return;
        }
        this.element = this._template.container.clone();
        el.append(this.element);
        this.updateUI(options);
    },
    updateUI: function (options) {
        var me = this;
        var el = this.element;
        if (!el) {
            // ui not yet created so no need to update it
            return;
        }

        if (!this.hasIndicators()) {
            this.clearChart();
            this.element.html(this.loc.statsgrid.noResults);
            return;
        } else if (this._chartElement) {
            // reattach possibly detached component
            el.html(this._chartElement);
        }
        if (this._renderState.inProgress) {
            // handle render being called multiple times in quick succession
            // previous render needs to end before repaint since we are doing async stuff
            this._renderState.repaint = true;
            // need to call this._renderDone(); to trigger repaint after render done
            return;
        }
        this._renderState.inProgress = true;
        this.getIndicatorData(this.getIndicator(), function (data) {
            if (!data) {
                me._renderDone();
                return;
            }
            var isUndefined = function (element) {
                return element.value === undefined;
            };

            if (data.every(isUndefined)) {
                me.clearChart();
                me._renderDone();
                me.element.html(me.loc.statsgrid.noValues);
                return;
            }
            var classificationOpts = me.service.getStateService().getClassificationOpts(me.getIndicator().hash);
            var fractionDigits = typeof classificationOpts.fractionDigits === 'number' ? classificationOpts.fractionDigits : 1;
            var formatter = Oskari.getNumberFormatter(fractionDigits);
            var chartOpts = {
                colors: me.getColorScale(data),
                valueRenderer: function (val) {
                    if (typeof val !== 'number') {
                        return null;
                    }
                    return formatter.format(val);
                }
            };

            if (me._chartInstance.isResizable()) {
                var dataCharts = jQuery(el).closest('.oskari-datacharts');
                if (options && options.height) {
                    // height for flyout toolbar, defaults to 57px (.oskari-flyouttoolbar height)
                    const heightOffset = jQuery(el).closest('.oskari-flyout').find('.oskari-flyouttoolbar:first').height() || 57;
                    jQuery(el).closest('.oskari-flyoutcontentcontainer').css('max-height', 'none').height(options.height - heightOffset);
                }
                if (options && options.width) {
                    // helps to calculate container width for chart, defaults to 16px + 16px padding
                    const widthOffset = (parseInt(dataCharts.css('padding-left').replace(/[^-\d.]/g, '')) +
                        parseInt(dataCharts.css('padding-right').replace(/[^-\d.]/g, ''))) || 32;
                    chartOpts.width = options.width - widthOffset;
                    dataCharts.width(options.width - widthOffset);
                }
            }

            if (!me._chartElement) {
                me._chartElement = me.createBarCharts(data, chartOpts);
                el.html(me._chartElement);
            } else {
                me.getChartInstance().redraw(data, chartOpts);
            }

            var labels = me.getChartHeaderElement();
            el.parent().parent().find('.axisLabel').append(labels);

            me._renderDone();
        });
    },
    /** **** PRIVATE METHODS ******/
    /**
     * Triggers a new render when needed (if render was called before previous was finished)
     */
    _renderDone: function () {
        var state = this._renderState;
        this._renderState = {};
        if (state.repaint) {
            this.updateUI();
        }
    },
    getChartHeaderElement: function () {
        if (this.getChartInstance().chartIsInitialized()) {
            return this.getChartInstance().getGraphAxisLabels();
        } else {
            return null;
        }
    },
    /**
     * @method createBarCharts
     * Creates the barchart component if chart is not initialized
     */
    createBarCharts: function (data, chartOpts) {
        if (data === undefined || data.length === 0) {
            Oskari.log('statsgrid.DiagramVisualizer').debug('no indicator data');
            return null;
        }

        if (!this.getChartInstance().chartIsInitialized()) {
            var barchart = this.getChartInstance().createBarChart(data, chartOpts);
            var el = jQuery(barchart);
            el.css({
                'width': '100%'
            });
            el.attr('id', 'graphic');
            return el;
        }
    },
    getChartInstance: function () {
        return this._chartInstance;
    },
    clearChart: function () {
        var chart = this.getChartInstance();
        if (chart) {
            chart.clear();
        }
    },
    getIndicator: function () {
        return this.service.getStateService().getActiveIndicator();
    },
    hasIndicators: function () {
        return !!this.service.getStateService().getIndicators().length;
    },
    getIndicatorData: function (indicator, callback) {
        if (!indicator) {
            callback();
            return;
        }
        this.service.getCurrentDataset(function (err, response) {
            if (err) {
                callback();
                return;
            }
            var indicatorData = [];
            response.data.forEach(function (dataItem) {
                indicatorData.push({
                    name: dataItem.name,
                    value: dataItem.values[indicator.hash],
                    id: dataItem.id
                });
            });
            callback(indicatorData);
        });
    },
    /**
     * @method createDataSortOption
     * creates an SelectList with options for sorting the data
     */
    createDataSortOption: function (container) {
        var me = this;
        var dropdownOptions = {
            placeholder_text: this.loc.datacharts.sorting.desc,
            no_results_text: 'locale.panels.newSearch.noResults'
        };
        // hardcoded
        var sortTypes = [
            {
                id: 'value-descending',
                title: this.loc.datacharts.sorting['value-descending']
            },
            {
                id: 'value-ascending',
                title: this.loc.datacharts.sorting['value-ascending']
            },
            {
                id: 'name-ascending',
                title: this.loc.datacharts.sorting['name-ascending']
            },
            {
                id: 'name-descending',
                title: this.loc.datacharts.sorting['name-descending']
            }
        ];
        var select = Oskari.clazz.create('Oskari.userinterface.component.SelectList');
        var dropdown = select.create(sortTypes, dropdownOptions);

        dropdown.css({
            width: '180px',
            marginLeft: '10px'
        });

        select.adjustChosen();

        dropdown.on('change', function (event) {
            event.stopPropagation();
            me.getChartInstance().sortDataByType(select.getValue());
        });
        container.append(dropdown);
    },
    /**
     * @method getColorScale
     * gets the color scale of the mapselection
     * @return colors[] containing colors
     */
    getColorScale: function (data) {
        var me = this;
        // Format data for Oskari.statistics.statsgrid.ClassificationService.getClassification
        var numericData = {};
        data.forEach(function (entry) {
            numericData[entry.id] = entry.value;
        });
        var stateService = this.service.getStateService();
        var activeIndicator = stateService.getActiveIndicator();
        var classificationOpts = stateService.getClassificationOpts(activeIndicator.hash);
        var classification = me.service.getClassificationService().getClassification(numericData, classificationOpts);
        var colors = this.service.getColorService().getColorsForClassification(classificationOpts, true);
        return classification.maxBounds && colors ? {bounds: classification.maxBounds, values: colors} : {bounds: [], values: ['#555', '#555']};
    },
    events: function () {
        var me = this;
        this.service.on('StatsGrid.ActiveIndicatorChangedEvent', function (event) {
            if (event.getCurrent()) {
                me.updateUI();
            }
        });
        this.service.on('StatsGrid.IndicatorEvent', function (event) {
            if (event.isRemoved() && !me.hasIndicators()) {
                // last indicator removed -> update ui/cleanup
                me.updateUI();
            }
        });
        this.service.on('StatsGrid.RegionsetChangedEvent', function () {
            me.updateUI();
        });
    }
});
