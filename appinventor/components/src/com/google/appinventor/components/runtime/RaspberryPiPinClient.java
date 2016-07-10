package com.google.appinventor.components.runtime;

import android.app.Activity;
import android.os.Handler;
import android.util.Log;
import edu.mit.mqtt.raspberrypi.HeaderPin;
import edu.mit.mqtt.raspberrypi.Messages;
import edu.mit.mqtt.raspberrypi.model.device.PinDirection;
import edu.mit.mqtt.raspberrypi.model.device.PinProperty;
import edu.mit.mqtt.raspberrypi.model.device.PinValue;
import edu.mit.mqtt.raspberrypi.model.device.RaspberrryPiModel;
import edu.mit.mqtt.raspberrypi.model.messaging.Topic;

import com.google.appinventor.components.annotations.DesignerProperty;

import java.util.List;

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
import com.google.appinventor.components.runtime.errors.ConnectionError;

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
@UsesLibraries(libraries = "org.eclipse.paho.android.service-1.0.2.jar, org.eclipse.paho.client.mqttv3-1.0.2.jar, gson-2.1.jar, raspberrypi-mqtt-messages-1.0-SNAPSHOT.jar")
public class RaspberryPiPinClient extends AndroidNonvisibleComponent implements Component, RaspberryPiMessageListener {

  private static final boolean DEBUG = true;
  private final static String LOG_TAG = "RaspberryPiPinClient";

  private int pinNumber = -1;
  private boolean pinState;
  private String pinDirection;
  private int pinMode;
  private int pullResistance;
  private String deviceName;
  private String mqttTopic;
  private String mqttMessage;
  private String lastWillTopic;
  private String lastWillMessage;
  private String externalMQTTBroker;

  private RaspberryPiServer raspberryPiServer;
  private RaspberryPiMessagingService mRaspberryPiMessagingService;

  /**
   * Creates a new AndroidNonvisibleComponent.
   *
   * @param pContainer
   *          container, component will be placed in
   */
  public RaspberryPiPinClient(ComponentContainer pContainer) {
    super(pContainer.$form());
    if (DEBUG) {
      Log.d(LOG_TAG, "Inside the RaspberryPiPinclient Constructor.");
    }

    Activity context = pContainer.$context();
    Handler handler = new Handler(context.getMainLooper());

    mRaspberryPiMessagingService = new RaspberryPiMessagingService(context, handler);
    mRaspberryPiMessagingService.addListener(this);

  }

  @SimpleProperty(description = "The RaspberryPiServer component this pin client is connected to", userVisible = true)
  public void RaspberryPiServer(RaspberryPiServer pRaspberryPiServer) {
    raspberryPiServer = pRaspberryPiServer;
    String ipv4Address = raspberryPiServer.Ipv4Address();
    int port = raspberryPiServer.Port();
    if (DEBUG) {
      Log.d(LOG_TAG, "Connecting to the RaspberryPiSever " + ipv4Address + ":" + port);
    }
    mRaspberryPiMessagingService.connect(ipv4Address, port);
    if (DEBUG) {
      Log.d(LOG_TAG, "Connected to the RaspberryPiSever " + ipv4Address + ":" + port);
    }

  }

  @SimpleProperty(description = "The RaspberryPiServer component this pin client is connected to", userVisible = true)
  public RaspberryPiServer RaspberryPiServer() {
    return raspberryPiServer;
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
    if (pinNumber == -1) {
      throw new ConnectionError("Pin number not set!");
    }

    HeaderPin myPin = new HeaderPin();
    myPin.number = pinNumber;
    myPin.property = PinProperty.PIN_STATE;
    myPin.value = pPinState ? PinValue.HIGH : PinValue.LOW;
    myPin.direction = PinDirection.fromString(pinDirection);
    myPin.raspberryPiModel = RaspberrryPiModel.fromString(raspberryPiServer.Model());
    myPin.label = deviceName;

    if (myPin.isInvalid()) {
      throw new ConnectionError(
          "All the required properties for the RaspberryPiPinClient not set. Please check pinNumber, pinDirection, and the raspberryPiServer. The raspberryPiServer should have a model set.");
    }

    String message = Messages.constructPinMessage(myPin);

    if (DEBUG) {
      Log.d(LOG_TAG, "Setting Pin " + pinNumber + " to " + myPin.value + " with this MQTT message: " + message);
    }
    mRaspberryPiMessagingService.publish(Topic.INTERNAL.toString(), message);
    if (DEBUG) {
      Log.d(LOG_TAG, "Set Pin " + pinNumber + " to " + myPin.value + " with this MQTT message: " + message);
    }
  }

