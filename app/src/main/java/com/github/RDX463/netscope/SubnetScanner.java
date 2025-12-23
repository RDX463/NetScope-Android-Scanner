package com.github.RDX463.netscope;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SubnetScanner {

    public interface ScanCallback {
        void onDeviceFound(String ip);
        void onScanFinished();
    }

    /**
     * Scans the local subnet (e.g., 192.168.1.X) for active devices.
     * @param baseIpPrefix The first 3 parts of the IP (e.g., "192.168.1.")
     */
    public void startDiscovery(String baseIpPrefix, ScanCallback callback) {
        new Thread(() -> {
            // 1. Use a larger thread pool because Pings are fast but waiting for timeout is slow
            ExecutorService executor = Executors.newFixedThreadPool(50);

            // 2. Loop through 1 to 254
            for (int i = 1; i < 255; i++) {
                final String testIp = baseIpPrefix + i;

                executor.execute(() -> {
                    if (isReachable(testIp)) {
                        callback.onDeviceFound(testIp);
                    }
                });
            }

            // 3. Wait for all threads to finish
            executor.shutdown();
            try {
                // Wait up to 2 seconds for all pings to return
                executor.awaitTermination(2, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            callback.onScanFinished();
        }).start();
    }

    // The actual "Ping" logic
    private boolean isReachable(String ip) {
        try {
            InetAddress address = InetAddress.getByName(ip);
            // Try to reach within 150ms.
            // If device is on local WiFi, 150ms is plenty.
            return address.isReachable(150);
        } catch (Exception e) {
            return false;
        }
    }
}