package com.github.RDX463.netscope;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.format.Formatter;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private TextView resultText;
    private Button scanButton;
    private TextView titleText;

    private boolean isScanning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. Connect UI elements
        resultText = findViewById(R.id.resultText);
        scanButton = findViewById(R.id.scanButton);
        titleText = findViewById(R.id.titleText);

        // 2. Set Button Action
        scanButton.setOnClickListener(v -> {
            if (isScanning) {
                // STOP LOGIC
                isScanning = false;
                scanButton.setText("Scan Network");
                resultText.append("\n[!] Scan Stopped by User.");
                // Note: Threads will finish their current task and then stop
                // because we will check 'isScanning' inside the loops.
            } else {
                // START LOGIC
                isScanning = true;
                startNetworkDiscovery();
            }
        });
    }

    private void startNetworkDiscovery() {
        // Clear previous results
        resultText.setText("");
        scanButton.setEnabled(false);
        scanButton.setText("Scanning Network...");

        // Get the local subnet (e.g., "192.168.1.")
        String subnet = getSubnetAddress();
        if (subnet == null) {
            resultText.setText("Error: Could not get WiFi details. Are you connected to WiFi?");
            scanButton.setEnabled(true);
            return;
        }

        titleText.setText("Scanning: " + subnet + "X");

        // Start the Subnet Scanner
        SubnetScanner subnetScanner = new SubnetScanner();
        subnetScanner.startDiscovery(subnet, new SubnetScanner.ScanCallback() {
            @Override
            public void onDeviceFound(String ip) {
                // Update UI immediately when a device is found
                runOnUiThread(() -> {
                    resultText.append("Found Device: " + ip + "\n");
                    triggerPortScan(ip);
                });
            }

            @Override
            public void onScanFinished() {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Scan Complete", Toast.LENGTH_SHORT).show();
                    scanButton.setText("Scan Network");
                    scanButton.setEnabled(true);
                    resultText.append("\n--- Scan Finished ---");
                });
            }
        });
    }

    // Helper to get the device's IP prefix (e.g., "192.168.1.")
    private String getSubnetAddress() {
        try {
            WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            // Get my IP
            String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());

            // Check if IP is valid (not 0.0.0.0)
            if (ip.equals("0.0.0.0")) return null;

            // Remove the last number to get the prefix
            return ip.substring(0, ip.lastIndexOf(".") + 1);
        } catch (Exception e) {
            return null;
        }
    }

    // Optional: Only used if you want to deep scan a specific IP
    private void triggerPortScan(String targetIp) {
        PortScanner portScanner = new PortScanner();
        portScanner.startScan(targetIp, new PortScanner.ScanCallback() {
            @Override
            public void onPortOpen(int port) {
                String service = getServiceName(port);
                runOnUiThread(() ->
                        resultText.append("   ⚠️ [" + targetIp + "] OPEN: " + port + " - " + service + "\n")
                );
            }
            @Override
            public void onScanComplete() {
                // Optional: Log that this specific IP is finished
            }
        });
    }
    private String getServiceName(int port) {
        switch (port) {
            case 21: return "FTP (File Transfer)";
            case 22: return "SSH (Secure Shell)";
            case 23: return "Telnet (Unsecure!)";
            case 53: return "DNS";
            case 80: return "HTTP (Web)";
            case 443: return "HTTPS (Secure Web)";
            case 445: return "SMB (Windows Share)";
            case 3389: return "RDP (Remote Desktop)";
            case 8080: return "HTTP Proxy";
            default: return "Unknown Service";
        }
    }
}