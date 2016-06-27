package com.google.appinventor.components.runtime;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.Settings.Secure;
import android.util.Log;

import com.google.appinventor.components.annotations.DesignerProperty;

import java.util.Locale;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import com.google.appinventor.components.annotations.DesignerComponent;
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

/**
 * RaspberryPiPinClient models any device attached to a GPIO pin of the
 * RaspberryPi. This also acts as an MQTT Client, and will either publish or
 * subscribe to certain topic(s). For example, a temperature sensor attached to
 * the Raspberry Pi device can publish to the topic “temperature”, whereas an
 * LED light can subscribe to the topic “temperature”, and when a certain
 * temperature has exceeded it may be programmed to flash. The same LED light
 * can subscribe to the topic “message”, and can be programmed to flash in a
 * different sequence when a message is received.
 * 
 * @author Thilanka Munasinghe (thilankawillbe@gmail.com)
 */
@DesignerComponent(version = YaVersion.RASPBERRYPI_COMPONENT_VERSION, description = "<p>A non-visible component that models any device that can be attached to a pin of the Raspberry Pi.</p>", category = ComponentCategory.EXPERIMENTAL, nonVisible = true, iconName = "images/raspberryPi.png")
@SimpleObject
@UsesPermissions(permissionNames = "android.permission.INTERNET, " + "android.permission.WAKE_LOCK, "
    + "android.permission.ACCESS_NETWORK_STATE, " + "android.permission.WRITE_EXTERNAL_STORAGE")
@UsesLibraries(libraries = "org.eclipse.paho.android.service-1.0.2.jar, org.eclipse.paho.client.mqttv3-1.0.2.jar")
public class RaspberryPiPinClient extends AndroidNonvisibleComponent implements Component {

  private static final boolean DEBUG = true;
  private final static String LOG_TAG = "RaspberryPiPinClient";

  private int pinNumber;
  private boolean pinState;
  private int direction;
  private int pinMode;
  private int pullResistance;
  private String deviceName;
  private String mqttTopic;
  private String mqttMessage;
  private String lastWillTopic;
  private String lastWillMessage;
  private String externalMQTTBroker;

  private static final String THREAD_NAME = "Thread[" + LOG_TAG + "]"; 
  
  public static final int QOS_0 = 0; // Delivery Once no confirmation
  public static final int QOS_1 = 1; // Delivery at least once with confirmation
  public static final int QOS_2 = 2; // Delivery only once with confirmation with handshake
  private static final boolean CLEAN_SESSION = true; // Start a clean session?
  private static final String MQTT_URL_FORMAT = "tcp://%s:%d"; // URL Format
  private static final String DEVICE_ID_FORMAT = "andr_%s"; // Device ID Format

  private String mDeviceId; // Device ID, Secure.ANDROID_ID
  private MqttConnectOptions mOpts; // Connection Options
  private MqttClient mClient; // MQTT Client
  private boolean mStarted = false; // Is the Client started?
  private Handler mConnHandler; // Seperate Handler thread for networking
  private MemoryPersistence mMemStore; // MemoryStore

  private Handler parentHandler; // Handler from main thread
  private ConnectivityManager mConnectivityManager; // To check for connectivity changes

  /**
   * Creates a new AndroidNonvisibleComponent.
   *
   * @param container
   *          container, component will be placed in
   */
  public RaspberryPiPinClient(ComponentContainer container) {
    super(container.$form());

    if (DEBUG) {
      Log.d(LOG_TAG, "Inside the RaspberryPiPinclient Constructor.");
    }

    Handler handler = new Handler(container.$context().getMainLooper());
    Activity context = container.$context();

    mDeviceId = String.format(DEVICE_ID_FORMAT, Secure.ANDROID_ID);
    mMemStore = new MemoryPersistence();
    mOpts = new MqttConnectOptions();
    mOpts.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);
    mOpts.setCleanSession(CLEAN_SESSION);

    HandlerThread thread = new HandlerThread(THREAD_NAME);
    thread.start();

    mConnHandler = new Handler(thread.getLooper());

    mConnectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    parentHandler = handler;

  }

  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_NON_NEGATIVE_INTEGER, defaultValue = Component.RASPBERRYPI_PINCLIENT_NUMBER_VALUE
      + "")
  @SimpleProperty(description = "The assigned number for the pin in the RaspberryPi GPIO Header.", userVisible = true)
  public void PinNumber(int pPinNumber) {
    pinNumber = pPinNumber;
  }

  @SimpleProperty(description = "The assigned number for the pin in the RaspberryPi GPIO Header.", category = PropertyCategory.BEHAVIOR, userVisible = true)
  public int PinNumber() {
    return pinNumber;
  }

  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = Component.RASPBERRYPI_PINCLIENT_STATE_VALUE
      + "")
  @SimpleProperty(description = "Designates whether the pin is on or off.", userVisible = true)
  public void PinState(boolean pPinState) {
    pinState = pPinState;
  }

  @SimpleProperty(description = "Designates whether the pin is on or off.", category = PropertyCategory.BEHAVIOR, userVisible = true)
  public boolean PinState() {
    return pinState;
  }

  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_NON_NEGATIVE_INTEGER, defaultValue = Component.RASPBERRYPI_PINCLIENT_DIRECTION_VALUE
      + "")
  @SimpleProperty(description = "Designates whether this is an input pin or an output pin.", userVisible = true)
  public void Direction(int pDirection) {
    direction = pDirection;
  }

  @SimpleProperty(description = "Designates whether this is an input pin or an output pin.", category = PropertyCategory.BEHAVIOR, userVisible = true)
  public int Direction() {
    return direction;
  }

  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_INTEGER, defaultValue = Component.RASPBERRYPI_PINCLIENT_MODE_VALUE
      + "")
  @SimpleProperty(description = "Designates what mode this pin is in.", userVisible = true)
  public void PinMode(int pPinMode) {
    pinMode = pPinMode;
  }

  @SimpleProperty(description = "Designates what mode this pin is in.", userVisible = true)
  public int PinMode() {
    return pinMode;
  }

  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_NON_NEGATIVE_INTEGER, defaultValue = Component.RASPBERRYPI_PINCLIENT_PULLRESISTANCE_VALUE
      + "")
  @SimpleProperty(description = "Designates what type of a resistor is attached to this pin.", userVisible = true)
  public void PullResistance(int pPullResistance) {
    pullResistance = pPullResistance;
  }

  @SimpleProperty(description = "Designates what type of a resistor is attached to this pin.", category = PropertyCategory.BEHAVIOR, userVisible = true)
  public int PullResistance() {
    return pullResistance;
  }

  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING)
  @SimpleProperty(description = "Designates the type of device connected to the pin. For e.g. LED, TemperatureSensor", userVisible = true)
  public void DeviceName(String pDeviceName) {
    deviceName = pDeviceName;
  }

  @SimpleProperty(description = "Designates the type of device connected to the pin. For e.g. LED, TemperatureSensor", category = PropertyCategory.BEHAVIOR, userVisible = true)
  public String DeviceName() {
    return deviceName;
  }

  @SimpleProperty(description = "The topic of interest for this pin. For e.g. if the pin is attached to a temperature sensor, the topic can be 'temperature'.", userVisible = true)
  public void MqttTopic(String pMqttTopic) {
    mqttTopic = pMqttTopic;
  }

  @SimpleProperty(description = "The topic of interest for this pin. For e.g. if the pin is attached to a temperature sensor, the topic can be 'temperature'.", category = PropertyCategory.BEHAVIOR, userVisible = true)
  public String MqttTopic() {
    return mqttTopic;
  }

  @SimpleProperty(description = "The message either sent to or received from the endpoint device attached to the pin. "
      + "For e.g. a TemperatureSensor can publish '80' as the payload.", userVisible = true)
  public void MqttMessage(String pMqttMessage) {
    mqttMessage = pMqttMessage;
  }

  @SimpleProperty(description = "The message either sent to or received from the endpoint device attached to the pin. For e.g. a TemperatureSensor can publish '80' as the payload.", category = PropertyCategory.BEHAVIOR, userVisible = true)
  public String MqttMessage() {
    return mqttMessage;
  }

  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING)
  @SimpleProperty(description = "The topic to publish the lastWillMessage.", userVisible = true)
  public void LastWillTopic(String pLastWillTopic) {
    lastWillTopic = pLastWillTopic;
  }

  @SimpleProperty(description = "The topic to publish the lastWillMessage.", category = PropertyCategory.BEHAVIOR, userVisible = true)
  public String LastWillTopic() {
    return lastWillTopic;
  }

  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING)
  @SimpleProperty(description = "Message to be sent in the event the client disconnects.", userVisible = true)
  public void LastWillMessage(String pLastWillMessage) {
    lastWillMessage = pLastWillMessage;
  }

  @SimpleProperty(description = "Message to be sent in the event the client disconnects.", category = PropertyCategory.BEHAVIOR, userVisible = true)
  public String LastWillMessage() {
    return lastWillMessage;
  }

  // TODO: Refactor this: Have a top level MQTTBroker and have the
  // RaspberryPiServer subclass from that?

  // @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING)
  // @SimpleProperty(description = "In addition to the RaspberryPi Server acting
  // as the MQTTBroker, "
  // + "a device attached a pin may optionally connect to an external MQTT
  // broker identified by "
  // + "the ip address and port of the external MQTT broker.", userVisible =
  // true)
  // public void ExternalMQTTBroker(String pExternalMQTTBroker) {
  // externalMQTTBroker = pExternalMQTTBroker;
  // }
  //
  // @SimpleProperty(description = "In addition to the RaspberryPi Server acting
  // as the MQTTBroker, "
  // + "a device attached a pin may optionally connect to an external MQTT
  // broker identified by "
  // + "the ip address and port of the external MQTT broker.", category =
  // PropertyCategory.BEHAVIOR, userVisible = true)
  // public String ExternalMQTTBroker() {
  // return externalMQTTBroker;
  // }
  //

  @SimpleFunction(description = "Changes the state of the pin from HIGH to LOW or vice versa.")
  public void Toggle() {
    pinState = !pinState;
  }

  @SimpleFunction(description = "Publish a message on the subject via the mqttBrokerEndpoint given. The Publish method, "
      + " takes three Parameters such as RaspberryPiServer, Topic and Message . ")
  public void Publish(final RaspberryPiServer pRaspberryPiServer, final String pTopic, final String pMessage) {

    if (DEBUG) {
      Log.d(LOG_TAG, "Calling the Publish method in RaspPinClient.");
    }

    if (!isConnected()) {
      if (DEBUG) {
	Log.d(LOG_TAG, "pRaspberryPiServer.Ipv4Address() =" + pRaspberryPiServer.Ipv4Address());
	Log.d(LOG_TAG, "Before pRaspberryPiServer.Port() =" + pRaspberryPiServer.Port());
      }
      connect(pRaspberryPiServer.Ipv4Address(), pRaspberryPiServer.Port());
    }

    mConnHandler.post(new Runnable() {
      @Override
      public void run() {

	int qos = 2;

	try {
	  if (DEBUG) {
	    Log.d(LOG_TAG, "Attempting to send a message:  Topic:\t" + pTopic + "  Message:\t" + pMessage);
	  }

	  MqttMessage message = new MqttMessage(pMessage.getBytes());
	  message.setQos(qos);
	  mClient.publish(pTopic, message);
	  if (DEBUG) {
	    Log.d(LOG_TAG, "Sent a message:  Topic:\t" + pTopic + "  Message:\t" + pMessage);
	  }
	  mClient.disconnect();
	} catch (MqttException e) {
	  Log.e(LOG_TAG, "Failed to send a message:  Topic:\t" + pTopic + "  Message:\t" + pMessage + "\tError: "
              + e.getMessage());
	  e.printStackTrace();
	}
      }
    });
  }

  @SimpleFunction(description = "Subscribes to a topic on the given subject at the given mqttBrokerEndpoint. The Subsribe, "
      + " method has two parameters such as  String pMqttBrokerEndpoint, String pTopic . ")
  public void Subscribe(RaspberryPiServer pRaspberryPiServer, String pTopic) {
  }

  @SimpleFunction(description = "UnSubscribes to a topic on the given subject. The UnSubsribe, "
      + " method takes the parameter String pTopic . ")
  public void Unsubscribe(RaspberryPiServer pRaspberryPiServer, String pTopic) {
    // TODO
    // We have to implement an UnSubscribe method for clients to unsubscribe if
    // needed. (stop listening to the messages)
  }

  /**
   * Connects to the RaspberryPi Server which acts as the MQTT broker
   *
   * @param pBrokerIPAddress
   *          The address of the broker (such as an ip address)
   * @param pBrokerPort
   *          The port to connect on
   */
  private synchronized void connect(final String pBrokerIPAddress, final int pBrokerPort) {
    if (isConnected()) {
      return;
    }
    String url = String.format(Locale.US, MQTT_URL_FORMAT, pBrokerIPAddress, pBrokerPort);
    if (DEBUG) {
      Log.d(LOG_TAG, "Connecting with URL: " + url);
    }
    try {
      if (DEBUG) {
	Log.d(LOG_TAG, "Connecting...");
      }
      mClient = new MqttClient(url, mDeviceId, mMemStore);
    } catch (MqttException e) {
      e.printStackTrace();
    }

    mConnHandler.post(new Runnable() {
      @Override
      public void run() {
	try {
	  mClient.connect(mOpts);

	  mStarted = true; // Service is now connected

	  if (DEBUG) {
	    Log.d(LOG_TAG, "Successfully connected");
	  }

	} catch (MqttException e) {
	  Log.e(LOG_TAG,
              "Unable to connect to the RaspberryPiSever with address " + pBrokerIPAddress + ":" + pBrokerPort);
	  e.printStackTrace();
	}
      }
    });
  }

  /**
   * Verifies the client state
   *
   * @return true if connected, false if we aren't connected
   */
  private boolean isConnected() {
    if (mStarted && mClient != null && !mClient.isConnected()) {
      Log.e(LOG_TAG, "Mismatch between what we think is connected and what is connected");
    }

    return mClient != null && (mStarted && mClient.isConnected());
  }

  @SimpleEvent(description = "Event handler to return if the state of the pin changed from HIGH to LOW, or vice versa.")
  public boolean PinStateChanged(RaspberryPiServer pRaspberryPiServer) {
    // TODO
    return false;
  }

  @SimpleEvent(description = "Event handler when the pin is connected to a device.")
  public boolean PinConnected(RaspberryPiServer pRaspberryPiServer) {
    // TODO
    return false;
  }

  @SimpleEvent(description = "Event handler when a message is received through MQTT.")
  public boolean MqttMessageReceived(String pMessage) {
    // TODO
    return false;
  }

}