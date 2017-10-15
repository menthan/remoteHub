/*
 * Copyright (C) 2017 menthan
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

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 *
 * @author menthan
 */
public class EventMapper {

    private static final Map<String, String> EVENT_MAP = new HashMap<>();
    private static final Map<String, String> SENSOR_MAP = new HashMap<>();

    public EventMapper(final Properties sensorProperties) {
        sensorProperties.forEach((k, v) -> assignSensors(k.toString(), v.toString()));
        sensorProperties.forEach((k, v) -> assignEvents(k.toString(), v.toString()));
    }

    SwitchAction map(String topic, String message) {
        if (isSensorEvent(topic)) {
            return new SwitchAction(determineOutput(topic), determineCommand(topic, message));
        }
        return new SwitchAction(topic.toLowerCase(), message.toLowerCase());
    }

    private static void assignSensors(String key, String value) {
        final String[] keys = key.split("/");
        if (keys.length == 1) {
            SENSOR_MAP.put(key, value);
        }
    }

    private static void assignEvents(String key, String value) {
        final String[] keys = key.split("/");
        final String sensorName = SENSOR_MAP.get(keys[0]);
        if ((keys.length == 2) && (sensorName != null)) {
            EVENT_MAP.put(sensorName + "/" + keys[1], value);
        }
    }

    private String determineCommand(String topic, String message) {
        // TODO: Map sensor&event to topic&value
        String output = EVENT_MAP.get(topic).toLowerCase();
        if (output.endsWith("ab")) {
            return "down";
        } else if (output.endsWith("auf")) {
            return "up";
        }
        return message.toLowerCase();
    }

    private String determineOutput(String topic) {
        // TODO remove updown crop when press, low, high events are mapped for sensors
        return EVENT_MAP
                .get(topic)
                .toLowerCase()
                .replace("/auf", "")
                .replace("/ab", "");
    }

    boolean isSensorEvent(final String topic) {
        return EVENT_MAP.containsKey(topic);
    }

}
