/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package remotehub;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
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
    private final GpioBroker gpioBroker;
    private final MQTTListener mqttListener;

    // dirty workaround!
    private static int reapplyCounter = 0;
    private static final int REAPPLY_TIMEOUT = 10;
    private static Map<String, String> assigns = new HashMap<>();

    public static void main(String[] args) {
        assigns.put("1401678/REED", "GarageUG/Licht");
        assigns.put("2890213/D1", "WCEG/Licht");
        assigns.put("2890213/D3", "FlurEG/Licht");
        assigns.put("2889615/D1", "WCEG/Licht");

        // 2888760
        assigns.put("2888760/D1", "Erker/Wandlicht");
        assigns.put("2888760/D2", "Kueche/Licht2");
        assigns.put("2888760/D3", "Wohnen/Wandlicht");
        assigns.put("2888760/D4", "Speis/Licht");

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

    public MessageHub() throws MqttException {
        gpioBroker = new GpioBroker();
        mqttListener = new MQTTListener(this.getClass().getName(), this);
    }

    @Override
    public void connectionLost(Throwable thrwbl) {
        LOGGER.log(Level.WARNING, "connection lost");
    }

    @Override
    public void messageArrived(String topic, MqttMessage mm) throws Exception {
        final String message = mm.toString();
        final String output = assigns.containsKey(topic) ? assigns.get(topic) : topic;

        // SAFETY: If rollershutter is actuated directly, deactivate other direction first.
        deactivateShutter(output, "/AB", "/AUF");
        deactivateShutter(output, "/AUF", "/AB");

        if ("ON".equalsIgnoreCase(message)) {
            gpioBroker.set(output, true);
        } else if ("OFF".equalsIgnoreCase(message)) {
            gpioBroker.set(output, false);
        } else if ("UP".equalsIgnoreCase(message)) {
            gpioBroker.set(output.concat("/AB"), false);
            gpioBroker.set(output.concat("/AUF"), true);
        } else if ("DOWN".equalsIgnoreCase(message)) {
            gpioBroker.set(output.concat("/AUF"), false);
            gpioBroker.set(output.concat("/AB"), true);
        } else if ("STOP".equalsIgnoreCase(message)) {
            gpioBroker.set(output.concat("/AB"), false);
            gpioBroker.set(output.concat("/AUF"), false);
        } else if (assigns.containsKey(topic)) {
            gpioBroker.set(output, "1".equals(message));
        } else {
            LOGGER.log(Level.INFO, "Unrecognized message: ".concat(output.concat(message)));
        }
    }

    private void deactivateShutter(final String topic,
            final String currentDirection, final String oppositeDirection) {
        if (topic.endsWith(currentDirection)) {
            gpioBroker.set(topic.replace(currentDirection, oppositeDirection), false);
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken imdt) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
