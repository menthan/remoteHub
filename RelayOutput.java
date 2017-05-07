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

    private static final int ADDRESS_SETUP_TIME = 35;//15 default
    private static final int PROPAGATION_DELAY = 21;
    private static final int HOLD_TIME = 3;

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

    private void setAddress() {
        addressPinA.setState(address.A);
        addressPinB.setState(address.B);
        addressPinC.setState(address.C);
    }

    private void unSetAddress() {
        addressPinA.setState(PinState.LOW);
        addressPinB.setState(PinState.LOW);
        addressPinC.setState(PinState.LOW);
        dataPin.setState(PinState.LOW);
    }

    void setState(Boolean data) {
        LOGGER.log(Level.FINE, "switching ".concat(name + " " + data));
        setAddress();
        dataPin.setState(data);
        icWait(ADDRESS_SETUP_TIME);
//        latchEnablePin.pulse(PROPAGATION_DELAY, PinState.LOW, true);
        latchEnablePin.setState(PinState.LOW);
        icWait(PROPAGATION_DELAY); // TODO maybe hold time is enough?
        latchEnablePin.setState(PinState.HIGH);
        icWait(PROPAGATION_DELAY);
        unSetAddress();
        icWait(PROPAGATION_DELAY);// TODO maybe solve multiple fast triggering short flashing?
    }

    void pulse(Integer time_in_ms) {
        LOGGER.log(Level.FINE, "pulsing ".concat(name));
        setAddress();
        icWait(ADDRESS_SETUP_TIME);
        latchEnablePin.setState(PinState.LOW);
        dataPin.pulse(time_in_ms, true);
        latchEnablePin.setState(PinState.HIGH);
        unSetAddress();
    }

    private void icWait(final int waitTimeNs) {
        try {
            Thread.sleep(0, waitTimeNs);
        } catch (InterruptedException ex) {
            LOGGER.log(Level.SEVERE, null, ex);
        }
    }
}
