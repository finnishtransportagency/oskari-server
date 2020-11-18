package fi.nls.oskari.control.layer;

import fi.nls.oskari.control.ActionDeniedException;
import fi.nls.oskari.control.ActionException;
import fi.nls.oskari.control.ActionParameters;
import fi.nls.oskari.control.ActionParamsException;
import fi.nls.oskari.domain.User;
import fi.nls.oskari.service.ServiceException;
import fi.nls.oskari.service.capabilities.CapabilitiesCacheService;
import fi.nls.oskari.util.DuplicateException;
import fi.nls.oskari.util.PropertyUtil;
import fi.nls.test.control.JSONActionRouteTest;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;

import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;


/**
 * @author SMAKINEN
 */
@RunWith(PowerMockRunner.class)
// these are needed with PowerMock and Java 11. Haven't tried if Java 13+ still needs these:
// https://github.com/powermock/powermock/issues/864
@PowerMockIgnore({"com.sun.org.apache.xalan.*", "com.sun.org.apache.xerces.*", "javax.xml.*", "org.w3c.dom.*", "org.xml.*", "com.sun.org.apache.xml.*"})
public class GetWSCapabilitiesHandlerTest extends JSONActionRouteTest {

    final private  GetWSCapabilitiesHandler handler = new  GetWSCapabilitiesHandler(mock(CapabilitiesCacheService.class));

    @BeforeClass
    public static void addLocales() throws Exception {
        PropertyUtil.clearProperties();
        Properties properties = new Properties();
        try {
            properties.load(GetWSCapabilitiesHandlerTest.class.getResourceAsStream("test.properties"));
            PropertyUtil.addProperties(properties);
            String locales = PropertyUtil.getNecessary("oskari.locales");
            if (locales == null)
                fail("No darned locales");
        } catch (DuplicateException e) {
            fail("Should not throw exception" + e.getStackTrace());
        }
    }
    @AfterClass
    public static void teardown() {
        PropertyUtil.clearProperties();
    }

    @Before
    public void setUp() throws Exception {
        handler.init();
    }

    @Test(expected = ActionDeniedException.class)
    public void testHandleActionGuestUser() throws Exception {

        // setup params
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("url", "dummyurl");

        final ActionParameters params = createActionParams(parameters);
        handler.handleAction(params);

        fail("Should not get his far with guest user");
    }

    @Test(expected = ActionDeniedException.class)
    public void testHandleActionInvalidUser() throws Exception {

        // setup params
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("url", "dummyurl");
        final User user = new User();
        user.addRole(1, "test role");
        final ActionParameters params = createActionParams(parameters);
        handler.handleAction(params);

        fail("Should not get his far with user without configured roles");
    }

    @Test
    public void testHandleActionWithConfiguredRole() throws Exception {

        // users with 'test role' or 'my role' should have access
        PropertyUtil.addProperty("actionhandler.GetWSCapabilitiesHandler.roles", "test role, my role", true);
        // re-init to get updated property
        handler.init();

        // setup params
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("url", "dummyurl");
        final User user = new User();
        user.addRole(1, "test role");
        final ActionParameters params = createActionParams(parameters, user);
        try {
            handler.handleAction(params);
        }
        catch(ActionException ex) {
            // FIXME: we should mock the static class and do some better handling for this
            // anyway we get close enough for now by checking for MalformedURLException...
            // PowerMockito.mockStatic(GetWMSCapabilities.class);
            // when(GetWMSCapabilities.getResponse(anyString())).thenReturn("Hello!");
            assertTrue(ex.getCause() instanceof ServiceException);
            assertTrue(ex.getCause().getCause() instanceof MalformedURLException);
        }
    }
    @Test(expected = ActionParamsException.class)
    public void testHandleActionInvalidParams() throws Exception {
        // users with 'test role' or 'my role' should have access
        PropertyUtil.addProperty("actionhandler.GetWSCapabilitiesHandler.roles", "test role, my role", true);
        // re-init to get updated property
        handler.init();

        final User user = new User();
        user.addRole(1, "test role");
        final ActionParameters params = createActionParams(user);
        handler.handleAction(params);

        fail("Should not get his far without url parameter");
    }
}
