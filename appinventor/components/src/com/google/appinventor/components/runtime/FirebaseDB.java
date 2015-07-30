// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2011-2012 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package com.google.appinventor.components.runtime;


import com.firebase.client.AuthData;
import com.firebase.client.ChildEventListener;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.DesignerProperty;
import com.google.appinventor.components.annotations.PropertyCategory;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.annotations.UsesLibraries;
import com.google.appinventor.components.annotations.UsesPermissions;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.common.YaVersion;
import com.google.appinventor.components.runtime.errors.YailRuntimeError;
import com.google.appinventor.components.runtime.util.JsonUtil;
import org.json.JSONException;

import android.util.Log;
import android.app.Activity;
import android.os.Handler;

// When the component is installed in App Inventor, the Javadoc
// comments will become included in the automatically-generated system
// documentation, except for lines starting with tags (such as @author).
/**
 * The Firebase component communicates with a Web service to store
 * and retrieve information.  The component has methods to
 * store a value under a tag and to retrieve the value associated with
 * the tag. It also possesses a listener to fire events when stored
 * values are changed.
 *
 * @author kasmus@mit.edu (Kristin Asmus)
 * @author will2596@gmail.com (William Byrne) (default Firebase partitioning and user authentication)
 */


// The annotations here provide information to the compiler about
// integrating the component into App Inventor system.  The following
// three annotations stipulate that Firebase Component will appear in the
// designer, that it will be an object in the App Inventor language,
// and say what Android system permissions it requires.

@DesignerComponent(version = YaVersion.FIREBASE_COMPONENT_VERSION,
    description = "Non-visible component that communicates with Firebase to store and " +
    "retrieve information.",
    designerHelpDescription = "Non-visible component that communicates with a Firebase" +
        " to store and retrieve information.",
    category = ComponentCategory.STORAGE,
    nonVisible = true,
    iconName = "images/firebaseDB.png")
@SimpleObject
@UsesPermissions(permissionNames = "android.permission.INTERNET")
@UsesLibraries(libraries = "firebase.jar")
public class FirebaseDB extends AndroidNonvisibleComponent implements Component {

  private static final String LOG_TAG = "Firebase";
  private static final String DEFAULT_URL =
      "https://resplendent-inferno-8682.firebaseio.com/";

  private String firebaseURL;
  private String developerBucket;
  private String projectBucket;
  private String firebaseToken;
  private Handler androidUIHandler;
  private final Activity activity;
  private Firebase myFirebase;
  private ChildEventListener childListener;
  private Firebase.AuthStateListener authListener;

  /**
   * Creates a new Firebase component.
   *
   * @param container the Form that this component is contained in.
   */
  public FirebaseDB(ComponentContainer container) {
    super(container.$form());
    // We use androidUIHandler when we set up operations that run asynchronously 
    // in a separate thread, but which themselves want to cause actions
    // back in the UI thread.  They do this by posting those actions
    // to androidUIHandler.
    androidUIHandler = new Handler();
    this.activity = container.$context();
    Firebase.setAndroidContext(activity);

    firebaseURL = DEFAULT_URL;
    developerBucket = ""; // set dynamically in the Designer
    projectBucket = ""; // given a dynamic default value in the Designer
    firebaseToken = ""; // set dynamically in the Designer
    myFirebase = new Firebase(firebaseURL + "developers/" + developerBucket + projectBucket);

    childListener = new ChildEventListener() {
      // Retrieve new posts as they are added to Firebase
      @Override
      public void onChildAdded(final DataSnapshot snapshot, String previousChildKey) {
        androidUIHandler.post(new Runnable() {
          public void run() {
            // Signal an event to indicate that the value was
            // stored.  We post this to run in the Application's main
            // UI thread.
            DataChanged(snapshot.getKey(), snapshot.getValue());
          }
        });
      }

      @Override
      public void onCancelled(final FirebaseError error) {
        androidUIHandler.post(new Runnable() {
          public void run() {
            // Signal an event to indicate that the value was
            // stored.  We post this to run in the Application's main
            // UI thread.
            FirebaseError(error.getMessage());
          }
        });
      }

      @Override
      public void onChildChanged(final DataSnapshot snapshot, String previousChildKey) {
        androidUIHandler.post(new Runnable() {
          public void run() {
            // Signal an event to indicate that the value was
            // stored.  We post this to run in the Application's main
            // UI thread.
            DataChanged(snapshot.getKey(), snapshot.getValue());
          }
        });
      }

      @Override
      public void onChildMoved(DataSnapshot snapshot, String previousChildKey) {
      }

      @Override
      public void onChildRemoved(final DataSnapshot snapshot) {
        androidUIHandler.post(new Runnable() {
          public void run() {
            // Signal an event to indicate that the value was
            // stored.  We post this to run in the Application's main
            // UI thread.
            DataChanged(snapshot.getKey(), null);
          }
        });
      }
    };

    authListener = new Firebase.AuthStateListener() {
      @Override
      public void onAuthStateChanged(AuthData data) {
        if (data == null) {
          myFirebase.authWithCustomToken(firebaseToken, new Firebase.AuthResultHandler() {
            @Override
            public void onAuthenticated(AuthData authData) {
              Log.e(LOG_TAG, "Auth Successful.");
            }

            @Override
            public void onAuthenticationError(FirebaseError error) {
              Log.e(LOG_TAG, "Auth Failed with Message: " + error.getMessage());
            }
          });
        }
      }
    };

    myFirebase.addAuthStateListener(authListener);
    myFirebase.addChildEventListener(childListener);
  }

