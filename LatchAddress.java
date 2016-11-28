/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
