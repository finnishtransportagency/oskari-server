package org.oskari.print.util;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.fop.svg.PDFTranscoder;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.multipdf.LayerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPatternContentStream;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.pdfbox.pdmodel.graphics.color.PDPattern;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.pattern.PDTilingPattern;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.oskari.print.PDF;
import org.oskari.print.request.PDPrintStyle;
import org.oskari.print.request.PDPrintStyle.LineCap;
import org.oskari.print.request.PDPrintStyle.LineJoin;
import org.oskari.print.request.PDPrintStyle.LinePattern;

import fi.nls.oskari.util.IOHelper;
import fi.nls.oskari.util.JSONHelper;

public class StyleUtil {

    private static final String SVG_MARKERS_JSON = "svg-markers.json";
    private static final String ICON_STROKE_COLOR = "#b4b4b4";
    private static final float ICON_SIZE = 32f;
    private static final double ICON_OFFSET = ICON_SIZE/2.0;

    public static final Map<String, PDPrintStyle.LabelAlign> LABEL_ALIGN_MAP = new HashMap<String, PDPrintStyle.LabelAlign>() {{
        put("markers", new PDPrintStyle.LabelAlign("left", 12f, 8f));
    }};

    public static PDPrintStyle getLineStyle (JSONObject oskariStyle) {
        JSONObject stroke = oskariStyle.optJSONObject("stroke");

        PDPrintStyle style = new PDPrintStyle();
        setStrokeStyle(style, stroke);
        setLabelStyle(style, oskariStyle);
        return style;
    }

    public static PDPrintStyle getPolygonStyle (JSONObject oskariStyle, PDResources resources) throws IOException {
        JSONObject stroke = oskariStyle.optJSONObject("stroke");
        if (stroke != null) {
            stroke = stroke.optJSONObject("area");
        }

        PDPrintStyle style = new PDPrintStyle();
        setStrokeStyle(style, stroke);
        setFillStyle(style, oskariStyle, resources);
        setLabelStyle(style, oskariStyle);
        return style;
    }

    public static PDPrintStyle getPointStyle (JSONObject oskariStyle, PDDocument doc) throws IOException {
        PDPrintStyle style = new PDPrintStyle();
        setImageStyle(style, oskariStyle, doc);
        setLabelStyle(style, oskariStyle);
        return style;
    }

    private static void setStrokeStyle(PDPrintStyle style, JSONObject stroke) {
        if (stroke == null) {
            return;
        }

        float lineWidth = (float) stroke.optDouble("width", -1);
        if (lineWidth > 0) {
            style.setLineWidth(lineWidth);
        }
        LineJoin lineJoin = LineJoin.get(JSONHelper.optString(stroke, "lineJoin"));
        if (lineJoin != null) {
            style.setLineJoin(lineJoin);
        }
        LineCap lineCap = LineCap.get(JSONHelper.optString(stroke, "lineCap"));
        if (lineCap != null) {
            style.setLineCap(lineCap);
        }
        LinePattern linePattern = LinePattern.get(JSONHelper.optString(stroke, "lineDash"));
        if (linePattern != null) {
            style.setLinePattern(linePattern);
        }
        Color color = ColorUtil.parseColor(JSONHelper.optString(stroke, "color"));
        if (color != null) {
            style.setFillColor(color);
        }
    }

    private static void setFillStyle(PDPrintStyle style, JSONObject oskariStyle, PDResources resources) throws IOException {
        JSONObject fill = oskariStyle.optJSONObject("fill");
        if (fill == null) {
            return;
        }

        Color color = ColorUtil.parseColor(JSONHelper.optString(fill, "color"));
        if (color == null) {
            return;
        }
        JSONObject fillArea = fill.optJSONObject("area");
        int pattern = fillArea != null ? fillArea.optInt("pattern", -1) : -1;
        if (pattern >= 0 && pattern <= 3) {
            style.setFillColor(createFillPattern(resources, pattern, color));
        } else {
            style.setFillColor(color);
        }
    }

    private static void setImageStyle(PDPrintStyle style, JSONObject oskariStyle, PDDocument doc) throws IOException {
        JSONObject image = oskariStyle.optJSONObject("image");
        if (image == null) {
            return;
        }

        JSONObject fill = image.optJSONObject("fill");
        String color = fill != null ? JSONHelper.optString(fill, "color") : null;
        if (color == null) {
            return;
        }
        style.setFillColor(ColorUtil.parseColor(color));

        int shape = image.optInt("shape", 5); // External icons not supported
        int size = image.optInt("size", 3);
        style.setIcon(getIcon(doc, shape, color, size));
    }

