package org.gerdi.submit.zenodo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.gerdi.submit.component.AbstractSubmitComponent;
import org.gerdi.submit.security.GeRDIUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

@Component
public class SubmitComponentImpl extends AbstractSubmitComponent {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubmitComponentImpl.class);

    @Override
    public String submitData(final String s, final JsonNode metadata, final List<String> list, final GeRDIUser geRDIUser) {
        try {
            System.out.println("Text: " + metadata.toString());
            URL url = new URL("https://zenodo.org/api/deposit/depositions/3368361?access_token=xd1GzkuWtIyXJeL3r3yPxsZ8oe3JEd4Zod0tceTZWEVBnAw22YLIndcp9ssD");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("PUT");
            connection.setRequestProperty("Content-Type", "application/json; utf-8");

            try(OutputStream os = connection.getOutputStream()) {
                String data = "{\"metadata\":" + metadata.toString() + "}";
                byte[] input = data.getBytes("utf-8");
                os.write(input, 0, input.length);
                System.out.println("Response " + connection.getResponseCode() + " " + connection.getResponseMessage());
//
            }
            try (BufferedReader br = connection.getResponseCode() >= 300 ? new BufferedReader(new InputStreamReader(connection.getErrorStream(), "utf-8")) : new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8")) ) {
                StringBuilder response = new StringBuilder();
                String responseLine = null;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                if (connection.getResponseCode() >= 300) {
                    JsonNode errorResponse = new ObjectMapper().readTree(response.toString());
                    return "{\"message\":\"Validation error\",\"errors\":" + errorResponse.get("errors") + "}";
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
