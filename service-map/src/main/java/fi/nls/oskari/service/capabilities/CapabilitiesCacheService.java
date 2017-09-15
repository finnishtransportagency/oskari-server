package fi.nls.oskari.service.capabilities;

import fi.nls.oskari.domain.map.OskariLayer;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.service.OskariComponent;
import fi.nls.oskari.service.ServiceException;
import fi.nls.oskari.util.IOHelper;
import fi.nls.oskari.util.PropertyUtil;
import fi.nls.oskari.util.XmlHelper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;
import javax.xml.stream.FactoryConfigurationError;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

public abstract class CapabilitiesCacheService extends OskariComponent {

    private static final Logger LOG = LogFactory.getLogger(CapabilitiesCacheService.class);
    private static final Map<String, String> TYPE_MAPPING = new HashMap<>(5);
    static {
        TYPE_MAPPING.put(OskariLayer.TYPE_WMS, "WMS");
        TYPE_MAPPING.put(OskariLayer.TYPE_WFS, "WFS");
        TYPE_MAPPING.put(OskariLayer.TYPE_WMTS, "WMTS");
    }
    // timeout capabilities request after 30 seconds (configurable)
    private static final String PROP_TIMEOUT = "capabilities.timeout";
    private static final int TIMEOUT_SECONDS = PropertyUtil.getOptional(PROP_TIMEOUT, 30);
    private static final int TIMEOUT_MS = TIMEOUT_SECONDS * 1000;

    private static final String NAMESPACE_WMS = "http://www.opengis.net/wms/";
    private static final String NAMESPACE_WFS = "http://www.opengis.net/wfs/";
    private static final String NAMESPACE_WMTS = "http://www.opengis.net/wmts/";

    private static final String ROOT_WMS_LESS_THAN_130 = "WMT_MS_Capabilities";
    private static final String ROOT_WMS_130 = "WMS_Capabilities";
    private static final String ROOT_WFS = "WFS_Capabilities";
    private static final String ROOT_WMTS = "Capabilities";

    public abstract OskariLayerCapabilities find(final String url, final String layertype, final String version);
    public abstract OskariLayerCapabilities save(final OskariLayerCapabilities capabilities);

    public OskariLayerCapabilities getCapabilities(String url, String type, String version)
            throws ServiceException {
        return getCapabilities(url, type, null, null, version);
    }

    public OskariLayerCapabilities getCapabilities(String url, String type, final String user, final String passwd, final String version)
            throws ServiceException {
        return getCapabilities(url, type, user, passwd, version, false);
    }

    public OskariLayerCapabilities getCapabilities(String url, String type, final String user, final String passwd, final String version, final boolean loadFromService)
            throws ServiceException {
        return getCapabilities(createTempOskariLayer(url, type, user, passwd, version), loadFromService);
    }

    private OskariLayer createTempOskariLayer(String url, String type, final String user, final String passwd, final String version) {
        OskariLayer layer = new OskariLayer();
        layer.setUrl(url);
        layer.setType(type);
        layer.setVersion(version);
        layer.setUsername(user);
        layer.setPassword(passwd);
        return layer;
    }

    public OskariLayerCapabilities getCapabilities(final OskariLayer layer)
            throws ServiceException {
        // prefer saved db version over network call by default
        return getCapabilities(layer, false);
    }

    public OskariLayerCapabilities getCapabilities(final OskariLayer layer, final boolean loadFromService)
            throws ServiceException {
        final String url = layer.getSimplifiedUrl(true);
        final String type = layer.getType();
        final String version = layer.getVersion();

        if (!loadFromService) {
            OskariLayerCapabilities dbCapabilities = find(url, type, version);
            if (dbCapabilities != null) {
                return dbCapabilities;
            }
        }

        // try to get xml from service
        final String data = loadCapabilitiesFromService(layer);

        final OskariLayerCapabilities draft = new OskariLayerCapabilities(url, type, version, data);
        return save(draft);
    }