  // The two procedures below give the getter and setter for the
  // Firebase component's FirebaseURL property.  Each one has
  // a @SimpleProperty annotation to indicate that it's a property in
  // the language (and blocks will be generated for it).  The setter
  // also has a @DesignerProperty that makes this property appear in the
  // Properties listed with the component in the designer.  Here we've
  // stipulated that the property should appear with a default value.  Note
  // that this default specifies what should be shown in the designer:
  // it does not automatically set the value of FirebaseURL by itself,
  // which is why we explicitly set the variable FirebaseURL above
  // where the component is created.

  /**
   * Returns the URL of the Firebase.
   */
  @SimpleProperty(
      category = PropertyCategory.BEHAVIOR)
  public String FirebaseURL() {
    return firebaseURL;
  }

  /**
   * Specifies the URL of the Firebase.
   * The default value is currently my private Firebase url //TODO: this should be changed
   */
  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING,
      defaultValue = DEFAULT_URL)
  @SimpleProperty
  public void FirebaseURL(String url) {
    firebaseURL = url;
    resetListener();
  }
  
  
  // The two procedures below give the getter and setter for the
  // Firebase component's ProjectPath property.  Each one has
  // a @SimpleProperty annotation to indicate that it's a property in
  // the language (and blocks will be generated for it).  The setter
  // also has a @DesignerProperty that makes this property appear in the
  // Properties listed with the component in the designer.  Here we've
  // stipulated that the property should appear with a default value.  Note
  // that this default specifies what should be shown in the designer:
  // it does not automatically set the value of ProjectPath by itself,
  // which is why we explicitly set the variable ProjectPath above
  // where the component is created.

  /**
   * Returns the path for the developer bucket of the Firebase.
   */
  @SimpleProperty(category = PropertyCategory.BEHAVIOR, userVisible = false)
  public String DeveloperBucket() {
    return developerBucket;
  }

  /**
   * Specifies the unique developer path of the Firebase. This is set programmatically
   * in {@link com.google.appinventor.client.editor.simple.components.MockFirebaseDB}
   * and consists of the current App Inventor user's email.
   *
   * @param bucket the name of the developer's bucket
   */
  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING)
  @SimpleProperty
  public void DeveloperBucket(String bucket) {
    developerBucket = bucket;
    resetListener();
  }

  /**
   * Returns the path for the project bucket of the Firebase.
   */
  @SimpleProperty(category = PropertyCategory.BEHAVIOR)
  public String ProjectBucket() {
    return projectBucket;
  }

  /**
   * Specifies the path for the project bucket of the Firebase.
   */
  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING,
      defaultValue = "")
  @SimpleProperty
  public void ProjectBucket(String path) {
    projectBucket = path;
    resetListener();
  }

  @SimpleProperty(category = PropertyCategory.BEHAVIOR, userVisible = false)
  public String FirebaseToken() {
    return firebaseToken;
  }

  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING)
  @SimpleProperty
  public void FirebaseToken(String JWT) {
    firebaseToken = JWT;
    resetListener();
  }
  
  private void resetListener() {
    // remove listeners from the old Firebase path
    myFirebase.removeEventListener(childListener);
    myFirebase.removeAuthStateListener(authListener);

    if(firebaseURL.equals(DEFAULT_URL)) {
      myFirebase = new Firebase(firebaseURL + "developers/" + developerBucket + projectBucket);

      // add the authListener to the new Firebase path only when the
      // default Firebase is being used
      myFirebase.addAuthStateListener(authListener);
    } else {
      myFirebase = new Firebase(firebaseURL + projectBucket);
    }

    // add the childListener to the new Firebase path
    myFirebase.addChildEventListener(childListener);
  }



  /**
   * Asks Firebase to store the given value under the given tag.
   *
   * @param tag The tag to use
   * @param valueToStore The value to store. Can be any type of value (e.g.
   * number, text, boolean or list).
   */
  @SimpleFunction
  public void StoreValue(final String tag, final Object valueToStore) {
    try {
      this.myFirebase.child(tag).setValue(JsonUtil.getJsonRepresentation(valueToStore));
    } catch(JSONException e) {
      throw new YailRuntimeError("Value failed to convert to JSON.", "JSON Creation Error.");
    }
  }
  
