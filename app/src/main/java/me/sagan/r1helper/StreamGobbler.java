package me.sagan.r1helper;

import java.util.*;
import java.io.*;

public class StreamGobbler extends Thread
{
    public InputStream is;
    public String type;
    public String output = "";

    public StreamGobbler(InputStream is, String type)
    {
        this.is = is;
        this.type = type;
    }

    public void run() {
        try
        {
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line=null;
            while ( (line = br.readLine()) != null) {
                output += line + "\n";
            }
        } catch (IOException ioe)
        {
            ioe.printStackTrace();
        }
    }
}