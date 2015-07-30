package com.google.appinventor.client.editor.simple.components;

import com.google.appinventor.client.Ode;
import com.google.appinventor.client.editor.simple.SimpleEditor;
import com.google.appinventor.client.output.OdeLog;
import com.google.appinventor.shared.rpc.components.FirebaseAuthService;
import com.google.appinventor.shared.rpc.components.FirebaseAuthServiceAsync;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;

/**
 * Mock for the non-visible FirebaseDB component. This needs a separate mock
 * from other non-visible components so that some of its properties can be
 * given dynamic default values.
 *
 * @author will2596@gmail.com (William Byrne)
 */
public class MockFirebaseDB extends MockNonVisibleComponent {

  public static final String TYPE = "FirebaseDB";
  private static final String PROPERTY_NAME_DEVELOPER_BUCKET = "DeveloperBucket";
  private static final String PROPERTY_NAME_PROJECT_BUCKET = "ProjectBucket";
  private static final String PROPERTY_NAME_FIREBASE_TOKEN = "FirebaseToken";
  private static final Ode ODE = Ode.getInstance();
  private static final FirebaseAuthServiceAsync AUTH_SVC = GWT.create(FirebaseAuthService.class);

  /**
   * Creates a new instance of a non-visible component whose icon is
   * loaded dynamically (not part of the icon image bundle)
   *
   * @param editor
   * @param type
   * @param iconImage
   */
  public MockFirebaseDB(SimpleEditor editor, String type, Image iconImage) {
    super(editor, type, iconImage);
  }

  /**
   * Initializes the "ProjectBucket", "DeveloperBucket", "FirebaseToken"
   * properties dynamically.
   */
  @Override
  public final void initComponent(Widget widget) {
    super.initComponent(widget);

    String devBucket = ODE.getUser().getUserEmail().replace(".", ":") + "";
    String projectName = ODE.getCurrentFileEditor().getFileId().split("/")[3];

    changeProperty(PROPERTY_NAME_DEVELOPER_BUCKET, devBucket + "/");
    changeProperty(PROPERTY_NAME_PROJECT_BUCKET, projectName);

    AsyncCallback<String> callback = new AsyncCallback<String>() {
      @Override
      public void onSuccess(String JWT) {
        changeProperty(PROPERTY_NAME_FIREBASE_TOKEN, JWT);
      }

      @Override
      public void onFailure(Throwable caught) {
        OdeLog.elog("Failed to create FirebaseDB JWT!");
      }
    };

    AUTH_SVC.getToken(devBucket, projectName, callback);
  }

  /**
   * Enforces the invisibility of the "DeveloperBucket" and "FirebaseToken"
   * properties.
   *
   * @param  propertyName the name of the property to check
   * @return true for a visible property, false for an invisible property
   */
  @Override
  protected boolean isPropertyVisible(String propertyName) {
    return !propertyName.equals(PROPERTY_NAME_DEVELOPER_BUCKET) &&
        !propertyName.equals(PROPERTY_NAME_FIREBASE_TOKEN) &&
        super.isPropertyVisible(propertyName);
  }
}
