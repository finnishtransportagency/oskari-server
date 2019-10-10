Oskari.clazz.define('Oskari.statistics.statsgrid.Legend', function (sandbox, locale) {
    this.sb = sandbox;
    this.locale = locale;
    this.log = Oskari.log('Oskari.statistics.statsgrid.Legend');
    this.service = this.sb.getService('Oskari.statistics.statsgrid.StatisticsService');
    this.__templates = {
        error: _.template('<div class="legend-noactive">${ msg }</div>'),
        header: _.template('<div class="header"><div class="link">${ link }</div><div class="title">${ source }</div><div class="sourcename">${ label }</div></div>'),
        activeHeader: _.template('<div class="title">${label}</div>'),
        edit: _.template('<div class="edit-legend" title="${ tooltip }"></div>')
    };
    this._element = jQuery('<div class="statsgrid-legend-container"> ' +
        '<div class="active-header"></div>' +
        '<div class="classification"></div>' +
        '<div class="active-legend"></div>' +
    '</div>');
    this._bindToEvents();

    this.editClassification = Oskari.clazz.create('Oskari.statistics.statsgrid.EditClassification', sandbox, this.locale);
    this._renderState = {
        panels: {}
    };
    this._accordion = Oskari.clazz.create('Oskari.userinterface.component.Accordion');
    this._renderQueue = [];
    // some components need to know when rendering is completed.
    Oskari.makeObservable(this);
}, {
    /**
     * Enables/disables the classification editor form
     * @param  {Boolean} enabled true to enable, false to disable
     */
    allowClassification: function (enabled) {
        this.editClassification.setEnabled(enabled);
    },
    // Header
    //   Source nn
    //   Indicator name + params
    //   (Link to change source - only shown if we have more than one indicator)
    // Accordion (or note about "insufficient data")
    //   Classification panel
    //   Legend
    //
    // Alternatively note about no indicator selected
    render: function (el, event) {
        var me = this;
        // handle render being called multiple times in quick succession
        // previous render needs to end before new render since we are doing async stuff
        if (!this._renderState.inProgress) {
            me._renderState.inProgress = true;
        } else {
            me._renderQueue.push({el: el, event: event});
            return;
        }
        var container = this._element;
        var accordion = this._accordion;
        // NOTE! detach classification before re-render to keep eventhandlers
        this.editClassification.getElement().detach();
        accordion.removeAllPanels();

        container.find('.legend-noactive').remove();
        container.children().empty();

        if (el) {
            // attach container to parent if provided, otherwise updates UI in the current parent
            el.append(container);
        }
        // check if we have an indicator to use or just render "no data"
        var activeIndicator = this.service.getStateService().getActiveIndicator();
        if (!activeIndicator) {
            container.append(this.__templates.error({ msg: this.locale('legend.noActive') }));
            me._renderDone();
            this.trigger('content-rendered');
            return;
        }
        // render classification options
        var classificationOpts = this.service.getStateService().getClassificationOpts(activeIndicator.hash);
        me._createClassificationUI(classificationOpts, function (classificationUI) {
            container.append(classificationUI);

            var panelClassification = me._createAccordionPanel(me.locale('classify.edit.title'));
            panelClassification.setContent(classificationUI);
            // add panels to accordion
            accordion.addPanel(panelClassification);
            var mountPoint = container.find('.classification');
            // add accordion to the container
            accordion.insertTo(mountPoint);
        });
        this._createLegend(activeIndicator, function (legendUI, classificationOpts) {
            var headerContainer = container.find('.active-header');
            var legendContainer = container.find('.active-legend');
            headerContainer.empty();
            legendContainer.empty();
            container.find('.legend-noactive').empty();
            // create inidicator dropdown if we have more than one indicator
            var hasMultiple = me.service.getStateService().getIndicators().length > 1;

            if (hasMultiple) {
                var indicatorMenu = Oskari.clazz.create('Oskari.statistics.statsgrid.SelectedIndicatorsMenu', me.service);
                indicatorMenu.render(headerContainer);
                indicatorMenu.setWidth('94%');
                headerContainer.addClass('multi-select-legend');
            } else {
                headerContainer.removeClass('multi-select-legend');
            }
            me._getLabels(activeIndicator, function (labels) {
                if (hasMultiple) {
                    headerContainer.attr('data-selected-indicator', labels.label);
                    return;
                }
                var header = me.__templates.activeHeader({
                    label: labels.label
                });
                headerContainer.empty();
                headerContainer.append(header);
            });

            if (!classificationOpts) {
                // didn't get classification options so not enough data to classify or other error
                container.find('.edit-legend').hide();
                container.find('.legend-noactive').empty();
                legendContainer.empty();
                container.append(legendUI);
                me._renderDone();
                return;
            }
            var edit = me.__templates.edit({ tooltip: me.locale('classify.edit.open') });
            headerContainer.append(edit);
            me._createEditClassificationListener();
            // legend
            legendContainer.html(legendUI);
            me._renderState.inProgress = false;
            me._renderDone();
            me.trigger('content-rendered');
        }); // _createLegend
    },

    /** **** PRIVATE METHODS ******/
    /**
     * Adds functionality to edit classification button.
     */
    _createEditClassificationListener: function () {
        var me = this;
        this._element.find('.edit-legend').on('click', function (event) {
            // toggle accordion
            me._accordion.getPanels().forEach(function (panel) {
                if (panel.isOpen()) {
                    panel.close();
                    me.trigger('edit-legend', false);
                } else {
                    panel.open();
                    me.trigger('edit-legend', true);
                }
            });
        });
    },
    /**
     * Triggers a new render when needed (if render was called before previous was finished)
     */
    _renderDone: function () {
        var me = this;
        if (me._renderQueue.length) {
            let render = me._renderQueue.shift();
            me.render(render.el, render.event);
        }
        var state = this._renderState;
        this._renderState = {};
        this._restorePanelState(this._accordion, state.panels);
        // trigger an event in case something needs to know that we just completed rendering
        this.trigger('rendered');
    },
    /**
     * Restores legend/classification panels to given state (open/closed)
     * @param  {Oskari.userinterface.component.Accordion} accordion
     * @param  {Object} state     with keys as panel titles and value as boolean (true == open, false == closed)
     */
    _restorePanelState: function (accordion, state) {
        if (!accordion || !state) {
            return;
        }
        var panels = accordion.getPanels();
        panels.forEach(function (panel) {
            var panelState = state[panel.getTitle()];
            if (typeof panelState !== 'boolean') {
                return;
            }
            if (panelState) {
                panel.open();
            } else {
                panel.close();
            }
        });
    },
    /**
     * Creates an accordion panel for legend and classification edit with eventlisteners on open/close
     * @param  {String} title UI label
     * @return {Oskari.userinterface.component.AccordionPanel} panel without content
     */
    _createAccordionPanel: function (title) {
        var me = this;
        var panel = Oskari.clazz.create('Oskari.userinterface.component.AccordionPanel');
        panel.on('open', function () {
            me._setPanelState(panel);
            var legend = me._element.find('.edit-legend');
            legend.addClass('edit-active');
            legend.prop('title', me.locale('classify.edit.close'));
        });
        panel.on('close', function () {
            me._setPanelState(panel);
            var legend = me._element.find('.edit-legend');
            legend.removeClass('edit-active');
            legend.prop('title', me.locale('classify.edit.open'));
        });
        panel.setTitle(title);
        panel.getHeader().remove();
        return panel;
    },
    /**
     * Used to track accordion panel states (open/close)
     * @param {Oskari.userinterface.component.AccordionPanel} panel panel that switched state
     */
    _setPanelState: function (panel) {
        if (!this._renderState.panels) {
            this._renderState.panels = {};
        }
        this._renderState.panels[panel.getTitle()] = panel.isOpen();
    },
    _getLabels: function (activeIndicator, callback) {
        var sourceUILabel = this.locale('statsgrid.source');
        var stateService = this.service.getStateService();

        this.service.getUILabels(activeIndicator, function (labels) {
            var modifiedLabels = {
                source: sourceUILabel + ' ' + (stateService.getIndicatorIndex(activeIndicator.hash) + 1),
                link: '',
                label: labels.full
            };
            callback(modifiedLabels);
        });
    },
    /**
     * Creates the color <> number range UI
     * @param  {Object}   activeIndicator identifies the current active indicator
     * @param  {Function} callback        function to call with legend element as param or undefined for error
     */
    _createLegend: function (activeIndicator, callback) {
        if (!this.service) {
            callback();
            return;
        }
        var me = this;
        var service = this.service;
        var stateService = this.service.getStateService();
        var currentRegionset = stateService.getRegionset();
        var locale = this.locale;

        this.service.getIndicatorData(activeIndicator.datasource, activeIndicator.indicator,
            activeIndicator.selections, activeIndicator.series, currentRegionset, function (err, data) {
                if (err) {
                    me.log.warn('Error getting indicator data', activeIndicator, currentRegionset);
                    callback(me.__templates.error({ msg: locale('legend.noData') }));
                    return;
                }
                if (!data) {
                    me.log.warn('Error getting indicator classification', data);
                    callback(me.__templates.error({ msg: locale('legend.noData') }));
                    return;
                }
                var classificationOpts = stateService.getClassificationOpts(activeIndicator.hash);
                var groupStats = service.getSeriesService().getSeriesStats(activeIndicator.hash);
                var classification = service.getClassificationService().getClassification(data, classificationOpts, groupStats);
                if (!classification) {
                    me.log.warn('Error getting indicator classification', data);
                    callback(me.__templates.error({ msg: locale('legend.noEnough') }));
                    return;
                }
                if (classificationOpts.count !== classification.getGroups().length) {
                    // classification count changed!! -> show error + re-render
                    classificationOpts.count = classification.getGroups().length;
                    callback(me.__templates.error({ msg: locale('legend.noEnough') }));
                    stateService.setClassification(activeIndicator.hash, classificationOpts);
                    return;
                }
                var colors = service.getColorService().getColorsForClassification(classificationOpts, true);
                var legend = classification.createLegend(colors);

                if (!legend) {
                    legend = '<div>' + locale('legend.cannotCreateLegend') + '</div>';
                }
                callback(legend, classificationOpts);
            });
    },
    _updateLegend: function () {
        var state = this.service.getStateService();
        var ind = state.getActiveIndicator();
        var legendElement = this._element.find('.active-legend');
        legendElement.empty();

        this._createLegend(ind, function (legend) {
            legendElement.append(legend);
        });
    },
    /**
     * Creates the classification editor UI
     * @param  {Object}   options  values for the classification form to use as initial values
     * @param  {Function} callback function to call with editpr element as param or undefined for error
     */
    _createClassificationUI: function (options, callback) {
        var element = this.editClassification.getElement();
        this.editClassification.setValues(options);
        callback(element);
    },
    /**
     * Listen to events that require re-rendering the UI
     */
    _bindToEvents: function () {
        var me = this;

        me.service.on('StatsGrid.IndicatorEvent', function (event) {
            // if indicator is removed/added - recalculate the source 1/2 etc links
            me.render(null, event);
        });

        me.service.on('StatsGrid.ActiveIndicatorChangedEvent', function (event) {
            // Always show the active indicator - also handles "no indicator selected"
            // if the selected indicator has no data & edit panel is open -> close it
            var current = event.current;
            if (current) {
                me.service.getIndicatorData(current.datasource, current.indicator,
                    current.selections, current.series, me.service.getStateService().getRegionset(), function (err, data) {
                        if (err) {}
                        if (!data) {
                            me._accordion.getPanels().forEach(function (panel) {
                                if (panel.isOpen()) {
                                    panel.close();
                                }
                            });
                        };
                    });
            }
            me.render(null, event);
        });

        me.service.on('StatsGrid.RegionsetChangedEvent', function (event) {
            // need to update the legend as data changes when regionset changes
            me.render(null, event);
        });

        me.service.on('StatsGrid.ClassificationChangedEvent', function (event) {
            me._updateLegend();
        });
    }
});
