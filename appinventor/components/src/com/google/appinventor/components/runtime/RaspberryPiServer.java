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
 * RaspberryPiServer models the Raspberry Pi and acts as an MQTT broker that
 * relays messages between the RaspberryPiPinClients and other external sources.
 * The broker, and thus the RaspberryPiServer has an IP address that clients can
 * connect via TCP/IP. For the RaspberryPi component, it is necessary to have
 * the MQTT clients such as sensors and LED outputs to be subscribed to certain
 * topics via the broker. The App Inventor developer can opt to connect to external MQTT
 * brokers or run an MQTT broker on the Raspberry PI device.
 * 
 * @author Thilanka Munasinghe (thilankawillbe@gmail.com)
 */
@DesignerComponent(description = "<p>A non-visible component that models the Raspberry Pi and acts as "
    + "an MQTT broker that relays messages between the RaspberryPiPinClients and other "
    + "external MQTT based sources.</p>", 
    version = YaVersion.RASPBERRYPI_COMPONENT_VERSION, 
    category = ComponentCategory.EXPERIMENTAL, 
    nonVisible = true, 
    iconName = "images/raspberryPi.png")
@SimpleObject
@UsesPermissions(permissionNames = "aadroid.permission.INTERNET")
public class RaspberryPiServer extends AndroidNonvisibleComponent implements Component {

  private static final boolean DEBUG = true;
  private final static String LOG_TAG = "RaspberryPiServer";

  private String model;
  private String ipv4Address;
  private int port;
  private int qos;
  private int pins;
  private boolean shutdown = false;
  
  /**
   * Creates a new AndroidNonvisibleComponent.
   *
   * @param container
   *          the component will be placed in
   */
  public RaspberryPiServer(ComponentContainer pContainer) {
    super(pContainer.$form());
  }

