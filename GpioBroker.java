/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package remotehub;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.Pin;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.PinState;
import static com.pi4j.io.gpio.RaspiPin.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    private static Map<RelayOutput, Boolean> states = new HashMap<>();
    private final List<Integer> wiredAddresses = Arrays.asList(7, 6, 5, 4, 2, 1);

    private final List<Pin> enablePinList = Arrays.asList(
            GPIO_01, GPIO_08, GPIO_09, GPIO_07, GPIO_00, GPIO_02, GPIO_03, GPIO_12, GPIO_13,
            GPIO_14, GPIO_21, GPIO_22, GPIO_23, GPIO_24, GPIO_25);

    private final List<RelayOutput> relays = new ArrayList<>();

    private final List<String> relayNames = Arrays.asList("Buero/Ja1/AUF", "Buero/Ja1/AB",
            "Buero/Ja2/AUF", "Buero/Ja2/AB", "Buero/Ja3/AUF", "Buero/Ja3/AB", "Speis/Ro/AUF",
            "Speis/Ro/AB", "Kueche/RoOst/AUF", "Kueche/RoOst/AB", "Kueche/RoSued/AUF",
            "Kueche/RoSued/AB", "Erker/RoOst/AUF", "Erker/RoOst/AB", "Erker/RoSued/AUF",
            "Erker/RoSued/AB", "Erker/RoWest/AUF", "Erker/RoWest/AB", "Terrasse/JaLinks/AUF",
            "Terrasse/JaLinks/AB", "Terrasse/JaRechts/AUF", "Terrasse/JaRechts/AB",
            "Schlafen/Ro/AUF", "Schlafen/Ro/AB", "KindSued/RoSued/AUF", "KindSued/RoSued/AB",
            "KindSued/RoWest/AUF", "KindSued/RoWest/AB", "KindNord/RoWest/AUF",
            "KindNord/RoWest/AB", "KindNord/RoNord/AUF", "KindNord/RoNord/AB", "Bad/Ro/AUF",
            "Bad/Ro/AB", "FlurUG/Licht", "TrphUG/Licht", "WCUG/Licht", "GarageUG/Licht",
            "Heizraum/Licht", "Buero/Deckenlicht1", "Buero/Wandlicht", "Buero/Deckenlicht2",
            "Aussen/Buerolicht", "Aussen/Buerosteckdose", "Buero/Wandsteckdose/", "Speis/Licht",
            "Kueche/Licht1", "Kueche/Licht2", "Kueche/LichtLED", "Erker/Steckdose",
            "Erker/Wandlicht", "Erker/Deckenlicht", "Erker/Durchgangslicht", "Wohnen/Steckdose",
            "Wohnen/Wandlicht", "Wohnen/Deckenlicht", "Garage/Licht", "Aussen/Garagenlicht",
            "Kueche/Dunstabzug", "WCEG/Licht", "WCEG/Spiegellicht", "Haupteingang/Licht",
            "FlurEG/Licht", "TrphOG/Licht", "Bad/LichtLED", "Bad/Licht",
            "Bad/Duschlicht", "Bad/Spiegellicht", "KindNord/Licht", "KindNord/Steckdose",
            "KindSued/Steckdose", "KindSued/Licht", "Schlafen/LichtBett", "Schlafen/Licht",
            "Schlafen/LichtAnkleide", "FlurOG/Licht", "Buehne/Licht", "Reserve/2",
            "Aussen/LichtTreppeUG", "Balkon/Steckdose", "Balkon/Licht", "Haupteingang/Steckdose",
            "Aussen/Weglicht", "Aussen/LichtWest", "Aussen/LichtWest2", "Reserve/Aussen",
            "Garage/Tor/AUF", "Garage/Tor/AB");
    private final GpioController gpio;

    public GpioBroker() {
        gpio = GpioFactory.getInstance();

        // assign address pins
        addressPinA = gpio.provisionDigitalOutputPin(GPIO_29, PinState.LOW);
        addressPinB = gpio.provisionDigitalOutputPin(GPIO_28, PinState.LOW);
        addressPinC = gpio.provisionDigitalOutputPin(GPIO_27, PinState.LOW);
        // assign data pin
        dataPin = gpio.provisionDigitalOutputPin(GPIO_26, PinState.LOW);
        // assign Enable Pins
        enablePins.forEach(pin -> pin.setPullResistance(PinPullResistance.PULL_UP));
        enablePinList.forEach(pin -> enablePins.add(gpio.provisionDigitalOutputPin(pin, PinState.HIGH)));

        AssignRelayOutputs();
        SetInitialStates();
    }

    private void AssignRelayOutputs() {
        // assign RelayOutputs like ABCDE
        final int relaisPerEnable = wiredAddresses.size();
        for (int i = 0; i < 3 * 30 && i < relayNames.size(); i++) {
            int wiredAddressNo = i % relaisPerEnable;
            if (i % 30 >= 12) { // reverse order for lower banks
                wiredAddressNo = relaisPerEnable - 1 - wiredAddressNo;
            } // else normal order for upper banks
            relays.add(createRelayOutput(relayNames.get(i), enablePins.get(i / relaisPerEnable),
                    wiredAddresses.get(wiredAddressNo)));
        }
    }

    private RelayOutput createRelayOutput(String name,
            GpioPinDigitalOutput enablePin,
            Integer latchAddress) {
        final RelayOutput relayOutput = new RelayOutput(name, enablePin, addressPinA, addressPinB,
                addressPinC, dataPin, LatchAddress.getAddress(latchAddress));
//        System.out.println("relay " + name + " " + enablePin.getName() + " Adress:" + latchAddress);
        return relayOutput;
    }

    synchronized void set(String relay, Boolean state) {
        relays.stream()
                .filter(ro -> ro.name.equalsIgnoreCase(relay))
                .findAny()
                .ifPresent(ro -> {
                    ro.inhibit(state);
                    LOGGER.log(Level.INFO, "switching ".concat(ro.name + " " + state));
                    // store message to hash map
                    states.put(ro, state);
                });

        if ("test".equals(relay.toLowerCase())) {
            test(state);
        }
    }

    void reapplySet() {
        // execute all value set from hash set again
        LOGGER.log(Level.WARNING, "ToDo: Fix the dirty reapplySet workaround!\n");
        states.forEach((ro, state) -> {
            ro.inhibit(state);
            LOGGER.log(Level.WARNING, "Reapply " + ro.name + " " + state);
        });
    }

    private void test(Boolean data) {
        for (int i = 0; i < relayNames.size(); i++) {
            relays.get(i).inhibit(data);
            LOGGER.log(Level.INFO,
                    "switching ".concat(relays.get(i).name + " " + data));
            try {
                Thread.sleep(200);
            } catch (InterruptedException ex) {
                LOGGER.log(Level.SEVERE, null, ex);
            }
            relays.get(i).inhibit(false);
        }
    }

    private void SetInitialStates() {
        relays.forEach(relay -> states.put(relay, Boolean.FALSE));
    }

}
