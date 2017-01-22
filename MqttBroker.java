/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package remotehub;

import java.util.logging.Level;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import static remotehub.MessageHub.LOGGER;

/**
 * Filters MQTT messages for RelayOutput changes and propagates them to Output Hub
 *
 * @author menthan
 */
public class MqttBroker implements MqttCallback {

    private final MqttClient client;
    private final GpioBroker broker;

    private static final String SERVER_URI = "tcp://localhost";

    public MqttBroker(String clientId, GpioBroker broker) throws MqttException {
        this.client = new MqttClient(SERVER_URI, clientId);

        client.setCallback(this);
        client.connect();
        LOGGER.log(Level.INFO, "connected.");
        client.subscribe("#");

        this.broker = broker;
    }

    @Override
    public void connectionLost(Throwable thrwbl) {
        LOGGER.log(Level.WARNING, "connection lost, reconnecting..");
        try {
            client.connect();
            LOGGER.log(Level.INFO, "connected");
            client.subscribe("#");
        } catch (MqttException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage());
        }
    }

    @Override
    public void messageArrived(String topic, MqttMessage mm) throws Exception {
        // SAFETY: If rollershutter is actuated directly, deactivate other direction first.
        deactivateShutter(topic, "/AB", "/AUF");
        deactivateShutter(topic, "/AUF", "/AB");

        if ("ON".equalsIgnoreCase(mm.toString())) {
            broker.set(topic, true);
        } else if ("OFF".equalsIgnoreCase(mm.toString())) {
            broker.set(topic, false);
        } else if ("UP".equalsIgnoreCase(mm.toString())) {
            broker.set(topic.concat("/AB"), false);
            broker.set(topic.concat("/AUF"), true);
        } else if ("DOWN".equalsIgnoreCase(mm.toString())) {
            broker.set(topic.concat("/AUF"), false);
            broker.set(topic.concat("/AB"), true);
        } else if ("STOP".equalsIgnoreCase(mm.toString())) {
            broker.set(topic.concat("/AB"), false);
            broker.set(topic.concat("/AUF"), false);
        } else {
            LOGGER.log(Level.FINEST, "Unrecognized message: ".concat(topic.concat(mm.toString())));
        }
    }

    private void deactivateShutter(final String topic,
            final String currentDirection, final String oppositeDirection) {
        if (topic.endsWith(currentDirection)) {
            broker.set(topic.replace(currentDirection, oppositeDirection), false);
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken imdt) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
