package com.github.RDX463.netscope;

import android.content.Context;
import android.text.Html;
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
        // 1. Clear screen
        resultText.setText("");
        scanButton.setEnabled(false);

        // 2. Print initial status in YELLOW
        printToConsole("root@android:~$ initializing_network_scan...", "#FFFF00");

        String subnet = getSubnetAddress();
        if (subnet == null) {
            // 3. Print error in RED
            printToConsole("ERROR: No WiFi Connection detected.", "#FF5555");
            scanButton.setEnabled(true);
            return;
        }

        // 4. Print target info in CYAN
        printToConsole("Target Subnet: " + subnet + "0/24", "#58A6FF");
        titleText.setText("Scanning: " + subnet + "X");

        SubnetScanner subnetScanner = new SubnetScanner();
        subnetScanner.startDiscovery(subnet, new SubnetScanner.ScanCallback() {
            @Override
            public void onDeviceFound(String ip) {
                // 5. Print found device in WHITE
                printToConsole("[+] Host Up: " + ip, "#FFFFFF");
                triggerPortScan(ip);
            }

            @Override
            public void onScanFinished() {
                // 6. Print finish message in GREY
                printToConsole("--- Scan Complete ---", "#8B949E");

                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Scan Complete", Toast.LENGTH_SHORT).show();
                    scanButton.setText("RE-SCAN");
                    scanButton.setEnabled(true);
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
    private void printToConsole(String text, String colorHex) {
        runOnUiThread(() -> {
            // Create HTML formatted string: <font color='#00FF41'>TEXT</font><br>
            String html = "<font color='" + colorHex + "'>" + text + "</font><br>";

            // Append to the TextView (supports HTML rendering)
            resultText.append(Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY));

            // Auto-scroll to bottom
            final android.widget.ScrollView scrollView = (android.widget.ScrollView) resultText.getParent();
            scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
        });
    }
}