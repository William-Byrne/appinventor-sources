package com.google.appinventor.shared.rpc.components;

import com.google.appinventor.shared.rpc.ServerLayout;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

/**
 * Service interface for the Firebase Authentication RPC.
 *
 * @author William Byrne
 */
@RemoteServiceRelativePath(ServerLayout.FIREBASE_AUTH_SERVICE)
public interface FirebaseAuthService extends RemoteService {

  String getToken(String developer, String project);
}
