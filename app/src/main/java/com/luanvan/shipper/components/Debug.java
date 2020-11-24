package com.luanvan.shipper.components;

import android.os.Environment;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Debug {
    public static void writeLog(String text){
        File logFile = new File(Environment.getExternalStorageDirectory()+"/Android/media/com.luanvan.shipper","log.txt");
        logFile.getParentFile().mkdirs();

        try {
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(logFile, true));
            bufferedWriter.append(text);
            bufferedWriter.newLine();
            bufferedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
