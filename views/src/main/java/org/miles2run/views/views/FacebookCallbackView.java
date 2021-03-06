package org.miles2run.views.views;

import facebook4j.Facebook;
import facebook4j.FacebookFactory;
import facebook4j.auth.AccessToken;
import org.apache.commons.lang3.StringUtils;
import org.jug.view.View;
import org.miles2run.core.repositories.jpa.SocialConnectionRepository;
import org.miles2run.core.utils.UrlUtils;
import org.miles2run.domain.entities.SocialConnection;
import org.miles2run.domain.entities.SocialConnectionBuilder;
import org.miles2run.domain.entities.SocialProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;

@Path("/")
public class FacebookCallbackView {

    private Logger logger = LoggerFactory.getLogger(FacebookCallbackView.class);

    @Inject
    private SocialConnectionRepository socialConnectionRepository;
    @Inject
    private FacebookFactory facebookFactory;
    @Context
    private HttpServletRequest request;

    @Path("/facebook/callback")
    @GET
    @Produces("text/html")
    public View callback(@QueryParam("code") String oauthCode, @QueryParam("error") String error) throws Exception {
        logger.info("Facebook Oauth code : {}", oauthCode);
        if (StringUtils.isNotBlank(error) || StringUtils.isBlank(oauthCode)) {
            logger.info("Facebook returned error {}, so redirecting user to index page.", error);
            return View.of("/", true);
        }
        Facebook facebook = facebookFactory.getInstance();
        facebook.setOAuthCallbackURL(UrlUtils.absoluteUrlForResourceMethod(request, FacebookCallbackView.class, "callback"));
        AccessToken oAuthAccessToken = facebook.getOAuthAccessToken(oauthCode);
        String connectionId = facebook.getId();
        SocialConnection existingSocialConnection = socialConnectionRepository.findByConnectionId(connectionId);
        if (existingSocialConnection != null) {
            if (existingSocialConnection.getProfile() == null) {
                logger.info("User already has authenticated with Facebook but profile creation was not finished so redirecting user to new profile creation page.");
                return View.of("/users/new?connectionId=" + connectionId, true);
            } else {
                String username = existingSocialConnection.getProfile().getUsername();
                logger.info("User {} already had authenticated with facebook and has a valid profile so redirecting to home.", username);
                HttpSession session = request.getSession();
                session.setAttribute("principal", username);
                return View.of("/", true);
            }
        }
        logger.info("User does not have social connection with us. So, creating a new SocialConnection and redirecting user to profile creation page.");
        SocialConnection socialConnection = new SocialConnectionBuilder().setAccessToken(oAuthAccessToken.getToken()).setAccessSecret(null).setProvider(SocialProvider.FACEBOOK).setHandle(facebook.users().getMe().getUsername()).setConnectionId(connectionId).createSocialConnection();
        socialConnectionRepository.save(socialConnection);
        return View.of("/users/new?connectionId=" + connectionId, true);
    }
}