    public static String loadCapabilitiesFromService(OskariLayer layer) throws ServiceException {
        final String url = contructCapabilitiesUrl(layer);
        if (url.isEmpty()) {
            return null;
        }

        String encoding = null;
        byte[] data = null;
        try {
            final HttpURLConnection conn = IOHelper.getConnection(url, layer.getUsername(), layer.getPassword());
            conn.setReadTimeout(TIMEOUT_MS);

            final int sc = conn.getResponseCode();
            if (sc != HttpURLConnection.HTTP_OK) {
                LOG.warn("Unexpected Status code:", sc, " url:", url);
                throw new ServiceException("Unexpected Status code: " + sc);
            }

            final String contentType = conn.getContentType();
            if (contentType != null && contentType.toLowerCase().indexOf("xml") == -1) {
                // not xml based on contentType
                LOG.warn("Unexpected Content-Type:", contentType, "url:", url);
                throw new ServiceException("Unexpected Content-Typee: " + contentType);
            }

            encoding = IOHelper.getCharset(conn);
            data = IOHelper.readBytes(conn);
        } catch (IOException e) {
            LOG.warn(e, "IOException occured, url:", url);
            throw new ServiceException("IOException occured", e);
        }

        try {
            XMLInputFactory xif = XMLInputFactory.newInstance();
            xif.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, true);
            XMLStreamReader xsr = xif.createXMLStreamReader(new ByteArrayInputStream(data));

            // Check XML prolog for character encoding
            String xmlEncoding = xsr.getCharacterEncodingScheme();
            if (xmlEncoding != null) {
                if (encoding != null && !xmlEncoding.equalsIgnoreCase(encoding)) {
                    LOG.warn("Content-Type header specified a different encoding than XML prolog!");
                    throw new ServiceException("Content-Type header specified a different encoding than XML prolog!");
                }
                encoding = xmlEncoding;
            }
            if (encoding == null) {
                LOG.debug("Charset wasn't set on either the Content-Type or the XML prolog"
                        + "using UTF-8 as default value");
                encoding = IOHelper.DEFAULT_CHARSET;
            }

            // Check that the response is what we expect
            checkCapabilities(xsr, layer.getType(), layer.getVersion());

            // Convert "utf-8" to "UTF-8" for example
            encoding = encoding.toUpperCase();
            String xml = new String(data, encoding);
            // Strip the potential prolog from XML so that we
            // don't have to worry about the specified charset
            return XmlHelper.stripPrologFromXML(xml);
        } catch (FactoryConfigurationError | XMLStreamException e) {
            LOG.warn(e, "Failed to parse XML from response");
        } catch (UnsupportedEncodingException e) {
            LOG.warn(e, "Failed to Encode byte[] to String encoding:", encoding);
        }
        return null;
    }

    public static String contructCapabilitiesUrl(final OskariLayer layer) {
        if (layer == null) {
            return "";
        }

        final String url = layer.getSimplifiedUrl(true);
        final String urlLC = url.toLowerCase();
        final String serviceType = TYPE_MAPPING.get(layer.getType());

        final Map<String, String> params = new HashMap<String, String>();
        // check existing params
        if (!urlLC.contains("service=")) {
            params.put("service", serviceType);
        }
        if (!urlLC.contains("request=")) {
            params.put("request", "GetCapabilities");
        }
        if (!urlLC.contains("version=") && layer.getVersion() != null) {
            params.put(getVersionNegotiationKey(serviceType), layer.getVersion());
        }

        return IOHelper.constructUrl(url, params);
    }

    private static String getVersionNegotiationKey(String service) {
        if (service != null) {
            switch (service) {
            case "WMS":
                return "version";
            case "WFS":
            case "WMTS":
                return "acceptVersions";
            }
        }
        return "";
    }

    private static void checkCapabilities(XMLStreamReader xsr, String type, String version)
            throws ServiceException {
        advanceToRootElement(xsr);
        String ns = xsr.getNamespaceURI();
        String name = xsr.getLocalName();
        validateCapabilities(type, version, ns, name);
    }

    private static boolean advanceToRootElement(XMLStreamReader xsr)
            throws ServiceException {
        try {
            if (xsr.nextTag() != XMLStreamConstants.START_DOCUMENT) {
                throw new ServiceException("Document did not start with a START_DOCUMENT!");
            }
            if (xsr.nextTag() != XMLStreamConstants.START_ELEMENT) {
                throw new ServiceException("Could not find root element!");
            }
            return true;
        } catch (XMLStreamException e) {
            throw new ServiceException("XMLStreamException occured!", e);
        }
    }

    private static void validateCapabilities(String type, String version, String ns, String name)
            throws ServiceException {
        LOG.debug("Validating capabilities, type:", type, "version:", version,
                "namespace", ns, "root element", name);

        switch (type) {
        case OskariLayer.TYPE_WMS:
            if (version == null) {
                // Layer didn't specify a version - response could be of any WMS version
                if (ns != null) {
                    // Can only be 1.3.0
                    checkNamespaceStartsWith(ns, NAMESPACE_WMS);
                    checkRootElementNameEquals(name, ROOT_WMS_130);
                } else {
                    checkRootElementNameEquals(name, ROOT_WMS_LESS_THAN_130);
                }
            } else if ("1.3.0".equals(version)) {
                checkNamespaceStartsWith(ns, NAMESPACE_WMS);
                checkRootElementNameEquals(name, ROOT_WMS_130);
            } else {
                checkRootElementNameEquals(name, ROOT_WMS_LESS_THAN_130);
            }
            break;
        case OskariLayer.TYPE_WFS:
            checkNamespaceStartsWith(ns, NAMESPACE_WFS);
            checkRootElementNameEquals(name, ROOT_WFS);
            break;
        case OskariLayer.TYPE_WMTS:
            checkNamespaceStartsWith(ns, NAMESPACE_WMTS);
            checkRootElementNameEquals(name, ROOT_WMTS);
            break;
        }
    }

    private static void checkNamespaceStartsWith(final String ns, final String expected)
            throws ServiceException {
        if (ns == null) {
            throw new ServiceException("Expected non-null namespace!");
        }
        if (!ns.startsWith(expected)) {
            throw new ServiceException(String.format(
                    "Expected namespace starting with '%s', got '%s'", expected, ns));
        }
    }

    private static void checkRootElementNameEquals(final String name, final String expected)
            throws ServiceException {
        if (!expected.equals(name)) {
            throw new ServiceException(String.format(
                    "Expected root element with name '%s', got '%s'", expected, name));
        }
    }

}