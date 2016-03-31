package fr.cnrs.liris.SimAttack.Util;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by apetit on 28/09/15.
 */
public class Util {

    public static List<String> read(String pathname) {
        if (pathname == null) {
            throw new NullPointerException();
        }
        return read(new File(pathname));
    }

    public static List<String> read(File file) {
        List<String> lines = new ArrayList<>();
        String currentLine;
        try {
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                while ((currentLine = br.readLine()) != null) {
                    lines.add(currentLine);
                }
            }
        } catch (IOException e) {
            System.err.println("An error occured while reading: "+file);
            e.printStackTrace();
        }
        return lines;
    }

    public static void save(Collection<String> data, String file) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            for (String keyword : data) {
                bw.write(keyword + "\n");
            }
        } catch (IOException e) {
            System.err.println("An error occured while writing: "+file);
            e.printStackTrace();
        }
    }

    public static String extractFileName(String filePathName) {
        if (filePathName == null)
            return null;

        int dotPos = filePathName.lastIndexOf('.');
        int slashPos = filePathName.lastIndexOf('\\');
        if (slashPos == -1)
            slashPos = filePathName.lastIndexOf('/');

        if (dotPos > slashPos) {
            return filePathName.substring(slashPos > 0 ? slashPos + 1 : 0, dotPos);
        }

        return filePathName.substring(slashPos > 0 ? slashPos + 1 : 0);
    }
}
