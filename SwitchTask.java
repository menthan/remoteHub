package remotehub;

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


import java.util.TimerTask;
import remotehub.GpioBroker;

/**
 *
 * @author menthan
 */
public class SwitchTask extends TimerTask {

    private final String relay;
    private final GpioBroker broker;

    public SwitchTask(GpioBroker broker, String relay) {
        this.relay = relay;
        this.broker = broker;
    }

    @Override
    public void run() {
        System.out.println("setting " + relay + " false");
        broker.set(relay, Boolean.FALSE);
    }

}