    private static void setLabelStyle (PDPrintStyle style, JSONObject oskariStyle) {
        JSONObject text = JSONHelper.getJSONObject(oskariStyle, "text");
        if (text == null) {
            return;
        }
        Object labelProperty = text.opt("labelProperty");
        if (labelProperty instanceof JSONArray) {
            style.setLabelProperty(JSONHelper.getArrayAsList((JSONArray) labelProperty));
        } else if (labelProperty instanceof String){
            style.setLabelProperty(Collections.singletonList((String) labelProperty));
        }
        int offsetX = text.optInt("offsetX", 0);
        int offsetY = text.optInt("offsetY", 0);
        String align = text.optString("textAlign");
        style.setLabelAlign(new PDPrintStyle.LabelAlign(align, offsetX, offsetY));
    }
    public static PDFormXObject getIcon (PDDocument doc, int shape, String fillColor, int size) throws IOException {
        try {
            JSONObject marker = getMarker(shape);
            return createIcon(doc, marker, fillColor, size);
        } catch (Exception e) {
            throw new IOException ("Failed to create marker icon: " + shape);
        }
    }

    // TODO: get marker data from EnvHelper
    public static JSONObject getMarker (int index) throws IOException {
        try (InputStream is = PDF.class.getResourceAsStream(SVG_MARKERS_JSON)) {
            if (is == null) {
                throw new IOException("Resource file " + SVG_MARKERS_JSON + " does not exist");
            }
            JSONArray svgMarkers = JSONHelper.createJSONArray(IOHelper.readString(is));
            if (index >= svgMarkers.length()) {
                throw new IOException("SVG marker:" + index + " does not exist");
            }
            return JSONHelper.getJSONObject(svgMarkers, index);
        }
    }

    private static PDFormXObject createIcon (PDDocument doc, JSONObject marker, String fillColor, int size) throws JSONException, IOException, TranscoderException {
        String markerData = JSONHelper.getString(marker, "data").replace("$fill", fillColor).replace("$stroke", ICON_STROKE_COLOR);
        double scale = size < 1 || size > 5 ? 1 : 0.6 +  size /10.0;
        double x =  marker.optDouble("offsetX", ICON_OFFSET) * scale;
        double y = marker.optDouble("offsetY", ICON_OFFSET) * scale;

        PDFTranscoder transcoder = new PDFTranscoder();
        TranscoderInput in = new TranscoderInput(new ByteArrayInputStream(markerData.getBytes()));

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        TranscoderOutput out = new TranscoderOutput(os);
        transcoder.transcode(in, out);
        try (PDDocument tempDoc = PDDocument.load(os.toByteArray())) {
            PDPage page = tempDoc.getPage(0);

            double d = page.getBBox().getHeight() / ICON_SIZE;
            scale = scale / d;

            LayerUtility layerUtil = new LayerUtility(doc);
            PDFormXObject form = layerUtil.importPageAsForm(tempDoc, page);
            form.setMatrix(new AffineTransform(scale, 0, 0, scale, -x, -y ));
            return form;
        }
    }

    private static PDColor createFillPattern(PDResources resources, int fillPattern, Color fillColor) throws IOException {
        PDPattern pattern = new PDPattern(null, PDDeviceRGB.INSTANCE);
        PDTilingPattern tilingPattern = new PDTilingPattern();
        int size = 64;
        tilingPattern.setBBox(new PDRectangle(size,size));
        tilingPattern.setPaintType(PDTilingPattern.PAINT_COLORED);
        tilingPattern.setTilingType(PDTilingPattern.TILING_CONSTANT_SPACING);
        tilingPattern.setXStep(size);
        tilingPattern.setYStep(size);
        COSName patternName = resources.add(tilingPattern);
        // thin 1 thick 2.5
        float lineWidth = fillPattern == 0 || fillPattern == 2 ? 1.0f : 2.5f;
        boolean isHorizontal = fillPattern == 2 || fillPattern == 3;
        float numberOfStripes = 9;
        if (lineWidth > 2) {
            numberOfStripes = isHorizontal ? 8 : 6;
        }
        float bandWidth = size / numberOfStripes;
        try (PDPatternContentStream pcs = new PDPatternContentStream(tilingPattern))
        {
            // Set color, draw diagonal line + 2 more diagonals so that corners look good
            pcs.setStrokingColor(fillColor);
            pcs.setLineWidth(lineWidth);
            pcs.setLineCapStyle(LineCap.square.code);
            float limit = isHorizontal ? numberOfStripes : numberOfStripes * 2;
            for (int i = 0 ; i < limit; i ++) {
                float transition = i * bandWidth+ bandWidth/2;
                if (isHorizontal)  {
                    pcs.moveTo(0, transition);
                    pcs.lineTo(size, transition );
                } else {
                    pcs.moveTo(0, size - transition);
                    pcs.lineTo(transition, size);
                }
                pcs.stroke();
            }
        }
        return new PDColor(patternName, pattern);
    }
}
