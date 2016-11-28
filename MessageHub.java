/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package remotehub;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import org.eclipse.paho.client.mqttv3.MqttException;

/**
 *
 * @author menthan
 */
public class MessageHub {

    static final Logger LOGGER = Logger.getLogger(MessageHub.class.getName());

    public static void main(String[] args) {
        try {
            logToFile();
            final GpioBroker broker = new GpioBroker();
            final MqttBroker listener = new MqttBroker("remoteHub", broker);

            while (true) {
                Thread.sleep(10000);
            }
        } catch (MqttException | InterruptedException | IOException | SecurityException ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage());
        }
    }

    private static void logToFile() throws IOException {
        FileHandler fh;
        // This block configure the logger with handler and formatter
        fh = new FileHandler("/var/log/remoteHub.log");
        LOGGER.addHandler(fh);
        SimpleFormatter formatter = new SimpleFormatter();
        fh.setFormatter(formatter);
    }
}