  @SimpleProperty(description = "Designates whether the pin is on or off.", category = PropertyCategory.BEHAVIOR, userVisible = true)
  public boolean PinState() {
    return pinState;
  }

  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING)
  @SimpleProperty(description = "Designates whether this is an input pin or an output pin.", userVisible = true)
  public void Direction(String pDirection) {
    pinDirection = pDirection;
  }

  @SimpleProperty(description = "Designates whether this is an input pin or an output pin.", category = PropertyCategory.BEHAVIOR, userVisible = true)
  public String Direction() {
    return pinDirection;
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
  public void Publish(final String pTopic, final String pMessage) {
    if (DEBUG) {
      Log.d(LOG_TAG, "Sending message " + pMessage + " on topic " + pTopic);
    }
    if (raspberryPiServerConnected()) {
      mRaspberryPiMessagingService.publish(pTopic, pMessage);
    }
    if (DEBUG) {
      Log.d(LOG_TAG, "Sent message " + pMessage + " on topic " + pTopic);
    }
  }

  @SimpleFunction(description = "Subscribes to a topic on the given subject at the given mqttBrokerEndpoint. The Subsribe, "
      + " method has two parameters such as  String pMqttBrokerEndpoint, String pTopic . ")
  public synchronized void Subscribe(final String pTopic) {
    if (DEBUG) {
      Log.d(LOG_TAG, "Subscribing to messages on topic " + pTopic);
    }
    if (raspberryPiServerConnected()) {
      mRaspberryPiMessagingService.subscribe(pTopic);
    }
    if (DEBUG) {
      Log.d(LOG_TAG, "Subscribed to messages on topic " + pTopic);
    }
  }

  @SimpleFunction(description = "Unsubscribes to a topic on the given subject. The UnSubsribe, "
      + " method takes the parameter String pTopic . ")
  public void Unsubscribe(String pTopic) {
    if (DEBUG) {
      Log.d(LOG_TAG, "Unsubscribing to messages on topic " + pTopic);
    }
    if (raspberryPiServerConnected()) {
      mRaspberryPiMessagingService.unsubscribe(pTopic);
    }
    if (DEBUG) {
      Log.d(LOG_TAG, "Unsubscribed to messages on topic " + pTopic);
    }
  }

  @SimpleEvent(description = "Event handler to return if the state of the pin changed from HIGH to LOW, or vice versa.")
  public void PinStateChanged() {
    if (DEBUG) {
      Log.d(LOG_TAG, "RaspberryPi pin " + pinNumber + " state changed to " + pinState + ".");
    }
    // TODO: what triggers a pin state change?
    EventDispatcher.dispatchEvent(this, "PinStateChanged");
  }

  @SimpleEvent(description = "Event handler when the pin is connected to a device.")
  public void PinConnected() {
    if (DEBUG) {
      Log.d(LOG_TAG, "RaspberryPi pin " + pinNumber + " connected.");
    }
    // TODO what is the condition here?
    EventDispatcher.dispatchEvent(this, "PinConnected");
  }

  @Override
  @SimpleEvent(description = "Event handler when a message is received through MQTT.")
  public void MqttMessageReceived(String pTopic, String pMessage) {
    if (DEBUG) {
      Log.d(LOG_TAG, "Mqtt Message " + pMessage + " received on subject " + pTopic + ".");
    }
    if (pTopic != null && pMessage != null && pMessage.length() > 0) {
      EventDispatcher.dispatchEvent(this, "MqttMessageReceived", pTopic, pMessage);
    }
  }

  @Override
  @SimpleEvent(description = "Event handler when a message is sent through MQTT.")
  public void MqttMessageSent(List<String> pTopics, String pMessage) {
    if (DEBUG) {
      StringBuilder topicBuilder = new StringBuilder();
      for (String topic : pTopics) {
	topicBuilder.append(topic);
      }
      String topics = topicBuilder.toString();
      Log.d(LOG_TAG, "Mqtt Message " + pMessage + " sent on subjects " + topics + ".");
    }
    if (pTopics != null && pTopics.size() > 0 && pMessage != null && pMessage.length() > 0) {
      EventDispatcher.dispatchEvent(this, "MqttMessageSent", pTopics, pMessage);
    }
  }

  @Override
  @SimpleEvent(description = "Event handler when a message the MQTT connection is lost.")
  public void MqttConnectionLost(String pError) {
    if (DEBUG) {
      Log.d(LOG_TAG, "Connection via MQTT lost due to this error: " + pError);
    }
    if (pError != null) {
      EventDispatcher.dispatchEvent(this, "MqttConnectionLost", pError);
    }
  }

  private boolean raspberryPiServerConnected() {
    if (DEBUG) {
      Log.d(LOG_TAG, "RasberryPiServer = " + raspberryPiServer);
    }
    if (raspberryPiServer == null) {
      throw new ConnectionError(
          "The RaspberryPiServer must be connected to the RaspberryPiPinClient to perform the action.");
    }
    return true;
  }

}