package com.enigma.audiobook.backend.proxies;

import com.enigma.audiobook.backend.utils.SerDe;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;

import static org.apache.http.entity.ContentType.APPLICATION_JSON;

@Component
public class RestClient {
    static SerDe serDe = new SerDe();
    static final String URL_FORMAT = "http://%s/%s";
    private final CloseableHttpClient client;

    public RestClient() {
        client = HttpClientBuilder.create().build();
    }

    public <T> T doPost(String hostAndPort, String path, String jsonStr, Class<T> clazz) {
        String uriStr = String.format(URL_FORMAT, hostAndPort, path);

        URI uri = URI.create(uriStr);
        HttpPost request = new HttpPost(uri);
        request.setEntity(new StringEntity(jsonStr, APPLICATION_JSON));

        try (CloseableHttpResponse response = client.execute(request)) {

            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                throw new RuntimeException(String.format("unable to make rest request, host:%s, path:%s",
                        hostAndPort, path));
            }

            String jsonResponse = EntityUtils.toString(response.getEntity());

            return serDe.fromJson(jsonResponse, clazz);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
