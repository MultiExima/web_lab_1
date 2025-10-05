package itmo.web.lab1;


import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fastcgi.FCGIInterface;

public class FastCGIServer {
    private static final List<String> results = new ArrayList<>();

    public void start() {
        String fcgiPort = System.getenv("FCGI_PORT");
        if (fcgiPort == null || fcgiPort.isEmpty()) {
            fcgiPort = System.getProperty("FCGI_PORT", "46740");
            System.setProperty("FCGI_PORT", fcgiPort);
        }
        
        FCGIInterface fcgi = new FCGIInterface();
        System.out.println("FastCGI server started on port " + fcgiPort + ".");

        while (fcgi.FCGIaccept() >= 0) {
            try {
                if (FCGIInterface.request == null || FCGIInterface.request.params == null) {
                    sendJsonResponse(500, "{\"error\":\"FCGIInterface not properly initialized\"}");
                    continue;
                }
                
                String queryString = FCGIInterface.request.params.getProperty("QUERY_STRING");
                Map<String, String> params = parseQueryString(queryString);

                BigDecimal x = new BigDecimal(params.get("x"));
                BigDecimal y = new BigDecimal(params.get("y"));
                BigDecimal r = new BigDecimal(params.get("r"));

                /*String error = validateParams(x, y, r);
                if (error != null) {
                    sendJsonResponse(400, "{\"error\":\"" + escapeJson(error) + "\"}");
                    continue;
                }
                */

                boolean hit = MathChecker.hitCheck(x, y, r);
                String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

                String json = "{" +
                        "\"x\":\"" + escapeJson(x.toPlainString()) + "\"," +
                        "\"y\":\"" + escapeJson(y.toPlainString()) + "\"," +
                        "\"r\":\"" + escapeJson(r.toPlainString()) + "\"," +
                        "\"hit\":" + hit + "," +
                        "\"time\":\"" + escapeJson(time) + "\"}";

                synchronized (results) {
                    results.add(json);
                }

                sendJsonResponse(200, json);
                

            } catch (Exception e) {
                String json = "{\"error\":\"" + escapeJson(e.getMessage() == null ? e.toString() : e.getMessage()) + "\"}";
                sendJsonResponse(500, json);
                e.printStackTrace();
            }
        }
    }

    private Map<String, String> parseQueryString(String qs) {
        Map<String, String> params = new HashMap<>();
        if (qs == null || qs.isEmpty()) {
            return params;
        }
        for (String pair : qs.split("&")) {
            int idx = pair.indexOf('=');
            if (idx <= 0) continue;
            String key = urlDecode(pair.substring(0, idx));
            String val = urlDecode(pair.substring(idx + 1));
            params.put(key, val);
        }
        return params;
    }

    private String urlDecode(String s) {
        try {
            return URLDecoder.decode(s, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            return s;
        }
    }

    /*
    private String validateParams(String xStr, String yStr, String rStr) {
        if (xStr == null || yStr == null || rStr == null) {
            return "Missing required params x,y,r";
        }
        try {
            double x = Double.parseDouble(xStr);
            double y = Double.parseDouble(yStr);
            int r = Integer.parseInt(rStr);
            if (!Validator.checkXcord(x)) return "X out of range [-5,5]";
            if (!Validator.checkYcord(y)) return "Y out of range [-3,3]";
            if (!Validator.checkRcord(r)) return "R out of range [1,5]";
        } catch (NumberFormatException ex) {
            return "Invalid numeric format";
        }
        return null;
    }
    */

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private void sendJsonResponse(int status, String content) {
        try {
            byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
            System.out.print("Status: " + status + "\r\n");
            System.out.print("Content-Type: application/json; charset=utf-8\r\n");
            System.out.print("Content-Length: " + bytes.length + "\r\n");
            System.out.print("\r\n");
            System.out.write(bytes);
            System.out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}




