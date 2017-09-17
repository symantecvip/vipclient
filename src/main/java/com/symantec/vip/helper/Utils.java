package com.symantec.vip.helper;

/**
 * Created by shantanu_gattani
 */
public class Utils {

    public static final String byteArrayToHexString(byte input[]) {
        int i = 0;
        if (input == null || input.length <= 0)
            return null;

        char lookupArray[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C',
                'D', 'E', 'F' };
        char[] result = new char[input.length * 2];

        while (i < input.length) {
            result[2 * i] = lookupArray[(input[i] >> 4) & 0x0F];
            result[2 * i + 1] = lookupArray[(input[i] & 0x0F)];
            i++;
        }
        return String.valueOf(result);
    }

}
