/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package remotehub;

import static java.util.logging.Level.INFO;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import static remotehub.MessageHub.LOGGER;

/**
 * Filters MQTT messages for RelayOutput changes and propagates them to Output Hub
 *
 * @author menthan
 */
public class MQTTListener {

    private final MqttClient client;

    private static final String SERVER_URI = "tcp://localhost";

    public MQTTListener(String clientId, MqttCallback callback) throws MqttException {
        this.client = new MqttClient(SERVER_URI, clientId);

        client.setCallback(callback);
        LOGGER.log(INFO, "connecting");
        client.connect();
        client.subscribe("#");
    }

}