//  /**
//   * Asks Firebase to store the given value under the given tag
//   * if no value exists at that location yet.
//   *
//   * @param tag The tag to use
//   * @param valueToStore The value to store. Can be any type of value (e.g.
//   * number, text, boolean or list).
//   */
//  @SimpleFunction
//  public void InitializeValue(final String tag, final Object valueToStore) {
//    this.myFirebase.child(tag).addListenerForSingleValueEvent(new ValueEventListener() {
//      @Override
//      public void onDataChange(final DataSnapshot snapshot) {
//        if (snapshot.getValue() == null) {
//          StoreValue(tag, valueToStore);
//        } else {
//          androidUIHandler.post(new Runnable() {
//            public void run() {
//              // Signal an event to indicate that the value was
//              // stored.  We post this to run in the Applcation's main
//              // UI thread.
//              GotValue(tag, snapshot.getValue());
//            }
//          });
//        }
//      }
//      @Override
//      public void onCancelled(final FirebaseError error) {
//        androidUIHandler.post(new Runnable() {
//          public void run() {
//            // Signal an event to indicate that the value was
//            // stored.  We post this to run in the Applcation's main
//            // UI thread.
//            FirebaseError(error.getMessage());
//          }
//        });
//      }
//    });
//  }

  // The implementation of GetValue uses an event-driven strategy.  
  // The onSuccess callback returns the response.
  // The onFailure callback signals a FirebaseError.
  /**
   * GetValue asks Firebase to get the value stored under the given tag.
   * It will return valueIfTagNotThere if there is no value stored
   * under the tag.
   *
   * @param tag The tag whose value is to be retrieved.
   * @param valueIfTagNotThere The value to pass to the event if the tag does
   *                           not exist.
   */
  @SimpleFunction
  public void GetValue(final String tag/*, final String valueIfTagNotThere*/) {
    this.myFirebase.child(tag).addListenerForSingleValueEvent(new ValueEventListener() {
      @Override
      public void onDataChange(final DataSnapshot currentData) {
        // TODO: Wouldn't it be better to just not fire the event if the value doesn't exist?
        // This seems like a good solution in a synchronous model, but not so much in an asynchronous
        // one.

//        final String value = (snapshot != null)
//            ? (String) snapshot.getValue() : valueIfTagNotThere;

        final Object value = currentData.getValue();

        androidUIHandler.post(new Runnable() {
          public void run() {
            // Signal an event to indicate that the value was
            // stored.  We post this to run in the Application's main
            // UI thread.
            GotValue(tag, value);
          }
        });
      }

      @Override
      public void onCancelled(final FirebaseError error) {
        androidUIHandler.post(new Runnable() {
          public void run() {
            // Signal an event to indicate that the value was
            // stored.  We post this to run in the Applcation's main
            // UI thread.
            FirebaseError(error.getMessage());
          }
        });
      }
    });
  }

  /**
   * Indicates that a GetValue request has succeeded.
   *
   * @param value the value that was returned. Can be any type of value
   * (e.g. number, text, boolean or list).
   */
  @SimpleEvent
  public void GotValue(String tag, Object value) {
    try {
      if(value != null) {
        value = JsonUtil.getObjectFromJson((String) value);
      }
    } catch(JSONException e) {
      throw new YailRuntimeError("Value failed to convert from JSON.", "JSON Creation Error.");
    }

    // Invoke the application's "GotValue" event handler
    EventDispatcher.dispatchEvent(this, "GotValue", tag, value);
  }
  
  /**
   * Indicates that the data in the Firebase has changed.
   * Launches an event with the tag and value that have been updated.
   *
   * @param tag the tag that has changed.
   * @param value the value that has changed.
   */
  @SimpleEvent
  public void DataChanged(String tag, Object value) {
    try {
      if(value != null) {
        value = JsonUtil.getObjectFromJson((String) value);
      }
    } catch(JSONException e) {
      throw new YailRuntimeError("Value failed to convert from JSON.", "JSON Creation Error.");
    }

    // Invoke the application's "DataChanged" event handler
    EventDispatcher.dispatchEvent(this, "DataChanged", tag, value);
  }

  /**
   * Indicates that the communication with the Firebase signaled an error.
   *
   * @param message the error message
   */
  @SimpleEvent
  public void FirebaseError(String message) {
    // Invoke the application's "FirebaseError" event handler
    // Log.w(LOG_TAG, "calling error event handler: " + message);
    EventDispatcher.dispatchEvent(this, "FirebaseError", message);
  }
}
