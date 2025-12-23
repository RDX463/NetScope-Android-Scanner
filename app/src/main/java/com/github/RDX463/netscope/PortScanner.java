package com.github.RDX463.netscope; // KEEP YOUR PACKAGE NAME

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class PortScanner {

    // Upgraded Interface: Now sends back a String (banner) instead of just the port
    public interface ScanCallback {
        void onPortOpen(int port, String banner);
        void onScanComplete();
    }

    // Common ports + likely banner locations
    public static final int[] COMMON_PORTS = {
            21, 22, 23, 25, 53, 80, 110, 135, 139, 143, 443, 445, 3306, 3389, 8080
    };

    public void startScan(final String ip, final ScanCallback callback) {
        new Thread(() -> {
            ExecutorService executor = Executors.newFixedThreadPool(20);
            List<Future<ScanResult>> futures = new ArrayList<>();

            for (int port : COMMON_PORTS) {
                futures.add(executor.submit(() -> {
                    // Try to connect and grab banner
                    String banner = grabBanner(ip, port, 500); // 500ms timeout
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

    // Logic to read the first line of text from the server
    private String grabBanner(String ip, int port, int timeout) {
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(ip, port), timeout);

            // Allow 400ms to read a banner. If no text, just say "Open"
            socket.setSoTimeout(400);

            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            // Try to read line. If server is silent (like generic HTTP), this might throw exception
            String banner = reader.readLine();
            socket.close();

            if (banner != null && !banner.isEmpty() && banner.length() < 50) {
                return banner.trim(); // Return the server's hello message
            }
            return ""; // Connection worked, but no banner text
        } catch (Exception ex) {
            // If connection worked but reading failed, it's still OPEN
            if (socket.isConnected()) {
                try { socket.close(); } catch (Exception e) {}
                return "";
            }
            return null; // Connection failed entirely
        }
    }

    // Simple helper class to hold the result
    private static class ScanResult {
        int port;
        String banner;
        ScanResult(int p, String b) { this.port = p; this.banner = b; }
    }
}