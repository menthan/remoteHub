/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package remotehub;

import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinState;
import java.util.logging.Level;
import static remotehub.MessageHub.LOGGER;

/**
 *
 * @author menthan
 */
public class RelayOutput {

    final String name;

    private final GpioPinDigitalOutput addressPinA;
    private final GpioPinDigitalOutput addressPinB;
    private final GpioPinDigitalOutput addressPinC;
    private final GpioPinDigitalOutput dataPin;
    private final GpioPinDigitalOutput latchEnablePin;

    private final LatchAddress address;

    public RelayOutput(String name,
            GpioPinDigitalOutput latchEnablePin, GpioPinDigitalOutput addressPinA,
            GpioPinDigitalOutput addressPinB, GpioPinDigitalOutput addressPinC,
            GpioPinDigitalOutput dataPin, LatchAddress address) {
        this.name = name;
        this.addressPinA = addressPinA;
        this.addressPinB = addressPinB;
        this.addressPinC = addressPinC;
        this.dataPin = dataPin;
        this.latchEnablePin = latchEnablePin;
        this.address = address;
    }

    void enable() {
        addressPinA.setState(address.A);
        addressPinB.setState(address.B);
        addressPinC.setState(address.C);
        latchEnablePin.setState(PinState.LOW);
    }

    void disable() {
        latchEnablePin.setState(PinState.HIGH);
    }

    void inhibit(Boolean data) {
        enable();
        dataPin.setState(data);
        // wait inhibition time
        try {
            Thread.sleep(
                    5);
        } catch (InterruptedException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
        disable();
    }
}
