package com.gae.co;

public class StringUtil {

    public static boolean isEmpty(String val) {
        if(val == null || val.length() == 0) {
            return true;
        } else {
            return false;
        }
    }
}
