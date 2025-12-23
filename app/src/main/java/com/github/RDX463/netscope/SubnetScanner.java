package com.github.RDX463.netscope; // Keep your package name

import java.net.InetAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SubnetScanner {

    // Upgraded Callback: Returns IP AND Hostname
    public interface ScanCallback {
        void onDeviceFound(String ip, String hostname);
        void onScanFinished();
    }

    public void startDiscovery(String subnet, ScanCallback callback) {
        ExecutorService executor = Executors.newFixedThreadPool(50); // Fast parallel scan

        // Loop 1 to 254
        for (int i = 1; i < 255; i++) {
            final String host = subnet + i;
            executor.execute(() -> {
                try {
                    InetAddress address = InetAddress.getByName(host);
                    // 1. Check if reachable (Ping)
                    if (address.isReachable(300)) { // 300ms timeout

                        // 2. If reachable, try to get the Name (Reverse DNS)
                        // This might take a moment, so we do it here in the thread
                        String hostname = address.getCanonicalHostName();

                        // If logic fails to find a name, it usually returns the IP again.
                        // We check for that to avoid redundancy.
                        if (hostname.equals(host)) {
                            hostname = "Unknown Device";
                        }

                        callback.onDeviceFound(host, hostname);
                    }
                } catch (Exception e) {
                    // Timeout or unreachable
                }
            });
        }

        // Wait for all threads to finish
        new Thread(() -> {
            try {
                executor.shutdown();
                // Wait up to 10 seconds for full scan
                executor.awaitTermination(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            callback.onScanFinished();
        }).start();
    }
}