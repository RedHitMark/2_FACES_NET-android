package com.android.app_2_faces_net;

import android.content.Context;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class Trial {
    public String run(Context context) {
        try {
            URL url = new URL("https://generatoredigoverni.it/quirinale.php");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();

            con.setRequestMethod("GET");

            return String.valueOf(con.getResponseCode());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "Failed!";
    }
}
