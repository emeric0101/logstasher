package fr.emeric0101.logstasher.service;

import fr.emeric0101.logstasher.dto.RestRequest;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

@Service
public class RestService {
    public String sendRequest(RestRequest restRequest) {

        StringEntity entity = null;
        if (restRequest.getBody() != null) {
            try {
                entity = new StringEntity(restRequest.getBody());
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException("Body not acceptable");
            }
        }


        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpUriRequest request;
        if (restRequest.getMethod().equals("GET")) {
            HttpGet httpGet = new HttpGet(restRequest.getUrl());
            request = httpGet;
        } else if (restRequest.getMethod().equals("PUT")) {
            HttpPut httpPut = new HttpPut(restRequest.getUrl());

            if (entity != null) {
                httpPut.setEntity(entity);
            }

            request = httpPut;
        } else if (restRequest.getMethod().equals("POST")) {
            HttpPost httpPost = new HttpPost(restRequest.getUrl());

            if (entity != null) {
                httpPost.setEntity(entity);
            }

            request = httpPost;
        } else if (restRequest.getMethod().equals("DELETE")) {
            HttpDelete httpDelete = new HttpDelete(restRequest.getUrl());
            request = httpDelete;
        } else {
            throw new RuntimeException("Type not supported yet");
        }


        request.setHeader("Accept", "application/json");
        request.setHeader("Content-type", "application/json");

        CloseableHttpResponse response1 = null;
        try {
            response1 = httpclient.execute(request);
            return EntityUtils.toString(response1.getEntity());
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            httpclient.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "IO ERROR";
    }
}
