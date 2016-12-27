package com.sogou.vulture.common.util;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;

import javax.ws.rs.client.*;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Tao Li on 23/12/2016.
 */
public class CommonUtils {
  private static String DEFAULT_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

  public static String now() {
    return LocalDateTime.now().format(DateTimeFormatter.ofPattern(DEFAULT_TIME_FORMAT));
  }

  private static Response sendHttpRequest(String method, String uri,
                                          Map<String, String> headers, Map<String, String> data,
                                          MediaType responseType) {
    Client client = ClientBuilder.newClient(new ClientConfig()).
        property(ClientProperties.CONNECT_TIMEOUT, 1000).
        property(ClientProperties.READ_TIMEOUT, 1000);
    WebTarget target = client.target(uri);
    Invocation.Builder builder = target.request(responseType);
    if (headers != null) {
      for (Map.Entry<String, String> entry : headers.entrySet()) {
        builder.header(entry.getKey(), entry.getValue());
      }
    }

    switch (method) {
      case "GET":
        return builder.get();
      case "POST":
        Form form = new Form();
        for (Map.Entry<String, String> entry : data.entrySet()) {
          form.param(entry.getKey(), entry.getValue());
        }
        return builder.post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE));
      default:
        throw new RuntimeException("Not support method: " + method);
    }
  }

  public static Response sendGetRequest(String uri, Map<String, String> headers,
                                        MediaType responseType) {
    return sendHttpRequest("GET", uri, headers, null, responseType);
  }

  public static Response sendPostRequest(String uri, Map<String, String> headers,
                                         Map<String, String> data, MediaType responseType) {
    return sendHttpRequest("POST", uri, headers, data, responseType);
  }

  public static String fillConfVariablePattern(String confVariablePattern, String time) {
    String timeFormat;
    switch (time.length()) {
      case 8:
        timeFormat = "yyyyMMdd";
        break;
      case 10:
        timeFormat = "yyyyMMddHH";
        break;
      case 12:
        timeFormat = "yyyyMMddHHmm";
        break;
      default:
        throw new IllegalArgumentException(
            "time format length must be one of yyyyMMdd, yyyyMMddHH or yyyyMMddHHmm");
    }

    LocalDateTime dateTime = LocalDateTime.parse(time, DateTimeFormatter.ofPattern(timeFormat));
    String filled = confVariablePattern;
    while (true) {
      String timePatternVar = extractFirstConfVariable(filled);
      if (timePatternVar != null) {
        filled = replaceFirstConfVariable(filled, timePatternVar, dateTime);
      } else {
        break;
      }
    }
    return filled;
  }

  private static String extractFirstConfVariable(String rowConfVariable) {
    String confVariableRegex = "\\$\\{(\\w+)\\}";
    Pattern p = Pattern.compile(confVariableRegex);
    Matcher m = p.matcher(rowConfVariable);
    while (m.find()) {
      return m.group(1);
    }
    return null;
  }

  private static String replaceFirstConfVariable(String rowConfVariable, String timePatternVar,
                                                 LocalDateTime dateTime) {
    return rowConfVariable.replaceFirst(
        String.format("\\$\\{%s\\}", timePatternVar),
        dateTime.format(DateTimeFormatter.ofPattern(timePatternVar)));
  }
}
