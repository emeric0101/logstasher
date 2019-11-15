package fr.emeric0101.logstasher.service;

import fr.emeric0101.logstasher.dto.RestRequest;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

@Service
public class RestService {
    public void sendRequest(RestRequest restRequest) {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpUriRequest request;
        if (restRequest.getType().equals("GET")) {
            HttpGet httpGet = new HttpGet(restRequest.getUrl());
            request = httpGet;
        } else if (restRequest.getType().equals("PUT")) {
            HttpPut httpPut = new HttpPut(restRequest.getUrl());
            request = httpPut;
        } else if (restRequest.getType().equals("POST")) {
            HttpPost httpPost = new HttpPost(restRequest.getUrl());
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-type", "application/json");
            StringEntity entity = null;
            try {
                entity = new StringEntity(restRequest.getBody());
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException("Body not acceptable");
            }

            httpPost.setEntity(entity);
            request = httpPost;
        } else if (restRequest.getType().equals("DELETE")) {
            HttpDelete httpDelete = new HttpDelete(restRequest.getUrl());
            request = httpDelete;
        } else {
            throw new RuntimeException("Type not supported yet");
        }

        CloseableHttpResponse response1 = null;
        try {
            response1 = httpclient.execute(request);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            httpclient.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
