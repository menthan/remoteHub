/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package remotehub;

import java.io.IOException;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 *
 * @author menthan
 */
public class MessageHub implements MqttCallback {

    static final Logger LOGGER = Logger.getLogger(MessageHub.class.getName());

    final Properties relayProperties = new Properties();
    final Properties sensorProperties = new Properties();

    private final GpioBroker gpioBroker;
    private final MQTTBroker mqttListener;

    // dirty workaround!
    private static int reapplyCounter = 0;
    private static final int REAPPLY_TIMEOUT = 30;
    private static final Map<String, String> EVENTS = new HashMap<>();
    private static final Map<String, String> SENSORS = new HashMap<>();

    public static void main(String[] args) {
        // Testing purposes
        EVENTS.put("1401678/REED", "GarageUG/Licht");

        try {
            addFileLogger();
            LOGGER.log(Level.INFO, "Remote Hub V 0.2");
//            LOGGER.setLevel(Level.WARNING);

            MessageHub messageHub = new MessageHub();

            while (true) {
                tryReaplly(messageHub);
                Thread.sleep(60000);
            }
        } catch (MqttException | InterruptedException | IOException | SecurityException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage());
        }
    }

    private static void addFileLogger() throws IOException, SecurityException {
        FileHandler fh;

        fh = new FileHandler("/var/log/remoteHub/remoteHub.log");
        LOGGER.addHandler(fh);
        SimpleFormatter formatter = new SimpleFormatter();
        fh.setFormatter(formatter);
    }

    private static void tryReaplly(MessageHub messageHub) {
        reapplyCounter = (reapplyCounter + 1) % REAPPLY_TIMEOUT;
        if (reapplyCounter == 0) {
            messageHub.gpioBroker.reapplySet();
        }
    }

    private static void assignSensors(String key, String value) {
        final String[] keys = key.split("/");
        if (keys.length == 1) {
            SENSORS.put(key, value);
        }
    }

    private static void assignEvents(String key, String value) {
        final String[] keys = key.split("/");
        final String sensorName = SENSORS.get(keys[0]);
        if ((keys.length == 2) && (sensorName != null)) {
            EVENTS.put(sensorName + "/" + keys[1], value);
        }
    }

    public MessageHub() throws MqttException, IOException {
        //load properties
        relayProperties.load(MessageHub.class.getClassLoader().getResourceAsStream("relays.properties"));
        sensorProperties.load(MessageHub.class.getClassLoader().getResourceAsStream("sensors.properties"));

        sensorProperties.forEach((k, v) -> assignSensors(k.toString(), v.toString()));
        sensorProperties.forEach((k, v) -> assignEvents(k.toString(), v.toString()));

        gpioBroker = new GpioBroker(relayProperties);
        mqttListener = new MQTTBroker(this.getClass().getName(), this);
    }

    @Override
    public void connectionLost(Throwable thrwbl) {
        LOGGER.log(Level.WARNING, "connection lost");
    }

    @Override
    public void messageArrived(String topic, MqttMessage mm) throws Exception {
        final String message = mm.toString();
        final String output = isSensorEvent(topic) ? EVENTS.get(topic) : topic;

        // SAFETY: If rollershutter is actuated directly, deactivate other direction first.
        deactivateShutter(output, "/AB", "/AUF");
        deactivateShutter(output, "/AUF", "/AB");

        if ("ON".equalsIgnoreCase(message) || "true".equalsIgnoreCase(message)) {
            gpioBroker.set(output, true);
        } else if ("OFF".equalsIgnoreCase(message) || "false".equalsIgnoreCase(message)) {
            gpioBroker.set(output, false);
        } else if ("UP".equalsIgnoreCase(message)) {
            gpioBroker.set(output.concat("/AB"), false);
            gpioBroker.pulse(output.concat("/AUF"), GARAGE_DOOR_PULSE);
        } else if ("DOWN".equalsIgnoreCase(message)) {
            gpioBroker.set(output.concat("/AUF"), false);
            gpioBroker.pulse(output.concat("/AB"), GARAGE_DOOR_PULSE);
        } else if ("STOP".equalsIgnoreCase(message)) {
            gpioBroker.set(output.concat("/AB"), false);
            gpioBroker.set(output.concat("/AUF"), false);
        } else if (isSensorEvent(topic)) {
//            mqttListener.publish(SENSORS.get(topic), message);
            if (isButtonPress(message)) {
                if (output.contains("Tor")) {
                    // garage door pressed
                    gpioBroker.pulse(output, GARAGE_DOOR_PULSE);
                } else {
                    gpioBroker.toggle(output);
                }
            } else // message low or high
            {
                if (!(dayTime() && output.toLowerCase().contains("licht"))) {
                    gpioBroker.set(output, "high".equals(message));
                }
            }
        } else {
//            LOGGER.log(Level.INFO, "Unrecognized message: ".concat(output.concat(message)));
        }
    }

    private static boolean isButtonPress(final String message) {
        return message.toLowerCase().startsWith("press") || message.toLowerCase().startsWith("pulse");
    }

    private static boolean isSensorEvent(final String topic) {
        return EVENTS.containsKey(topic);
    }
    private static final int GARAGE_DOOR_PULSE = 200;

    private void deactivateShutter(final String topic,
            final String currentDirection, final String oppositeDirection) {
        if (topic.endsWith(currentDirection)) {
            gpioBroker.set(topic.replace(currentDirection, oppositeDirection), false);
        }
    }

    private boolean dayTime() {
        final LocalTime DAWN = LocalTime.of(9, 0);
        final LocalTime DUSK = LocalTime.of(19, 0);
        final LocalTime now = LocalTime.now();
        return now.isAfter(DAWN) && now.isBefore(DUSK);
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken imdt) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
