# 🛡️ NetScope - Android Deep Network Recon

> **A native Android tool to perform active reconnaissance, banner grabbing, and vulnerability auditing on local networks.**

![Version](https://img.shields.io/badge/version-1.3.0-green) ![Platform](https://img.shields.io/badge/platform-Android-blue) ![License](https://img.shields.io/badge/license-MIT-orange)

## 📱 The Problem
Standard network scanners only show you IP addresses (e.g., `192.168.1.5`) and generic open ports (e.g., `Port 80: Web`). They don't tell you *what* the device actually is. To truly secure a network, you need to know if that IP is a printer, a router, or an insecure IoT camera.

## 🚀 The Solution
**NetScope v1.3** is a specialized "Deep Recon" tool that goes beyond simple scanning. It performs **Active Reconnaissance** to identify device manufacturers and specific software versions.

### 🔥 Key Features (v1.3)
* **🕵️‍♂️ Deep Service Recon:**
    * **Banner Grabbing:** Connects to SSH/FTP ports to read the server version (e.g., `SSH-2.0-Dropbear_2012.55`).
    * **HTTP Title Extraction:** Fetches the HTML `<title>` tag from web servers to identify devices by name (e.g., "TP-Link Wireless Router" or "Hikvision Camera").
* **🏷️ Hostname Resolution:** automatically attempts to resolve DNS/mDNS names.
* **🔄 Smart Auto-Updates:** Built-in engine checks GitHub Releases for updates. Includes a manual `[ UPDATE APP ]` button.
* **📋 Export Data:** One-click `[ COPY LOGS ]` to save scan results to the clipboard.
* **🎨 Hacker Terminal Theme:** A clean, high-contrast dark interface with color-coded alerts.

## 🛠️ Technical Architecture

### 1. Subnet Discovery (Parallel Ping Sweep)
NetScope bypasses Android's ARP table restrictions by using a **Java FixedThreadPool** to multicast ICMP Echo requests across the entire `/24` subnet.
* **Speed:** Scans 254 hosts in < 2 seconds.

### 2. Deep Port Analysis (Socket Inspection)
Unlike passive scanners, NetScope opens real TCP sockets to interact with services.
* **Web Services (Port 80/8080):** Sends a minimal `GET / HTTP/1.1` request to parse the HTML title.
* **System Services (Port 22/21):** Reads the raw `InputStream` to capture the service banner before the connection closes.

### 3. Native Update Engine
The app contains a custom `DownloadManager` implementation that queries the **GitHub API**, parses the JSON release tags, and securely triggers the Android Package Installer (supporting Android 14+ security standards).

## 📥 Installation
1.  Go to the **[Releases](../../releases)** page.
2.  Download the latest `NetScope-v1.3.apk`.
3.  Open the file on your phone.
    * *Note:* You may need to allow **"Install from Unknown Sources"** since this is a specialized tool not on the Play Store.

## ⚠️ Disclaimer
**NetScope is for educational and defensive use only.**
Scanning networks you do not own or do not have explicit permission to audit is illegal in many jurisdictions. The developer assumes no responsibility for misuse.

---
*Built with 💻 & ☕ by [RDX463]*
