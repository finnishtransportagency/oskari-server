/**
 * @class Oskari.mapframework.bundle.myplacesimport.StyleForm
 *
 * Shows a form for a user layer style
 */
Oskari.clazz.define(
    'Oskari.mapframework.bundle.myplacesimport.StyleForm',

    /**
     * @static @method create called automatically on construction
     *
     *
     */
    function (instance) {
        this.instance = instance;
        this.loc = Oskari.getMsg.bind(null, 'MyPlacesImport');

        this.visualizationForm = Oskari.clazz.create(
            'Oskari.userinterface.component.VisualizationForm'
        );
        this.template = jQuery(
            '<div class="myplacesimportstyleform">' +
            '   <div class="field name">' +
            '       <label for="userlayername">' + this.loc('flyout.layer.name') + '</label><br clear="all" />' +
            '       <input type="text" data-name="userlayername" />' +
            '   </div>' +
            '   <div class="field desc">' +
            '       <label for="description">' + this.loc('flyout.layer.desc') + '</label><br clear="all" />' +
            '       <input type="text" data-name="userlayerdesc" />' +
            '   </div>' +
            '   <div class="field source">' +
            '       <label for ="datasource">' + this.loc('flyout.layer.source') + '</label><br clear="all" />' +
            '       <input type="text" data-name="userlayersource" />' +
            '   </div>' +
            '   <div class="field visualization">' +
                    '<label for=style>' + this.loc('flyout.layer.style') + '</label><br clear="all" />' +
                    '<div class="rendering"></div>' +
            '   </div>' +
            '</div>');
    }, {
        start: function () {},

        /**
         * @method getForm
         * @return {jQuery} jquery reference for the form
         */
        getForm: function () {
            var ui = this.template.clone();
            // populate the rendering fields
            var content = ui.find('div.rendering');
            content.append(this.visualizationForm.getForm());

            return ui;
        },

        /**
         * @method setStyleValues
         * @param {Object} style
         */
        setStyleValues: function (style) {
            style.fill.color = (typeof style.fill.color === 'string' ? style.fill.color : null);
            style.stroke.area.color = (typeof style.stroke.area.color === 'string' ? style.stroke.area.color : null);
            this.visualizationForm.setOskariStyleValues(style);
        },

        /**
         * Returns form values as an object
         *
         * @method getValues
         * @return {Object} values
         */
        getValues: function () {
            var values = {};
            var me = this;
            // infobox will make us lose our reference so search
            // from document using the form-class
            var onScreenForm = this._getOnScreenForm();

            if (onScreenForm.length > 0) {
                values.name = onScreenForm.find('input[data-name=userlayername]').val();
                values.desc = onScreenForm.find('input[data-name=userlayerdesc]').val();
                values.source = onScreenForm.find('input[data-name=userlayersource]').val();
            }
            values.style = JSON.stringify(me.visualizationForm.getOskariStyle());

            return values;
        },
        /**
         * Returns reference to the on screen version shown by OpenLayers
         *
         * @method _getOnScreenForm
         * @private
         */
        _getOnScreenForm: function () {
            return jQuery('div.myplacesimportstyleform');
        }

    });
