package com.google.appinventor.server.components;

import com.google.appinventor.server.OdeRemoteServiceServlet;
import com.firebase.security.token.TokenGenerator;
import com.google.appinventor.shared.rpc.components.FirebaseAuthService;

import java.util.HashMap;
import java.util.Map;

/**
 * Servlet for the Firebase Authentication RPC.
 *
 * @author William Byrne
 */
public class FirebaseAuthServiceImpl extends OdeRemoteServiceServlet
    implements FirebaseAuthService {

  /**
   * Creates a JSON Web Token (JWT) to authenticate an App Inventor App
   * using the default App Inventor Firebase Account.
   *
   * @param  developer the App Inventor user who created the Firebase App
   * @param  project   the App Inventor project using the Firebase component
   * @return a JWT containing developer and project
   *         information for the Firebase App
   */
  public String getToken(String developer, String project) {
    Map<String, Object> payload = new HashMap<String, Object>();
    payload.put("developer", developer);
    payload.put("project", project);

    // Create a TokenGenerator with the App Inventor Firebase Secret
    TokenGenerator tokenGen = new TokenGenerator("hOUUPiTkuKkJLg2nG4wEoWfF6eGaf0dV1ZQETUvp");
    return tokenGen.createToken(payload); // return a JWT containing the payload
  }
}
