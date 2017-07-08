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

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinState;
import static com.pi4j.io.gpio.RaspiPin.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Timer;
import java.util.logging.Level;
import static remotehub.MessageHub.LOGGER;

/**
 *
 * @author menthan
 */
public class GpioBroker {

    private final GpioPinDigitalOutput addressPinA;
    private final GpioPinDigitalOutput addressPinB;
    private final GpioPinDigitalOutput addressPinC;
    private final GpioPinDigitalOutput dataPin;
    private final List<GpioPinDigitalOutput> enablePins = new ArrayList<>();

    private static final Map<RelayOutput, Boolean> STATES = new HashMap<>();
    private final List<Integer> wiredAddresses = Arrays.asList(7, 6, 5, 4, 2, 1);
    final int relaisPerEnable = wiredAddresses.size();

    private final List<Pin> enablePinList = Arrays.asList(
            GPIO_16, // Testwise changed pin connection from GPIO_01
            GPIO_08, GPIO_09, GPIO_07, GPIO_00, GPIO_02, GPIO_03, GPIO_12, GPIO_13,
            GPIO_14, GPIO_21, GPIO_22, GPIO_23, GPIO_24, GPIO_25);

    private final List<RelayOutput> relays = new ArrayList<>();

    private final GpioController gpio;

    public GpioBroker(final Properties relayProperties) {
        gpio = GpioFactory.getInstance();

        // assign address pins
        addressPinA = gpio.provisionDigitalOutputPin(GPIO_29, PinState.LOW);
        addressPinB = gpio.provisionDigitalOutputPin(GPIO_28, PinState.LOW);
        addressPinC = gpio.provisionDigitalOutputPin(GPIO_27, PinState.LOW);
        // assign data pin
        dataPin = gpio.provisionDigitalOutputPin(GPIO_26, PinState.LOW);
        // assign Enable Pins
        enablePinList.forEach(pin -> enablePins.add(gpio.provisionDigitalOutputPin(pin, PinState.HIGH)));

        relayProperties.forEach((key, value) -> assignRelayOutput(value.toString().trim(), Integer.parseInt(key.toString())));
        setInitialStates();
    }

    private void assignRelayOutput(String relayName, Integer order) {
        int wiredAddressNo = order % relaisPerEnable;
        if (order % 30 >= 12) { // reverse order for lower banks
            wiredAddressNo = relaisPerEnable - 1 - wiredAddressNo;
        } // else normal order for upper banks
        relays.add(createRelayOutput(relayName,
                enablePins.get(order / relaisPerEnable),
                wiredAddresses.get(wiredAddressNo)));
    }

    private RelayOutput createRelayOutput(String name,
            GpioPinDigitalOutput enablePin,
            Integer latchAddress) {
        return new RelayOutput(name, enablePin, addressPinA, addressPinB,
                addressPinC, dataPin, LatchAddress.getAddress(latchAddress));
    }

    synchronized void set(String relay, Boolean state) {
        getRelay(relay)
                .ifPresent(ro -> {
                    ro.setState(state);
                    // store message to hash map
                    STATES.put(ro, state);
                });

        if ("test".equalsIgnoreCase(relay)) {
            test();
        }
    }

    void pulse(String relay, Integer timeInMs) {
        new Timer().schedule(new PulseTask(this, relay), timeInMs);
    }

    private Optional<RelayOutput> getRelay(String relay) {
        return relays.stream()
                .filter(ro -> ro.name.equalsIgnoreCase(relay))
                .findAny();
    }

    synchronized void toggle(String relay) {
        getRelay(relay)
                .ifPresent(ro -> {
                    final boolean newState = !STATES.get(ro);
                    ro.setState(newState);
                    STATES.put(ro, newState);
                });
    }

    void reapplySet() {
        // execute all value set from hash set again
        LOGGER.log(Level.WARNING, "ToDo: Fix the dirty reapplySet workaround!\n");
        STATES.forEach((ro, state) -> {
            ro.setState(state);
            LOGGER.log(Level.INFO, "Reapply " + ro.name + " " + state);
        });
    }

    private void test() {
        reapplySet();
    }

    private void setInitialStates() {
        relays.forEach(relay -> STATES.put(relay, Boolean.FALSE));
    }
}
