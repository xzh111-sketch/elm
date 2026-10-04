package com.lin.elm.conf;

import java.text.SimpleDateFormat;
import java.util.Date;

public class CommonUtil {
    public static String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new Date());
    }
}
