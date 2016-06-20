package com.google.appinventor.components.runtime;

import android.content.Intent;
import android.util.Log;

import com.google.appinventor.components.annotations.DesignerProperty;
import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.PropertyCategory;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.annotations.UsesPermissions;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.common.YaVersion;
import com.google.appinventor.components.runtime.util.ErrorMessages;

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
 * @author thilanka
 */
@DesignerComponent(version = YaVersion.RASPBERRYPI_COMPONENT_VERSION,
        description = "<p>A non-visible component that models any device that can be attached to a pin of the Raspberry Pi.</p>",
        category = ComponentCategory.EXPERIMENTAL,
        nonVisible = true,
        iconName = "images/raspberryPi.png")
@SimpleObject
@UsesPermissions(permissionNames = "aadroid.permission.INTERNET")
public class RaspberryPiPinClient extends AndroidNonvisibleComponent
        implements Component {

    private final ComponentContainer container;
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

    /**
     * Creates a new AndroidNonvisibleComponent.
     *
     * @param container container, component will be placed in
     */
    protected RaspberryPiPinClient(ComponentContainer container) {
        super(container.$form());
        this.container = container;
    }
    
    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_NON_NEGATIVE_INTEGER, 
        defaultValue = Component.RASPBERRYPI_PINCLIENT_NUMBER_VALUE + "")
    @SimpleProperty(description = "The assigned number for the pin in the RaspberryPi GPIO Header.", 
        userVisible = true)
    public void PinNumber(int pPinNumber) {
      
      pinNumber = pPinNumber;
    }

    @SimpleProperty(description = "The assigned number for the pin in the RaspberryPi GPIO Header.", 
        category = PropertyCategory.BEHAVIOR, 
        userVisible = true)
    public int PinNumber() {
      return pinNumber;
    }
    
    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, 
        defaultValue = Component.RASPBERRYPI_PINCLIENT_STATE_VALUE + "")
    @SimpleProperty(description = "Designates whether the pin is on or off.", 
        userVisible = true)
    public void PinState(boolean pPinState) {
      pinState = pPinState;
    }

    @SimpleProperty(description = "Designates whether the pin is on or off.", 
        category = PropertyCategory.BEHAVIOR, 
        userVisible = true)
    public boolean PinState() {
      return pinState;
    }
    
    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_NON_NEGATIVE_INTEGER, 
        defaultValue = Component.RASPBERRYPI_PINCLIENT_DIRECTION_VALUE + "")
    @SimpleProperty(description = "Designates whether this is an input pin or an output pin.", 
        userVisible = true)
    public void Direction(int pDirection) {
      direction = pDirection;
    
    }
    
    @SimpleProperty(description = "Designates whether this is an input pin or an output pin.", 
        category = PropertyCategory.BEHAVIOR, 
        userVisible = true)
    public int Direction() {
      return direction;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_INTEGER, 
        defaultValue = Component.RASPBERRYPI_PINCLIENT_MODE_VALUE + "")
    @SimpleProperty(description = "Designates what mode this pin is in.", 
        userVisible = true)
    public void PinMode(int pPinMode) {
      pinMode = pPinMode;
    }

    @SimpleProperty(description = "Designates what mode this pin is in.", 
        userVisible = true)
    public int PinMode() {
      return pinMode;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_NON_NEGATIVE_INTEGER, 
        defaultValue = Component.RASPBERRYPI_PINCLIENT_PULLRESISTANCE_VALUE + "")
    @SimpleProperty(description = "Designates what type of a resistor is attached to this pin.", 
        userVisible = true)
    public void PullResistance(int pPullResistance) {
      pullResistance = pPullResistance;
    }

    @SimpleProperty(description = "Designates what type of a resistor is attached to this pin.", 
        category = PropertyCategory.BEHAVIOR, 
        userVisible = true)
    public int PullResistance() {
      return pullResistance;
    }
    
    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING)
    @SimpleProperty(description = "Designates the type of device connected to the pin. For e.g. LED, TemperatureSensor", 
        userVisible = true)
    public void DeviceName(String pDeviceName) {
      deviceName = pDeviceName;
    }

    @SimpleProperty(description = "Designates the type of device connected to the pin. For e.g. LED, TemperatureSensor", 
        category = PropertyCategory.BEHAVIOR, 
        userVisible = true)
    public String DeviceName() {
      return deviceName;
    }

    @SimpleProperty(description = "The topic of interest for this pin. For e.g. if the pin is attached to a temperature sensor, the topic can be 'temperature'.", 
        userVisible = true)
    public void MqttTopic(String pMqttTopic) {
      mqttTopic = pMqttTopic;
    }

    @SimpleProperty(description = "The topic of interest for this pin. For e.g. if the pin is attached to a temperature sensor, the topic can be 'temperature'.", 
        category = PropertyCategory.BEHAVIOR, 
        userVisible = true)
    public String MqttTopic() {
      return mqttTopic;
    }

    @SimpleProperty(description = "The message either sent to or received from the endpoint device attached to the pin. "
        + "For e.g. a TemperatureSensor can publish '80' as the payload.", 
        userVisible = true)
    public void MqttMessage(String pMqttMessage) {
      mqttMessage = pMqttMessage;
    }

    @SimpleProperty(description = "The message either sent to or received from the endpoint device attached to the pin. For e.g. a TemperatureSensor can publish '80' as the payload.", 
        category = PropertyCategory.BEHAVIOR, 
        userVisible = true)
    public String MqttMessage() {
      return mqttMessage;
    }
    
    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING)
    @SimpleProperty(description = "The topic to publish the lastWillMessage.", 
        userVisible = true)
    public void LastWillTopic(String pLastWillTopic) {
      lastWillTopic = pLastWillTopic;
    }

    @SimpleProperty(description = "The topic to publish the lastWillMessage.", 
        category = PropertyCategory.BEHAVIOR, 
        userVisible = true)
    public String LastWillTopic() {
      return lastWillTopic;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING)
    @SimpleProperty(description = "Message to be sent in the event the client disconnects.", 
        userVisible = true)
    public void LastWillMessage(String pLastWillMessage) {
      lastWillMessage = pLastWillMessage;
    }

    @SimpleProperty(description = "Message to be sent in the event the client disconnects.", 
        category = PropertyCategory.BEHAVIOR, 
        userVisible = true)
    public String LastWillMessage() {
      return lastWillMessage;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING)
    @SimpleProperty(description = "In addition to the RaspberryPi Server acting as the MQTTBroker, "
        + "a device attached a pin may optionally connect to an external MQTT broker identified by "
        + "the ip address and port of the external MQTT broker.", 
        userVisible = true)
    public void ExternalMQTTBroker(String pExternalMQTTBroker) {
      externalMQTTBroker = pExternalMQTTBroker;
    }

    @SimpleProperty(description = "In addition to the RaspberryPi Server acting as the MQTTBroker, "
        + "a device attached a pin may optionally connect to an external MQTT broker identified by "
        + "the ip address and port of the external MQTT broker.", 
        category = PropertyCategory.BEHAVIOR, 
        userVisible = true)
    public String ExternalMQTTBroker() {
      return externalMQTTBroker;
    }

    @SimpleFunction(description="Changes the state of the pin from HIGH to LOW or vice versa.")
    public void Toggle(){
      pinState = !pinState;
    }
    
    @SimpleFunction(description="Publish a message on the subject via the mqttBrokerEndpoint given.")
    public void Publish(String pMqttBrokerEndpoint, String pTopic, String pMessage) {
      //TODO
    }
    
    @SimpleFunction(description="Subscribes to a topic on the given subject at the given mqttBrokerEndpoint.")
    public void Subscribe(String pMqttBrokerEndpoint, String pTopic) {
      //TODO
    }
    
    @SimpleEvent(description="Event handler to return if the state of the pin changed from HIGH to LOW, or vice versa.")
    public boolean PinStateChanged(){
      //TODO
      return false;
    }

    @SimpleEvent(description="Event handler when the pin is connected to a device.")
    public boolean PinConnected(){
      //TODO
      return false;
    }
    
    @SimpleEvent(description="Event handler when a message is received through MQTT.")
    public boolean MqttMessageReceived(String pMessage) {
      //TODO
      return false;
    }
}