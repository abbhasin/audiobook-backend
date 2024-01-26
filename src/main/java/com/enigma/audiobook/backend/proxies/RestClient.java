package com.enigma.audiobook.backend.proxies;

import com.enigma.audiobook.backend.utils.SerDe;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.Header;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

import static org.apache.http.entity.ContentType.APPLICATION_JSON;

@Slf4j
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

    public HeaderAndEntity doPut(String url, byte[] data) {
        URI uri = URI.create(url);
        HttpPut request = new HttpPut(uri);
        request.setEntity(new ByteArrayEntity(data));

        try (CloseableHttpResponse response = client.execute(request)) {
            log.info("http response:"+response);
            log.info("http response status:" + response.getStatusLine());
            if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                throw new RuntimeException(String.format("unable to make rest request, url:%s",
                        url));
            }
            List<Header> headers = Arrays.asList(response.getAllHeaders());
            String jsonResponse = response.getEntity() == null ? "" : EntityUtils.toString(response.getEntity());

            return new HeaderAndEntity(headers, jsonResponse);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Data
    public static class HeaderAndEntity {
        final List<Header> headers;
        final String jsonResponse;
    }
}
