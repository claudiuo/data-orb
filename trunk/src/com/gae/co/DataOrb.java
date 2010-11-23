package com.gae.co;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
public class DataOrb extends HttpServlet {

    private static final int CHANGE_THRESHOLD = 20;
    private static final int LOW_VOLUME = 10000000;
    private static final int HIGH_VOLUME = 400000000;
    private static final int LOW_VOLUME_IND = LOW_VOLUME / 7;	// 1428571
    private static final int HIGH_VOLUME_IND = HIGH_VOLUME / 7;	// 57142857
    private final static Logger Log = Logger.getLogger(DataOrb.class.getName());

    public void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        Log.setLevel(Level.INFO);

        StringBuffer sb = new StringBuffer();
        String errorMsg = null;

        String color = "";
        int intensity = 0;

        // TODO: removed this code for testing so we don't keep calling the service
//        try {
//            URL url = new URL("http://download.finance.yahoo.com/d/quotes.csv?s=%5EDJI&f=sl1d1t1c1ohgv&e=.csv");
//            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
//
//            String line;
//            while ((line = reader.readLine()) != null) {
//                sb.append(line);
//            }
//            reader.close();
//
//        } catch (MalformedURLException e) {
//            errorMsg = handleException(e);
//        } catch (IOException e) {
//            errorMsg = handleException(e);
//        }
//        String result = sb.toString();
        // TODO: end code removed for testing

        String result = "\"^DJI\",10211.07,\"6/11/2010\",\"3:01pm\",+38.54,10166.78,10215.68,10082.71,2746412";
        Log.info("received:" + result);

        // split the CSV fields
        if(!isError(errorMsg)) {
            String[] fields = result.split(",");
            // there should be at least 8 fields
            if(fields.length < 8) {
                errorMsg = "Not enough fields received";
            } else {
                sb = new StringBuffer();
                for (int i = 0; i < fields.length; i++) {
                    // we are requesting CSV so some items may be enclosed in quotes
                    String fieldValue = cleanField(fields[i]);
                    // replace the value with the cleaned up version
                    fields[i] = fieldValue;

                    sb.append(fieldValue);
                    sb.append("-");
                }
            }

            // get the fields we need
            int lastHour = getHour(fields[3]);

            // TODO: this is only TEST CODE: to be removed
            sb.append("=");
            sb.append(lastHour);

            // day
            float dayChange = getNumber(fields[4]).floatValue();
            // TODO: this is only TEST CODE: to be removed
            sb.append("=");
            sb.append(dayChange);

            long volume = getNumber(fields[8]).longValue();
            // TODO: this is only TEST CODE: to be removed
            sb.append("=");
            sb.append(volume);


            // the NYSE closing bell is at 4pm.  A last trade time at or after 4pm
            // means the day is over
            if (lastHour == 4) {
                color = Color.BLUE.toString();
                intensity = 10;
            } else {
                if (dayChange < (-1 * CHANGE_THRESHOLD) ) {
                    color = Color.RED.toString();
                } else if (dayChange > CHANGE_THRESHOLD) {
                    color = Color.GREEN.toString();
                } else {
                    color = Color.BLUE.toString();
                }

                // original code assumed volume range 100M - 400M and subtracting 9
                // from lastHour to get the hours open; however, I discovered that
                // a) volume when market opens is in the 10M range
                // b) subtracting 9 from lastHour gives zero before 10 am
                // so I am subtracting 8 only (to have hours open between 1 and 8)
                // and set the lower volume to 10M instead

                // original comment:
                // A normal volume range is 10M - 400M.  Map these to values between
                // 0 and 100.  Map outliers to -1 or 101.
                // That measuring stick of 100M - 400M per day is spread evenly
                // across the 6.5 (round to 7) hours per day that the NYSE is open.
                // TODO: Improve the granularity here by calculating this based
                // on minutes the market is open.
                if (lastHour < 9) {
                    lastHour += 12;	// normalize to 24 hr time
                }
                // market opens at 9; subtract 8 so we don't have hoursOpnen = 0
                int hoursOpen = lastHour - 8;
                // TODO: this is only TEST CODE: to be removed
                sb.append("=");
                sb.append(hoursOpen);

                int volumeLow = LOW_VOLUME_IND * hoursOpen;
                int volumeHigh = HIGH_VOLUME_IND * hoursOpen;
                intensity = (int)(volume - volumeLow) / ( (volumeHigh - volumeLow) / 100 );

System.out.println(volumeLow);
System.out.println(volumeHigh);
System.out.println(intensity);

                // Set up a base line so the orb is always glowing a bit
                if (intensity < 10) {
                    intensity = 10;
                } else if (intensity > 100) {
                    intensity = 101;
                }
            }

            result = sb.toString();
        } else {
            // there was some problem either with fetching the data
            // use a special value to indicate an error to the Orb
            color = Color.BLUE.toString();
            intensity = 101;	// 101 will be used to indicate an error (blink)
        }

        resp.setContentType("text/plain");
        if(isError(errorMsg)) {
            resp.getWriter().println(errorMsg);
        } else {
            PrintWriter writer = resp.getWriter();
            // print what we got from the service
            writer.println(result);
            // the output to be used by the Arduino
            writer.println("$" + color + "," + intensity);

        }
    }

    private Number getNumber(String val) {
        if(StringUtil.isEmpty(val)) {
            return 0;
        } else {
            try {
                return new BigDecimal(val);
            } catch (NumberFormatException nfe) {
                return 0;
            }
        }
    }

    private int getHour(String timeStr) {
        int dotIndex = timeStr.indexOf(':');
        if(dotIndex > 0) {
            try {
                return Integer.parseInt(timeStr.substring(0, dotIndex));
            } catch (NumberFormatException nfe) {
                return 0;
            }
        }
        return 0;
    }

    private boolean isError(String val) {
        if(StringUtil.isEmpty(val)) {
            return false;
        } else {
            return true;
        }
    }

    private String cleanField(String val) {
        if(StringUtil.isEmpty(val)) {
            return "";
        }
        if(val.startsWith("\"") && val.endsWith("\"")) {
            return val.substring(1, val.length()-1);
        } else {
            return val;
        }
    }

    private String handleException(Exception e) {
        e.printStackTrace();
        return "ERROR " + e.getLocalizedMessage();
    }
}
