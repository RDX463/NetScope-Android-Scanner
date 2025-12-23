package com.github.RDX463.netscope; // Make sure this matches your package name!

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class PortScanner {

    // Interface to send results back to the Main Activity
    public interface ScanCallback {
        void onPortOpen(int port);
        void onScanComplete();
    }

    // Common ports of interest (SSH, Web, Database, etc.)
    public static final int[] COMMON_PORTS = {
            21, 22, 23, 80, 443, 3306, 8080, 53, 135, 139, 445, 3389
    };

    /**
     * Scans a specific IP for open ports using multiple threads.
     *
     * @param ip The IP address to scan (e.g., "192.168.1.5")
     * @param callback The interface to update the UI
     */
    public void startScan(final String ip, final ScanCallback callback) {
        new Thread(() -> {

            // Create 20 workers to scan ports in parallel
            ExecutorService executor = Executors.newFixedThreadPool(20);
            List<Future<Integer>> futures = new ArrayList<>();

            for (int port : COMMON_PORTS) {
                futures.add(executor.submit(new Callable<Integer>() {
                    @Override
                    public Integer call() {
                        if (isPortOpen(ip, port, 200)) { // 200ms timeout
                            return port;
                        }
                        return null; // Port is closed
                    }
                }));
            }

            // Collect results
            for (Future<Integer> future : futures) {
                try {
                    Integer openPort = future.get();
                    if (openPort != null) {
                        callback.onPortOpen(openPort);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            executor.shutdown();
            callback.onScanComplete();

        }).start();
    }

    // Helper to try connecting to a socket
    private boolean isPortOpen(String ip, int port, int timeout) {
        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(ip, port), timeout);
            socket.close();
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}