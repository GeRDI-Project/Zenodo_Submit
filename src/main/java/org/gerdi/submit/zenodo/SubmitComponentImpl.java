package org.gerdi.submit.zenodo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.gerdi.submit.component.AbstractSubmitComponent;
import org.gerdi.submit.security.GeRDIUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

@Component
public class SubmitComponentImpl extends AbstractSubmitComponent {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubmitComponentImpl.class);
    private static final String BOUNDARY =  "*****"+Long.toString(System.currentTimeMillis())+"*****";
    private static final byte[] LINE_FEED = "\r\n".getBytes();

    @Override
    public String submitData(final String s, final JsonNode metadata, final List<String> list, final GeRDIUser geRDIUser) {
        // Create empty upload
        final URL createurl;
        try {
            createurl = new URL("https://zenodo.org/api/deposit/depositions?access_token=xd1GzkuWtIyXJeL3r3yPxsZ8oe3JEd4Zod0tceTZWEVBnAw22YLIndcp9ssD");
        } catch (MalformedURLException e) {
            LOGGER.error("Malformed URL", e);
            return "{\"message\":\"Internal error\"}";
        }
        int id = createEmptyUpload(createurl);
        if (id == -1) {
            return "{\"message\":\"Could not create empty upload.\"}";
        }

        // Transmit metadata
        final URL metadataurl;
        try {
            metadataurl = new URL("https://zenodo.org/api/deposit/depositions/" + id + "?access_token=xd1GzkuWtIyXJeL3r3yPxsZ8oe3JEd4Zod0tceTZWEVBnAw22YLIndcp9ssD");
        } catch (MalformedURLException e) {
            LOGGER.error("Malformed URL", e);
            return "{\"message\":\"Internal error\"}";
        }
        String metadataErrorResponse = sendMetadata(metadata, metadataurl);
        if (metadataErrorResponse != null) return metadataErrorResponse;

        // Transmit data
        final URL dataurl;
        try {
            dataurl = new URL("https://zenodo.org/api/deposit/depositions/" + id + "/files?access_token=xd1GzkuWtIyXJeL3r3yPxsZ8oe3JEd4Zod0tceTZWEVBnAw22YLIndcp9ssD");
        } catch (MalformedURLException e) {
            LOGGER.error("Malformed URL", e);
            return "{\"message\":\"Internal error\"}";
        }
        for (String file: list) {
            String dataErrorResponse = sendData(file, dataurl);
            if (dataErrorResponse != null) return dataErrorResponse;
        }
        return null;
    }

    private int createEmptyUpload(final URL url) {
        try {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setUseCaches(false);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("User-Agent", "GeRDI/0.1");
            connection.setRequestProperty("Content-Type", "application/json; utf-8");

            // Send empty JSON object
            try(OutputStream os = connection.getOutputStream()) {
                String data = "{}";
                byte[] input = data.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            try (BufferedReader br = connection.getResponseCode() >= 300 ? new BufferedReader(new InputStreamReader(connection.getErrorStream(), "utf-8")) : new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8")) ) {
                StringBuilder response = new StringBuilder();
                String responseLine = null;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                if (connection.getResponseCode() == 201) {
                    JsonNode errorResponse = new ObjectMapper().readTree(response.toString());
                    return errorResponse.get("id").intValue();
                } else {
                    LOGGER.error("Error while creating empty upload: " + response.toString());
                    return -1;
                }
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }

        } catch (IOException e) {

        }
        return -1;
    }

    private String sendData(final String file, final URL url) {
        try {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setUseCaches(false);

            connection.setRequestMethod("POST");
            connection.setRequestProperty("Connection", "Keep-Alive");
            connection.setRequestProperty("User-Agent", "GeRDI/0.1");
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary="+BOUNDARY);
            try (OutputStream os = connection.getOutputStream()) {
                os.write(LINE_FEED);
                os.write(LINE_FEED);
                FileInputStream fis = new FileInputStream(new File(file));
                final int index = file.lastIndexOf('/');
                final String filename;
                if (index == -1) {
                    filename = file;
                } else {
                    final String lastPart = file.substring(index + 1);
                    if (lastPart.equals("")) {
                        return "{\"message\":\"'"+ file + "' is not a file\"}";
                    }
                    filename = lastPart;
                }
                os.write(("--"+BOUNDARY).getBytes());
                os.write(LINE_FEED);
                os.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + filename + "\"").getBytes());
                os.write(LINE_FEED);
                os.write("Content-Type: text/plain".getBytes());
                os.write(LINE_FEED);
                os.write(LINE_FEED);
                os.flush();
                byte[] buffer = new byte[4096];
                int bytesRead = -1;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
                fis.close();
                os.write(LINE_FEED);
                os.write(LINE_FEED);
                os.write(("--" + BOUNDARY + "--").getBytes());
                os.flush();
            }
            try (BufferedReader br = connection.getResponseCode() >= 300 ? new BufferedReader(new InputStreamReader(connection.getErrorStream(), "utf-8")) : new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8")) ) {
                StringBuilder response = new StringBuilder();
                String responseLine = null;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                if (connection.getResponseCode() >= 300) {
                    JsonNode errorResponse = new ObjectMapper().readTree(response.toString());
                    return errorResponse.toString();
                }
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }

        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return null;
    }

    private String sendMetadata(final JsonNode metadata, final URL url) {
        try {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("PUT");
            connection.setRequestProperty("Content-Type", "application/json; utf-8");

            try(OutputStream os = connection.getOutputStream()) {
                String data = "{\"metadata\":" + metadata.toString() + "}";
                byte[] input = data.getBytes("utf-8");
                os.write(input, 0, input.length);
//
            }
            try (BufferedReader br = connection.getResponseCode() >= 300 ? new BufferedReader(new InputStreamReader(connection.getErrorStream(), "utf-8")) : new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8")) ) {
                StringBuilder response = new StringBuilder();
                String responseLine = null;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                if (connection.getResponseCode() >= 300) {
                    if (connection.getResponseCode() >= 500) {
                        return "{\"message\":\"Zenodo server error\"}";
                    } else {
                        JsonNode errorResponse = new ObjectMapper().readTree(response.toString());
                        return "{\"message\":\"Metadata validation error\",\"errors\":" + errorResponse.get("errors") + "}";
                    }
                }
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }

        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        return null;
    }
}
