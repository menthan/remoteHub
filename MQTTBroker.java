/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package remotehub;

import java.io.UnsupportedEncodingException;
import static java.util.logging.Level.INFO;
import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttException;
import static remotehub.MessageHub.LOGGER;

/**
 * Filters MQTT messages for RelayOutput changes and propagates them to Output Hub
 *
 * @author menthan
 */
public class MQTTBroker {

    private final IMqttAsyncClient client;

    private static final String SERVER_URI = "tcp://localhost";

    public MQTTBroker(String clientId, MqttCallback callback) throws MqttException {
        this.client = new MqttAsyncClient(SERVER_URI, clientId);

        client.setCallback(callback);
        LOGGER.log(INFO, "connecting");
        client.connect().waitForCompletion();
        client.subscribe("#", 1);
    }

    public void publish(String topic, String message) throws UnsupportedEncodingException, MqttException {
//        if (subscription.isComplete()) {
//            final MqttMessage mqttMessage = new MqttMessage(message.getBytes());
//            client.publish(topic, mqttMessage);
//        } // WARNING: publish causes connection loss due to null pointer
    }

}
