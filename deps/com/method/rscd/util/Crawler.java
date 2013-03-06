package com.method.rscd.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Crawler {
    
    public static String downloadPage(String address) {
        URL url;
        try {
            url = new URL(address);
        } catch (MalformedURLException ex) {
            ex.printStackTrace();
            return null;
        }

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openConnection().getInputStream()));
            StringBuilder page = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null)
                page.append(line);
            return page.toString();
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
    }
    
    public static String extractParameter(String input, int length) {
        if (input == null)
            return null;
        Pattern p = Pattern.compile("value=\"(\\S{" + length + "})\"");
        Matcher m = p.matcher(input);
        if (m.find())
            return m.group(1);
        else
            return null;
    }
    
}
