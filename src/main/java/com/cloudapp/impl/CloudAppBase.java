package com.cloudapp.impl;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.params.AuthPolicy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cloudapp.api.CloudAppException;

public class CloudAppBase {

  private static final Logger LOGGER = LoggerFactory.getLogger(CloudAppBase.class);
  protected static final String MY_CL_LY_SCHEME = "http";
  protected static final String MY_CL_LY_HOST = "my.cl.ly";

  protected DefaultHttpClient client;
  protected Host host;

  public CloudAppBase(DefaultHttpClient client, Host host) {
    this.client = client;
    this.host = host;
  }

  /**
   * Executes a GET to a url with a certain body.
   * 
   * @param url
   * @return
   * @throws CloudAppException
   */
  protected Object executeGet(String path) throws CloudAppException {
    HttpGet req = new HttpGet(host.createUri(path));
    return executeRequest(req, 200);
  }
  
  /**
   * Executes a DELETE to a url.
   * 
   * @param url
   * @return
   * @throws CloudAppException
   */
  protected Object executeDelete(String path) throws CloudAppException {
    HttpDelete req = new HttpDelete(path);
    return executeRequest(req, 200);
  }


  /**
   * Executes a POST to a url with a certain body.
   * 
   * @param url
   * @param body
   * @return
   * @throws CloudAppException
   */
  protected Object executePost(String path, String body, int expectedCode)
      throws CloudAppException {
    HttpPost req = new HttpPost(host.createUri(path));
    if (body != null) {
      req.setEntity(transformJSONtoEntity(body));
    }
    return executeRequest(req, expectedCode);
  }

  /**
   * Executes a PUT request to a URL for a given body.
   * 
   * @param url
   *          The url to do the PUT request too.
   * @param body
   *          The body of the request.
   * @return A JSONObject or JSONArray constructed from the body of the CloudApp API's
   *         response.
   * @throws CloudAppException
   */
  protected Object executePut(String path, String body, int expectedCode)
      throws CloudAppException {
    HttpPut req = new HttpPut(path);
    if (body != null) {
      req.setEntity(transformJSONtoEntity(body));
    }
    return executeRequest(req, expectedCode);
  }

  /**
   * Executes an HttpClient request and returns the response content as a JSON Object. If
   * the body parameter is not null, it sets it as a StringEntity on the request object.
   * 
   * @param req
   * @param body
   * @return
   * @throws CloudAppException
   */
  protected Object executeRequest(HttpRequestBase req, int expectedCode)
      throws CloudAppException {
    try {
      req.addHeader("Accept", "application/json");

      HttpResponse response = client.execute(req);
      int status = response.getStatusLine().getStatusCode();
      String responseBody = EntityUtils.toString(response.getEntity());
      if (status == expectedCode) {
        // Try object, than array, than fail..
        try {
          return new JSONObject(responseBody);
        } catch (JSONException e) {
          return new JSONArray(responseBody);
        }
      }
      throw new CloudAppException(status, responseBody, null);
    } catch (ClientProtocolException e) {
      LOGGER.error("Something went wrong trying to contact the CloudApp API.", e);
      throw new CloudAppException(500,
          "Something went wrong trying to contact the CloudApp API", e);
    } catch (IOException e) {
      LOGGER.error("Something went wrong trying to contact the CloudApp API.", e);
      throw new CloudAppException(500,
          "Something went wrong trying to contact the CloudApp API.", e);
    } catch (JSONException e) {
      LOGGER.error("Something went wrong trying to handle JSON.", e);
      throw new CloudAppException(500, "Something went wrong trying to handle JSON.", e);
    }
  }

  /**
   * Transforms a JSONObject to a suitable body entity.
   * 
   * @param o
   *          The json object to transform
   * @return
   * @throws CloudAppException
   */
  protected StringEntity transformJSONtoEntity(String o) throws CloudAppException {
    try {
      StringEntity s = new StringEntity(o, "UTF-8");
      s.setContentEncoding("UTF-8");
      s.setContentType("application/json");
      return s;
    } catch (UnsupportedEncodingException e) {
      LOGGER.error("Could not encode json to string.", e);
      throw new CloudAppException(500, "Could not encode json to string.", e);
    }
  }
    
  public static class Host {
      private String scheme;
      private String host;
      private String auth;
      private int port;

      public Host(String scheme, String host, int port, String auth) {
          this.scheme = scheme;
          this.host = host;
          this.port = port;
          this.auth = auth;
      }

      public String getAuth() {
          return auth;
      }

      public String getScheme() {
          return scheme;
      }

      public String getHost() {
          return host;
      }

      public int getPort() {
          return port;
      }
      
      public String createUri(String path) {
          return String.format("%s://%s:%d%s", scheme, host, port, path);
      }
  }

}