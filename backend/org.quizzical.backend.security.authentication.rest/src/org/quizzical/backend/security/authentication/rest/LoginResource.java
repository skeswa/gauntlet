package org.quizzical.backend.security.authentication.rest;

import java.io.IOException;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

import org.gauntlet.core.api.ApplicationException;
import org.gauntlet.core.commons.util.Validator;
import org.gauntlet.quizzes.api.dao.IQuizDAOService;
import org.osgi.service.log.LogService;
import org.quizzical.backend.security.authentication.jwt.api.IJWTTokenService;
import org.quizzical.backend.security.authentication.jwt.api.SessionUser;
import org.quizzical.backend.security.authorization.api.dao.user.IUserDAOService;
import org.quizzical.backend.security.authorization.api.dao.user.UserNotFoundException;
import org.quizzical.backend.security.authorization.api.model.user.User;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Path("auth")
public class LoginResource {
    private static final String IPADDRESS_PATTERN =
		"^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
		"([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
		"([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
		"([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";
    private Pattern pattern;
    
	private volatile IUserDAOService userService;
	private volatile IJWTTokenService tokenService;
	private volatile IQuizDAOService quizService;
	private volatile LogService logger;
	
	private void init() {
		pattern = Pattern.compile(IPADDRESS_PATTERN);
	}

	@Path("login")
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	public Response login(final @Context HttpServletRequest request, final Login login) throws ApplicationException {
		try {
			
			if (Validator.isNull(login))
				return Response.status(401).build();	

			final User user = userService.getUserByEmailAndPassword(login.getUsername(), login.getPassword());
			final SessionUser sessionUser = new SessionUser(user);
			final String token = tokenService.generateToken(sessionUser);
			
			if (user.getQa() || user.getAdmin())
				sessionUser.setDignosed(true);
			else {
				Boolean isDiagnosed = quizService.userHasTakenDiagnoticTest(user);
				sessionUser.setDignosed(isDiagnosed);
			}

			return Response.ok().entity(sessionUser).header("Authorization","Basic " + token).build();
		} catch(final UserNotFoundException ex) {
			return Response.status(401).build();
		} catch (JsonProcessingException e) {
			return Response.status(401).build();
		} 
	}

	@Path("whoami")
	@Produces(MediaType.APPLICATION_JSON)
	@GET
	public Response me(@Context HttpServletRequest request) throws UserNotFoundException, ApplicationException {
		try {
			final String userJson = tokenService.extractSessionUserAsJson(request);
			if (userJson == null || "null".equalsIgnoreCase(userJson))
				return Response.status(403).build();
			return Response.ok().entity(userJson).build();
		} catch (JsonProcessingException e) {
			return Response.status(401).build();
		} catch (IOException e) {
			return Response.status(401).build();
		} 
	}

	@Path("logout")
	@POST
	public Response logout(@Context HttpServletRequest request) {
		final String domain = createCookieDomain(request);
		NewCookie cookie = new NewCookie(IJWTTokenService.COOKIE_NAME, null, "/", domain, "Authentication cookie", 0, false);
		return Response.ok().cookie(cookie).build();
	}
	

	private String createCookieDomain(HttpServletRequest request) {
		String domain = null;
		if (pattern.matcher(request.getServerName()).matches()) {
			domain = request.getServerName();
		}
		else {
			domain = "."+request.getServerName().replaceAll(".*\\.(?=.*\\.)", "");
		}
		return domain;
	}
}