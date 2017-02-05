package ch.zhaw;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        String originalFile = "/home/slang/Documents/git/BookingAnalyzerGeocoding/u611a_10_normalized.csv";
        String newFile = "/home/slang/Documents/git/BookingAnalyzerGeocoding/u611a_10_normalized_geocoded.csv";
        String currentWorkingLineFile = "/home/slang/Documents/git/BookingAnalyzerGeocoding/currentWorkingLine.txt";
        int currentWorkingLine = 0;
        try {
            byte[] bytes = Files.readAllBytes(Paths.get(currentWorkingLineFile));
            String s = new String(bytes);
            s = s.trim();
            currentWorkingLine = Integer.parseInt(s);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        String mapsKey = "AiF20pSFG81-Ja01AykfU_1Sd-L07ee0w6nxBGXYittfm7MkIPZfaWSBFsDxXKf7";
        // Complete url: http://dev.virtualearth.net/REST/v1/Locations?countryRegion=countryRegion&adminDistrict=adminDistrict&locality=locality&postalCode=postalCode&addressLine=addressLine&userLocation=userLocation&userIp=userIp&usermapView=usermapView&includeNeighborhood=includeNeighborhood&maxResults=maxResults&key=BingMapsKey
        String geoCodingServiceUrl = "http://dev.virtualearth.net/REST/v1/Locations?countryRegion={countryIsoCode}&locality={city}&postalCode={postalCode}&addressLine={address}&includeNeighborhood=0&maxResults=1&key=" + mapsKey;
        String line = "";
        String cvsSplitBy = "@";

        FileInputStream inputStream = null;
        Scanner sc = null;

        try {
            inputStream = new FileInputStream(originalFile);
            sc = new Scanner(inputStream, "UTF-8");

            try (FileWriter fw = new FileWriter(newFile, true);
                 BufferedWriter bw = new BufferedWriter(fw);
                 PrintWriter out = new PrintWriter(bw)) {

                // Get line with field names
                line = sc.nextLine();
                Map<String, Integer> pos = GetHeaderPositions(line, cvsSplitBy);
                if (currentWorkingLine == 0) {
                    out.println(line + "@longitude@latitude");
                    currentWorkingLine++;
                }

                // Skip already processed files
                for (int i = 1; i < currentWorkingLine; i++) {
                    sc.nextLine();
                }

                while (sc.hasNextLine()) {
                    System.out.print(currentWorkingLine + "-");
                    line = sc.nextLine();

                    String[] splitLine = line.split(cvsSplitBy);

                    String street = splitLine[pos.get("CUSTRAS")];
                    String country = splitLine[pos.get("CUCNTRY")];
                    String zip = splitLine[pos.get("CUZIP")];
                    String city = splitLine[pos.get("CUORT")];

                    GeoLocation geoLocation = GetGeoLocation(currentWorkingLine, geoCodingServiceUrl, street, zip, city, country);
                    String newLine;
                    if (geoLocation == null) {
                        newLine = line;
                    } else {
                        newLine = line + "@" + geoLocation.Longitude + "@" + geoLocation.Latitude;
                    }
                    out.println(newLine);
                    currentWorkingLine++;

                    // Store current working line
                    try (PrintWriter outLine = new PrintWriter(currentWorkingLineFile)) {
                        outLine.print(currentWorkingLine);
                    }
                }
            }


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (sc != null) {
                sc.close();
            }
        }
    }

    private static GeoLocation GetGeoLocation(int currentWorkingLine, String urlString, String street, String zip, String city, String country) {

        street = encode(currentWorkingLine, street);
        zip = encode(currentWorkingLine, zip);
        city = encode(currentWorkingLine, city);
        country = encode(currentWorkingLine, country);

        urlString = urlString.replace("{address}", street);
        urlString = urlString.replace("{postalCode}", zip);
        urlString = urlString.replace("{city}", city);
        urlString = urlString.replace("{countryIsoCode}", country);

        URL url = null;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            Log(currentWorkingLine, e, "Unable to construct geocoding url");
            return null;
        }
        JSONTokener tokener = null;
        try {
            tokener = new JSONTokener(url.openStream());
        } catch (IOException e) {
            Log(currentWorkingLine, e, "Unable to get geocoding results");
            return null;
        }
        JSONArray coordinates;
        try {
            JSONObject root = new JSONObject(tokener);
            JSONObject resourcesSets = root.getJSONArray("resourceSets").getJSONObject(0);
            JSONArray resources = resourcesSets.getJSONArray("resources");
            JSONObject resource = resources.getJSONObject(0);
            JSONObject point = resource.getJSONObject("point");
            coordinates = point.getJSONArray("coordinates");
        } catch (Exception e) {
            Log(currentWorkingLine, e, "Unable to get coordinates from geocoding request. Url: " + urlString);
            return null;
        }

        return new GeoLocation(coordinates.getDouble(0), coordinates.getDouble(1));
    }

    private static String encode(int currentWorkingLine, String parameter) {
        try {
            parameter = URLEncoder.encode(parameter, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log(currentWorkingLine, e, "value " + parameter + " can not be UTF-8 encoded");
        }
        return parameter;
    }

    private static void Log(int currentWorkingLine, Exception e, final String description) {
        System.out.println("\nLine " + currentWorkingLine + ", " + description + ": " + e.getMessage());
    }

    private static Map<String, Integer> GetHeaderPositions(String line, String cvsSplitBy) {
        Map<String, Integer> headerPositions = new HashMap<>();

        String[] splitLine = line.split(cvsSplitBy);
        int pos = 0;
        for (String header: splitLine) {
            headerPositions.put(header, pos);
            pos++;
        }

        return headerPositions;
    }

}
