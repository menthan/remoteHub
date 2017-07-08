/*
 * Copyright (C) 2017 Frederic Lott
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package remotehub;

import java.io.IOException;
import java.time.LocalTime;
import java.time.ZoneId;
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

    private final EventMapper eventMapper;
    private final GpioBroker gpioBroker;
    private final MQTTBroker mqttBroker;

    private final SunRiseSet sun;

    private static final int GARAGE_DOOR_PULSE = 200;

    public static void main(String[] args) {

        try {
            addFileLogger();
            LOGGER.log(Level.INFO, "Remote Hub V 0.3");
            LOGGER.setLevel(Level.FINEST);

            new MessageHub();
        } catch (MqttException | IOException | SecurityException ex) {
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

    public MessageHub() throws MqttException, IOException {
        this.sun = new SunRiseSet(48.282622, 9.899724, 2);

        //load properties
        relayProperties.load(MessageHub.class.getClassLoader().getResourceAsStream("relays.properties"));
        sensorProperties.load(MessageHub.class.getClassLoader().getResourceAsStream("sensors.properties"));

        eventMapper = new EventMapper(sensorProperties);
        gpioBroker = new GpioBroker(relayProperties);
        mqttBroker = new MQTTBroker(this.getClass().getName(), this);

        while (true) {
            final LocalTime now = LocalTime.now(ZoneId.of("Europe/Berlin"));
            if (now.getHour() == 6 && now.getMinute() == 15) {
                moveBlinds(true);
            }
            try {
                Thread.sleep(60000);
            } catch (InterruptedException ex) {
                Logger.getLogger(MessageHub.class.getName()).log(Level.SEVERE, null, ex);
                break;
            }
        }

    }

    private void moveBlinds(Boolean dayTime) {
        //open/close all blinds
        String direction = dayTime ? "Up" : "Down";
        relayProperties.forEach((key, value) -> {
            final String relais = value.toString().toLowerCase()
                    .replace("/auf", "").replace("/ab", "");
            if (relais.contains("/ro/")) {
                final SwitchAction switchAction = new SwitchAction(relais, direction);
                execute(switchAction);
            }
        }
        );
    }

    @Override
    public void connectionLost(Throwable thrwbl) {
        LOGGER.log(Level.WARNING, "connection lost: " + thrwbl.getCause());
    }

    @Override
    public void messageArrived(String topic, MqttMessage mm) throws Exception {
        LOGGER.log(Level.INFO, "Received message: ".concat(topic.concat(mm.toString())));
        final SwitchAction action = eventMapper.map(topic, mm.toString());
        if (eventMapper.isSensorEvent(topic) && !mm.isRetained()) {
            // TODO publish new topic state for sensor events:
            mqttBroker.publish(action.getOutput(), action.getCommand());
        }
        execute(action);
    }

    private static final Integer JALOUSIE_TIME = 60000;

    private void execute(SwitchAction action) {
        final String output = action.getOutput();
        final String command = action.getCommand();
        final boolean garageDoor = isGarageDoor(output);
        //LOGGER.log(Level.INFO, "Mapped to message: ".concat(output.concat(command)));

        if (isRollerShutter(output) || garageDoor) {
            final Integer pulseTime;
            if (garageDoor) {
                pulseTime = GARAGE_DOOR_PULSE;
            } else {
                pulseTime = JALOUSIE_TIME;
            }

            if ("up".equalsIgnoreCase(command)) {
                gpioBroker.set(output.concat("/ab"), false);
                gpioBroker.pulse(output.concat("/auf"), pulseTime);
            } else if ("down".equalsIgnoreCase(command)) {
                gpioBroker.set(output.concat("/auf"), false);
                gpioBroker.pulse(output.concat("/ab"), pulseTime);
            } else {
                gpioBroker.set(output.concat("/ab"), false);
                gpioBroker.set(output.concat("/auf"), false);
            }
        } else if (isBinary(output)) {
            if ("on".equalsIgnoreCase(command) || "true".equalsIgnoreCase(command)) {
                gpioBroker.set(output, true);
            } else if ("off".equalsIgnoreCase(command) || "false".equalsIgnoreCase(command)) {
                gpioBroker.set(output, false);
            } else if (isButtonPress(command)) {
                gpioBroker.toggle(output);
            } else // command is "low" or "high"
             if (output.contains("licht")) {
                    gpioBroker.set(output, !sun.dayTime() && "high".equals(command));
                } else {
                    gpioBroker.set(output, "high".equals(command));
                }
        } else {
            LOGGER.log(Level.FINEST, "Unassigned message: ".concat(output.concat(command)));
        }

    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken imdt) {
        try {
            LOGGER.log(Level.FINEST, "Delivered message: ".concat(imdt.getMessage().toString()));
        } catch (MqttException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }

    private static boolean isGarageDoor(final String output) {
        return output.contains("garage/tor");
    }

    private static boolean isRollerShutter(final String output) {
        return output.contains("/ro/") || output.contains("/ja/");
    }

    private static boolean isBinary(final String output) {
        return output.contains("licht") || output.contains("steckdose") || output.contains("abzug");
    }

    private static boolean isButtonPress(final String command) {
        return command.toLowerCase().startsWith("press") || command.toLowerCase().startsWith("pulse");
    }

}
