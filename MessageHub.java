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
// TODO log to file!

    static final Logger LOGGER = Logger.getLogger(MessageHub.class.getName());
    /**
     *
     * @author menthan
     */
    final Properties relayProperties = new Properties();

    private final GpioBroker gpioBroker;
    private final MQTTBroker mqttListener;

    // dirty workaround!
    private static int reapplyCounter = 0;
    private static final int REAPPLY_TIMEOUT = 30;
    private static final Map<String, String> SENSORS = new HashMap<>();

    public static void main(String[] args) {

        // Testing purposes
        SENSORS.put("1401678/REED", "GarageUG/Licht");

        final String GLASTUER = "2888760/";
        final String KIND_SUED = "2890171/";
        final String BAD = "2890298/";
        final String GARAGE = "2887877/";
        final String KIND_SUED2 = "2888963/";
        final String SCHLAFEN = "notassigned";
//        final String TBD = "2889695";
        //final String WC_EG = "2890213/";
        final String SPEIS = "2227448/";
        final String FLUR_EG = "2889615/";

        SENSORS.put(GLASTUER + "D1", "Erker/Wandlicht");
        SENSORS.put(GLASTUER + "D2", "Erker/Deckenlicht");
        SENSORS.put(GLASTUER + "D3", "Speis/Licht");
        SENSORS.put(GLASTUER + "D4", "Wohnen/Wandlicht");
        SENSORS.put(GLASTUER + "D6", "Kueche/Licht2");
        SENSORS.put(GLASTUER + "D7", "Kueche/LichtLED");
        SENSORS.put(KIND_SUED + "D1", "KindSued/Licht");
        SENSORS.put(KIND_SUED + "D2", "KindSued/Steckdose");
        SENSORS.put(KIND_SUED + "D3", "KindSued/RoWest/AUF");
        SENSORS.put(KIND_SUED + "D4", "KindSued/RoWest/AB");
        SENSORS.put(KIND_SUED2 + "D1", "KindSued/Licht");
        SENSORS.put(KIND_SUED2 + "D2", "KindSued/Steckdose");
        SENSORS.put(KIND_SUED2 + "D3", "KindSued/RoWest/AUF");
        SENSORS.put(KIND_SUED2 + "D4", "KindSued/RoWest/AB");
        SENSORS.put(BAD + "D1", "Bad/Spiegellicht");
        SENSORS.put(BAD + "D2", "Bad/Duschlicht");
        SENSORS.put(BAD + "D3", "Bad/LichtLED");
        SENSORS.put(BAD + "D4", "Bad/Licht");
        SENSORS.put(GARAGE + "D1", "Garage/Tor/AUF");
        SENSORS.put(GARAGE + "D2", "Garage/Tor/AB");
        SENSORS.put(GARAGE + "D3", "Garage/Licht");
        SENSORS.put(GARAGE + "D4", "Aussen/Garagenlicht");
        SENSORS.put(SCHLAFEN + "D1", "Schlafen/LichtBett");
        SENSORS.put(SCHLAFEN + "D2", "Schlafen/Licht");
        SENSORS.put(SCHLAFEN + "D3", "Schlafen/Licht");
        SENSORS.put(SCHLAFEN + "D4", "Schlafen/Licht");
//        assigns.put(WC_EG + "D1", "WCEG/Licht");
//        assigns.put(WC_EG + "D3", "FlurEG/Licht");
        SENSORS.put(FLUR_EG + "D1", "WCEG/Licht");
        SENSORS.put(FLUR_EG + "D2", "TrphOG/Licht");
        SENSORS.put(FLUR_EG + "D3", "FlurEG/Licht");
        SENSORS.put(FLUR_EG + "D4", "TrphUG/Licht");
        SENSORS.put(SPEIS + "D1", "Speis/Licht");
        SENSORS.put(SPEIS + "D2", "Speis/Licht");
        // assigns.put(SPEIS + "D3", "n.a.");
        SENSORS.put(SPEIS + "D4", "Kueche/Licht2");
        SENSORS.put(SPEIS + "D5", "Erker/Wandlicht");

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

    public MessageHub() throws MqttException, IOException {
        //load sensor mapping properties
        relayProperties.load(MessageHub.class.getClassLoader().getResourceAsStream("relays.properties"));
        gpioBroker = new GpioBroker(relayProperties);
        mqttListener = new MQTTBroker(this.getClass().getName(), this);

        //load sensor mapping properties
//        final Properties sensorProps = new Properties();
//        sensorProps.load(MessageHub.class.getClassLoader().getResourceAsStream("sensors.properties"));
//        sensorProps.forEach((key, value) -> SENSORS.put(key.toString(), value.toString()));
//        SENSORS.forEach((key, value) -> System.out.println(key + " " + value));
    }

    @Override
    public void connectionLost(Throwable thrwbl) {
        LOGGER.log(Level.WARNING, "connection lost");
    }

    @Override
    public void messageArrived(String topic, MqttMessage mm) throws Exception {
        final String message = mm.toString();
        final String output = isSensorEvent(topic) ? SENSORS.get(topic) : topic;

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
            if ("PRESSED".equalsIgnoreCase(message)) {
                if (output.contains("Tor")) {
                    // garage door pressed
                    gpioBroker.pulse(output, GARAGE_DOOR_PULSE);
                } else {
                    gpioBroker.toggle(output);
                }

            } else // this is the remnant of a pir event
            {
                if (!(dayTime() && output.toLowerCase().contains("licht"))) {
                    gpioBroker.set(output, "1".equals(message));
                }
            }
        } else {
//            LOGGER.log(Level.INFO, "Unrecognized message: ".concat(output.concat(message)));
        }
    }

    private static boolean isSensorEvent(final String topic) {
        return SENSORS.containsKey(topic);
    }
    private static final int GARAGE_DOOR_PULSE = 200;

    private void deactivateShutter(final String topic,
            final String currentDirection, final String oppositeDirection) {
        if (topic.endsWith(currentDirection)) {
            gpioBroker.set(topic.replace(currentDirection, oppositeDirection), false);
        }
    }

    private boolean dayTime() {
        final LocalTime DAWN = LocalTime.of(10, 0);
        final LocalTime DUSK = LocalTime.of(17, 0);
        final LocalTime now = LocalTime.now();
        return now.isAfter(DAWN) && now.isBefore(DUSK);
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken imdt) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
