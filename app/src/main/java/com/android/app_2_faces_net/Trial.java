package com.android.app_2_faces_net;

import android.content.Context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Trial {
    public String run(Context context, String string) {
        try {
            Socket socket = new Socket(string, 50001);

            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            out.println("Hello from ");
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "Failed!";
    }
}