  /**
   * RaspberryPi Model and version. (e.g. A, B, B+, Compute, etc). Depending on
   * the model, there will be constraints on the number of pins, and other
   * features.
   *
   * @param pModel
   */
  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING, 
      defaultValue = Component.RASPBERRYPI_SERVER_VALUE)
  @SimpleProperty(description = "Sets the model version of the Raspberry Pi. "
      + "Depending on this model input, we will perform pin validation in the RaspberryPiPinClient inputs.", 
      userVisible = true)
  public void Model(String pModel) {
    model = pModel;
    if (pModel.equals("Pi1A") || pModel.equals("Pi1B")){
      pins = 26;
    } else if (pModel.equals("Pi1A+") || pModel.equals("Pi1B+") || pModel.equals("Pi2B") || pModel.equals("Pi3B")){
      pins = 40;
    } else {
      Log.e(LOG_TAG, "Unsupported RasberryPi Model");
    } 
  }

  /**
   * Returns the model of the RaspberryPiServer.
   *
   * @return model
   */
  @SimpleProperty(description = "Returns the model type of the RaspberryPi Server", 
      category = PropertyCategory.BEHAVIOR, 
      userVisible = true)
  public String Model() {
    return model;
  }

  /**
   * The IP Address of the Raspberry Pi device.  The MQTT broker should be reachable via this address.
   * 
   * @param pModel
   */
  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING, 
      defaultValue = Component.RASPBERRYPI_SERVER_IPV4_VALUE)
  @SimpleProperty(description = "The IP Address of the Raspberry Pi device.  The MQTT broker should be reachable via this address.", 
      userVisible = true)
  public void Ipv4Address(String pIpv4Address) {
    ipv4Address = pIpv4Address;
    // TODO Validate the ipAddress and see it is reachable
  }

  /**
   * Returns the ipv4 address of the RaspberryPiServer.
   *
   * @return Ipv4Address
   */
  @SimpleProperty(description = "Returns the model type of the RaspberryPi Server", 
      category = PropertyCategory.BEHAVIOR, 
      userVisible = true)
  public String Ipv4Address() {
    return ipv4Address;
  }
  
  /**
   * The TCP/IP port that the MQTT broker on the RaspberryPi is running on.
   * 
   * @param pPort
   */
  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_NON_NEGATIVE_INTEGER, 
      defaultValue = Component.RASPBERRYPI_SERVER_PORT_VALUE + "")
  @SimpleProperty(description = "The TCP/IP port that the MQTT broker on the RaspberryPi is running on.", 
      userVisible = true)
  public void Port(int pPort) {
    port = pPort;
    // TODO Validate the port
  }

  /**
   * Returns the TCP/IP port that the MQTT broker on the RaspberryPi is running on.
   *
   * @return port
   */
  @SimpleProperty(description = "Returns the TCP/IP port that the MQTT broker on the RaspberryPi is running on.", 
      category = PropertyCategory.BEHAVIOR, 
      userVisible = true)
  public int Port() {
    return port;
  }
  
  /**
   * Quality of Service parameter for a RaspberryPi Server broker, which guarantees the received status of the messages.
   * 
   * @param pQos
   */
  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_NON_NEGATIVE_INTEGER, 
      defaultValue = Component.RASPBERRYPI_SERVER_QOS_VALUE + "")
  @SimpleProperty(
      description = "Quality of Service parameter for a RaspberryPi Server broker, "
          + "which guarantees the received status of the messages.", 
      userVisible = true)
  public void Qos(int pQos) {
    if (pQos > 2) {
      Log.e(LOG_TAG, "QOS was " + pQos + ". It has to be either 0, 1 or 2.");
    }
    qos = pQos;
    
  }

  /**
   * Returns the Quality of Service parameter for a RaspberryPi Server broker, which guarantees the received status of the messages.
   *
   * @return model
   */
  @SimpleProperty(description = "Returns the Quality of Service parameter for a RaspberryPi Server broker, "
      + "which guarantees the received status of the messages.", 
      category = PropertyCategory.BEHAVIOR, 
      userVisible = true)
  public String Qos() {
    return model;
  }
  
  /**
   * Checks if the given pin can work on the RaspberryPi model being used.
   * @param pPinNumber
   * @return true if this pin is available on the specified RaspberryPi model.
   */
  @SimpleFunction(description="Returns true if this pin is available on the specified RaspberryPi model.")
  public boolean HasPin(int pPinNumber){
    return pPinNumber <= pins && pPinNumber > 0;
  }

  /**
   * Shutdown the RaspberryPi GPIO provider.
   */
  @SimpleFunction(description="Shutdown the RaspberryPi GPIO provider.")
  public void Shutdown(){
    if (DEBUG) {
      Log.d(LOG_TAG, "Shutting down the RaspberryPi Server...");
      Log.d(LOG_TAG, "To be implemented.");
    }
    shutdown  = true;
    //TODO pass the message to the RaspberryPiAppInventorCompanion
  }
  
  /**
   * Determines the status of the RaspberryPi.
   * @return true if the RaspberryPi GPIO provider has been shutdown.
   */
  @SimpleFunction(description="Returns true if the RaspberryPi GPIO provider has been shutdown.")
  public boolean IsShutdown() {
    return shutdown;
  }
  
  /**
   * Connected clients are the active devices through the MQTT broker.
   * @return the number of connected clients.
   */
  @SimpleFunction(description="Returns the number of connected clients.")
  public int ConnectedClients(){
    if (DEBUG) {
      Log.d(LOG_TAG, "Getting the connected clients...");
      Log.d(LOG_TAG, "To be implemented.");
    }
    //TODO pass the message to the RaspberryPiAppInventorCompanion
   return 0; 
  }
  
  /**
   * Disconnected Clients were once connected to the MQTT broker.
   * @return the number of clients that were registered with the RaspberryPi broker, but are now disconnected.
   */
  @SimpleFunction(description="Returns the number of clients that were registered with the RaspberryPi broker, but are now disconnected.")
  public int DisconnectedClients(){
    if (DEBUG) {
      Log.d(LOG_TAG, "Getting the disconnected clients...");
      Log.d(LOG_TAG, "To be implemented.");
    }
    //TODO pass the message to the RaspberryPiAppInventorCompanion
   return 0; 
  }
  
  /**
   * Returns true if the client with the given pin number is connected.
   * @param pPin
   * @return true if the client with the given pin number is connected.
   */
  @SimpleFunction(description="Returns true if the client with the given pin number is connected.")
  public boolean IsClientConnected(int pPin) {
    if (DEBUG) {
      Log.d(LOG_TAG, "Checking if the client is connected...");
      Log.d(LOG_TAG, "To be implemented.");
    }
    //TODO pass the message to the RaspberryPiAppInventorCompanion
   return false; 
  }
  
  /**
   * Event handler when RaspberryPiServer has shutdown.
   * @return true when the server has shutdown.
   */
  @SimpleEvent(description="Event handler when RaspberryPiServer has shutdown.")
  public boolean HasShutdown(){
    if (DEBUG) {
      Log.d(LOG_TAG, "Handle an event if the RaspberryPi Server has shutdown.");
      Log.d(LOG_TAG, "To be implemented.");
    }
    //TODO subscribe to a message from the broker via the RaspberryPiAppInventorCompanion
   return false; 
  }
  
  /**
   * Event handler when a certain client attached to a pin has connected.
   * @return true when the given client has connected.
   */
  @SimpleEvent(description="Event handler when a certain client has connected.")
  public boolean ClientConnected(int pPin){
    if (DEBUG) {
      Log.d(LOG_TAG, "Handle an event if a client has connected.");
      Log.d(LOG_TAG, "To be implemented.");
    }
    //TODO subscribe to a message from the broker via the RaspberryPiAppInventorCompanion
   return false; 
  }
  
  @SimpleEvent(description="Event handler when a certain client attached to a pin has disconnected")
  public boolean ClientDisconnected(int pPin){
    if (DEBUG) {
      Log.d(LOG_TAG, "Handle an event if a client has diconnected.");
      Log.d(LOG_TAG, "To be implemented. ");
    }
    //TODO subscribe to a message from the broker via the RaspberryPiAppInventorCompanion.
    return false;
  }
}