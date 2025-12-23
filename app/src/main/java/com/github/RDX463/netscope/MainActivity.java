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
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Environment;
import androidx.core.content.FileProvider;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import android.os.Build;

public class MainActivity extends AppCompatActivity {

    private TextView resultText;
    private Button scanButton;
    private TextView titleText;
    private boolean isScanning = false;
    private Button copyButton;
    private Button updateButton; // <--- NEW

    private static final String GITHUB_USER = "RDX463"; // CHANGE THIS
    private static final String CURRENT_VERSION = "v1.3.0";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. Connect UI elements
        resultText = findViewById(R.id.resultText);
        scanButton = findViewById(R.id.scanButton);
        titleText = findViewById(R.id.titleText);
        copyButton = findViewById(R.id.copyButton);
        updateButton = findViewById(R.id.updateButton);

        // 2. Set Copy Action
        copyButton.setOnClickListener(v -> copyLogsToClipboard());

        // 3. Set Update Action (Manual Click)
        updateButton.setOnClickListener(v -> {
            Toast.makeText(MainActivity.this, "Checking for updates...", Toast.LENGTH_SHORT).show();
            // Pass 'true' so we know to show a toast if no update is found
            checkForUpdates(true);
        });

        // 4. Set Scan Button Action
        scanButton.setOnClickListener(v -> {
            if (isScanning) {
                // STOP LOGIC
                isScanning = false;
                scanButton.setText("RE-SCAN");
                printToConsole("[!] Scan Aborted by User.", "#FF5555");
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
            public void onDeviceFound(String ip, String hostname) {
                // Update UI with IP and Name
                String info = "[+] Host Up: " + ip;

                // Add Hostname in a different color (Cyan) if available
                if (!hostname.equals("Unknown Device")) {
                    info += " (" + hostname + ")";
                    printToConsole(info, "#58A6FF"); // Cyan for named devices
                } else {
                    printToConsole(info, "#FFFFFF"); // White for generic IPs
                }

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

                    // --- THE FIX FOR "ABORTED" BUG ---
                    isScanning = false;
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

    // Deep scan a specific IP
    private void triggerPortScan(String targetIp) {
        PortScanner portScanner = new PortScanner();
        portScanner.startScan(targetIp, new PortScanner.ScanCallback() {
            @Override
            public void onPortOpen(int port, String banner) {
                // Get generic name (e.g., "SSH")
                String service = getServiceName(port);

                // If we grabbed a specific banner use it
                String displayInfo = (banner.isEmpty()) ? service : banner;

                printToConsole("    └── ⚠️ PORT " + port + ": " + displayInfo, "#00FF41");
            }
            @Override
            public void onScanComplete() {
                // Done
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

    // FEATURE: Copy Logs
    private void copyLogsToClipboard() {
        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        android.content.ClipData clip = android.content.ClipData.newPlainText("NetScope Scan", resultText.getText());
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "Logs copied to clipboard!", Toast.LENGTH_SHORT).show();
    }

    // FEATURE: Update Checker (FIXED METHOD SIGNATURE)
    private void checkForUpdates(boolean isManualCheck) {
        new Thread(() -> {
            try {
                // 1. Fetch JSON from GitHub
                java.net.URL url = new java.net.URL("https://api.github.com/repos/" + GITHUB_USER + "/NetScope-Android-Scanner/releases/latest");
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/vnd.github.v3+json");

                java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) response.append(line);

                // 2. Parse JSON
                JSONObject json = new JSONObject(response.toString());
                String serverVersion = json.getString("tag_name");

                // Check version match
                if (!serverVersion.equals(CURRENT_VERSION)) {
                    // Get the assets array ONCE
                    JSONArray assets = json.getJSONArray("assets");
                    String downloadUrl = "";

                    // LOOP to find the actual APK file
                    for (int i = 0; i < assets.length(); i++) {
                        JSONObject asset = assets.getJSONObject(i);
                        String fileName = asset.getString("name");
                        if (fileName.endsWith(".apk")) {
                            downloadUrl = asset.getString("browser_download_url");
                            break; // Found it!
                        }
                    }

                    // Only proceed if we found a valid APK url
                    if (!downloadUrl.isEmpty()) {
                        final String finalUrl = downloadUrl;
                        runOnUiThread(() -> showUpdateDialog(serverVersion, finalUrl));
                    }
                } else {
                    // MANUAL CHECK: If versions match, tell the user
                    if (isManualCheck) {
                        runOnUiThread(() -> Toast.makeText(MainActivity.this, "App is up to date (" + CURRENT_VERSION + ")", Toast.LENGTH_SHORT).show());
                    }
                }
            } catch (Exception e) {
                // ERROR HANDLING
                if (isManualCheck) {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Update check failed (Offline?)", Toast.LENGTH_SHORT).show());
                }
                e.printStackTrace();
            }
        }).start();
    }

    private void showUpdateDialog(String newVersion, String downloadUrl) {
        new AlertDialog.Builder(this)
                .setTitle("Update Available: " + newVersion)
                .setMessage("A new version of NetScope is available. Would you like to download and install it now?")
                .setPositiveButton("Update Now", (dialog, which) -> downloadAndInstall(downloadUrl))
                .setNegativeButton("Later", null)
                .show();
    }

    private void downloadAndInstall(String url) {
        Toast.makeText(this, "Downloading update...", Toast.LENGTH_SHORT).show();

        // 1. Prepare Download Request
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setTitle("NetScope Update");
        request.setDescription("Downloading " + url);
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "NetScope_Update.apk");
        request.setMimeType("application/vnd.android.package-archive");

        // 2. Start Download
        DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        long downloadId = manager.enqueue(request);

        // 3. Listen for completion
        BroadcastReceiver onComplete = new BroadcastReceiver() {
            public void onReceive(Context ctxt, Intent intent) {
                File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "NetScope_Update.apk");
                installApk(file);
                unregisterReceiver(this);
            }
        };

        // Check Android version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        }
    }

    private void installApk(File file) {
        try {
            Uri contentUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", file);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(contentUri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Error opening installer: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}