package fi.nls.oskari.spring.security;

import fi.nls.oskari.control.ActionParameters;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import org.oskari.log.AuditLog;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Simple extension for getting the login failure exception logged
 */
public class OskariLoginFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private Logger log = LogFactory.getLogger(OskariLoginFailureHandler.class);

    public OskariLoginFailureHandler() {
        super();
    }

    public OskariLoginFailureHandler(String url) {
        super(url);
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        // just log the error for now
        log.error(exception, "Login failed!");
        super.onAuthenticationFailure(request, response, exception);
        AuditLog.guest(ActionParameters.getClientIp(request))
                .withMsg("Login")
                .withParam("ex", exception.getMessage())
                .errored(AuditLog.ResourceType.USER);
    }
}
