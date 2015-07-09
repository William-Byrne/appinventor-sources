package com.google.appinventor.client.editor.simple.components;

import com.google.appinventor.client.Ode;
import com.google.appinventor.client.editor.simple.SimpleComponentDatabase;
import com.google.appinventor.client.editor.simple.SimpleEditor;
import com.google.appinventor.client.widgets.properties.EditableProperty;
import com.google.appinventor.client.widgets.properties.TextPropertyEditor;
import com.google.appinventor.shared.simple.ComponentDatabaseInterface;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;

import java.util.List;

/**
 * Mock for the non-visible FirebaseDB component. This needs a separate mock
 * from other non-visible components so that some of its properties can be
 * given dynamic default values.
 *
 * @author will2596@gmail.com (William Byrne)
 */
public class MockFirebaseDB extends MockNonVisibleComponent {

  public static final String TYPE = "FirebaseDB";
  private static final String PROPERTY_NAME_UUPATH = "UniqueUserPath";
  private static final String PROPERTY_NAME_PPATH = "ProjectPath";
  private static final Ode ODE = Ode.getInstance();

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
   * Initializes the invisible "UniqueUserPath" property for use in partitioning
   * the default App Inventor Firebase.
   */
  @Override
  public final void initComponent(Widget widget) {
    super.initComponent(widget);

    String userPath = ODE.getUser().getUserEmail().hashCode() + "/";
    String projectName = ODE.getCurrentFileEditor().getFileId().split("/")[3] + "/";


    changeProperty(PROPERTY_NAME_UUPATH, userPath);
    changeProperty(PROPERTY_NAME_PPATH, projectName);
  }

  /**
   * Enforces the invisibility of the "UniqueUserPath" property.
   *
   * @param  propertyName the name of the property to check
   * @return true for a visible property, false for an invisible property
   */
  @Override
  protected boolean isPropertyVisible(String propertyName) {
    return !propertyName.equals(PROPERTY_NAME_UUPATH) &&
        super.isPropertyVisible(propertyName);
  }
}
