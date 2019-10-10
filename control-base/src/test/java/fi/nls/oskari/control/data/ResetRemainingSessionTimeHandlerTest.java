package fi.nls.oskari.control.data;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import fi.nls.oskari.control.ActionParameters;
import fi.nls.oskari.control.session.ResetRemainingSessionTimeHandler;
import fi.nls.test.control.JSONActionRouteTest;
import fi.nls.test.util.ResourceHelper;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ResetRemainingSessionTimeHandler.class})
public class ResetRemainingSessionTimeHandlerTest extends JSONActionRouteTest {
    
    private static final int MAX_INACTIVE_INTERVAL = 1800;
    private static final long NOW = System.currentTimeMillis();
    private static final long FIVE_SECONDS_AGO = NOW - 5000;
    private ResetRemainingSessionTimeHandler handler = new ResetRemainingSessionTimeHandler();
   
    @Test
    public void testResponseSessionExists() throws Exception {
        
        ActionParameters params = createActionParams();
        
        PowerMockito.mockStatic(System.class);
        when(System.currentTimeMillis()).thenReturn(NOW);
        when(params.getRequest().getSession().getMaxInactiveInterval()).thenReturn(MAX_INACTIVE_INTERVAL);
        when(params.getRequest().getSession().getLastAccessedTime()).thenReturn(FIVE_SECONDS_AGO);
        handler.handleAction(params);
        verifyResponseContent(ResourceHelper
                .readJSONResource("ResetRemainingSessionTimeHandlerTest-session-response-expected.json", this));
    }

    @Test
    public void testResponseNoSession() throws Exception {
        ActionParameters params = createActionParams();
        doReturn(null).when(params.getRequest()).getSession(false);
        handler.handleAction(params);
        verifyResponseContent(ResourceHelper
                .readJSONResource("ResetRemainingSessionTimeHandlerTest-no-session-response-expected.json", this));
    }
    
    
}
