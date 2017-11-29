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
import java.util.Calendar;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
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
    private final Timer blindsUpTimer = new Timer();
    private final Timer blindsDownTimer = new Timer();

    private static final Integer GARAGE_DOOR_PULSE = 200;
    private static final Integer JALOUSIE_PULSE = 60000;

    public static void main(String[] args) {

        try {
            addFileLogger();
            LOGGER.info("Remote Hub V 0.3");
            LOGGER.setLevel(Level.INFO);

            new MessageHub();
        } catch (MqttException | IOException | SecurityException ex) {
            LOGGER.severe(ex.getMessage());
        }
    }

    private static void addFileLogger() {
        FileHandler fh;
        SimpleFormatter formatter = new SimpleFormatter();

        try {
            fh = new FileHandler("/var/log/remoteHub/remoteHub.log");
            LOGGER.addHandler(fh);
            fh.setFormatter(formatter);
        } catch (IOException | SecurityException ex) {
            LOGGER.log(Level.SEVERE, "could not open log file: {0}", ex.toString());
        }
    }

    public MessageHub() throws MqttException, IOException {
        this.sun = new SunRiseSet(48.282622, 9.899724, 2);

        //load properties
        relayProperties.load(MessageHub.class.getClassLoader().getResourceAsStream("relays.properties"));
        sensorProperties.load(MessageHub.class.getClassLoader().getResourceAsStream("sensors.properties"));

        eventMapper = new EventMapper(sensorProperties);
        gpioBroker = new GpioBroker(relayProperties);
        mqttBroker = new MQTTBroker(this.getClass().getName(), this);

        scheduleBlinds(blindsUpTimer, "Up");
        scheduleBlinds(blindsDownTimer, "Down");

        while (true) {

            try {
                Thread.sleep(60000);
            } catch (InterruptedException ex) {
                Logger.getLogger(MessageHub.class.getName()).log(Level.SEVERE, null, ex);
                break;
            }
        }
    }

    private void scheduleBlinds(Timer timer, String direction) {
        final LocalTime time = sun.getTime(direction);
        final Calendar date = Calendar.getInstance();

        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MILLISECOND, 0);
        if (direction.equalsIgnoreCase("up")) {
            date.set(Calendar.HOUR_OF_DAY, 7);
            date.set(Calendar.MINUTE, 0);
        } else { // shut blinds one hour after sunset
            date.set(Calendar.HOUR_OF_DAY, time.plusHours(1).getHour());
            date.set(Calendar.MINUTE, time.plusHours(1).getMinute());
        }

        LOGGER.info("schedule blinds " + direction + " for " + date.getTime());
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                moveBlinds(direction);
                LOGGER.info("scheduled blinds " + direction);
            }
        }, date.getTime(), 1000 * 60 * 60 * 24);
    }

    /* Direction can be 'Up', 'Down' or 'Stop' (case agnostic) */
    private void moveBlinds(String direction) {
        //open/close all blinds
        relayProperties.forEach((key, value) -> {
            final String relais = value.toString().trim().toLowerCase();
            if (isRollerShutter(relais)
                    && (relais.contains("kueche") || relais.contains("erker") || relais.contains("terrasse"))
                    && relais.contains("/ab")) {
                execute(new SwitchAction(relais.replace("/ab", ""), direction.toLowerCase()));
                LOGGER.finer("moveBlinds: " + relais + direction);
            }
        });
    }

    @Override
    public void connectionLost(Throwable thrwbl) {
        LOGGER.warning("connection lost: ".concat(thrwbl.getCause().toString()));
    }

    @Override
    public void messageArrived(String topic, MqttMessage mm) throws Exception {
        LOGGER.info("Received message: ".concat(topic.concat(mm.toString())));
        final SwitchAction action = eventMapper.map(topic, mm.toString());
        if (eventMapper.isSensorEvent(topic) && !mm.isRetained()) {
            // TODO publish new topic state for sensor events:
            mqttBroker.publish(action.getOutput(), action.getCommand());
        }
        execute(action);
    }

    private void execute(SwitchAction action) {
        final String output = action.getOutput();
        final String command = action.getCommand();
        LOGGER.log(Level.FINER, "Mapped to message: ".concat(output.concat(command)));

        if (isRollerShutter(output) || isGarageDoor(output)) {
            switch (command) {
                case "up":
                    gpioBroker.set(output.concat("/ab"), false);
                    gpioBroker.pulse(output.concat("/auf"), getPulseTime(output));
                    break;
                case "down":
                    gpioBroker.set(output.concat("/auf"), false);
                    gpioBroker.pulse(output.concat("/ab"), getPulseTime(output));
                    break;
                default:
                    gpioBroker.set(output.concat("/ab"), false);
                    gpioBroker.set(output.concat("/auf"), false);
                    break;
            }
            if (isGarageDoor(output)) {
                gpioBroker.pulse("Garage/Licht", 300000);
            }
        } else if (isBinary(output)) {
            if ("on".equals(command) || "true".equals(command)) {
                gpioBroker.set(output, true);
            } else if ("off".equals(command) || "false".equals(command)) {
                gpioBroker.set(output, false);
            } else if (isButtonPress(command)) {
                gpioBroker.toggle(output);
            } else // command is "low" or "high"
             if (output.contains("licht")
                        && (output.contains("eg") || output.contains("og"))) {
                    gpioBroker.set(output, !sun.dayTime() && "high".equals(command));
                } else {
                    gpioBroker.set(output, "high".equals(command));
                }
        } else {
            LOGGER.finest("Unassigned message: ".concat(output.concat(command)));
        }
    }

    private Integer getPulseTime(final String output) {
        return (isGarageDoor(output) ? GARAGE_DOOR_PULSE : JALOUSIE_PULSE);
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken imdt) {
        try {
            LOGGER.fine("Delivered message: ".concat(imdt.getMessage().toString()));
        } catch (MqttException ex) {
            LOGGER.severe(ex.getMessage());
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
        return command.startsWith("press") || command.startsWith("pulse");
    }

}
