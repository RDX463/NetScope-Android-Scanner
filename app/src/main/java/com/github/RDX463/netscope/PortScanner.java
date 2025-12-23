package com.github.RDX463.netscope;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class PortScanner {

    public interface ScanCallback {
        void onPortOpen(int port, String banner);
        void onScanComplete();
    }

    // Common ports list
    public static final int[] COMMON_PORTS = {
            21, 22, 23, 25, 53, 80, 110, 135, 139, 143, 443, 445, 3306, 3389, 8080
    };

    public void startScan(final String ip, final ScanCallback callback) {
        new Thread(() -> {
            ExecutorService executor = Executors.newFixedThreadPool(20);
            List<Future<ScanResult>> futures = new ArrayList<>();

            for (int port : COMMON_PORTS) {
                futures.add(executor.submit(() -> {
                    // Try to connect and grab banner/title
                    String banner = grabBanner(ip, port, 1000); // 1 second timeout
                    if (banner != null) {
                        return new ScanResult(port, banner);
                    }
                    return null; // Closed
                }));
            }

            for (Future<ScanResult> future : futures) {
                try {
                    ScanResult result = future.get();
                    if (result != null) {
                        callback.onPortOpen(result.port, result.banner);
                    }
                } catch (Exception e) { e.printStackTrace(); }
            }

            executor.shutdown();
            callback.onScanComplete();
        }).start();
    }

    // The "Brain" of the scanner
    private String grabBanner(String ip, int port, int timeout) {
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(ip, port), timeout);
            socket.setSoTimeout(1000); // Read timeout

            // 1. Prepare IO
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // 2. HTTP Specific Logic (Port 80/8080)
            if (port == 80 || port == 8080) {
                // Send a minimal HTTP GET request
                out.print("GET / HTTP/1.1\r\n");
                out.print("Host: " + ip + "\r\n");
                out.print("Connection: close\r\n\r\n");
                out.flush();

                // Scan the response for the <title> tag
                String line;
                while ((line = in.readLine()) != null) {
                    if (line.toLowerCase().contains("<title>")) {
                        int start = line.toLowerCase().indexOf("<title>") + 7;
                        int end = line.toLowerCase().indexOf("</title>");
                        if (end > start) {
                            return "Web: " + line.substring(start, end).trim();
                        }
                    }
                }
                return "HTTP Server (No Title)";
            }

            // 3. Generic Logic (SSH, FTP, etc.)
            // Just listen for the server's greeting
            String banner = in.readLine();
            socket.close();

            if (banner != null && !banner.isEmpty() && banner.length() < 70) {
                return banner.trim();
            }
            return ""; // Port Open, but silent

        } catch (Exception ex) {
            // Connection failed
            return null;
        }
    }

    private static class ScanResult {
        int port;
        String banner;
        ScanResult(int p, String b) { this.port = p; this.banner = b; }
    }
}