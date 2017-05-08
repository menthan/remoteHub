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

/**
 *
 * @author menthan
 */
public class LatchAddress {

    public final Boolean A;
    public final Boolean B;
    public final Boolean C;

    public LatchAddress(Boolean A, Boolean B, Boolean C) {
        this.A = A;
        this.B = B;
        this.C = C;
    }

    public static LatchAddress getAddress(Integer address) {
        return new LatchAddress((address & 0x01) != 0,
                (address & 0x02) != 0,
                (address & 0x04) != 0);
    }

}
