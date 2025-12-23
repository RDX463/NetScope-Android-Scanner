# 🛡️ NetScope - Android Network Vulnerability Scanner

> **A native Android tool to discover devices and audit open ports on local networks without root access.**

![Version](https://img.shields.io/badge/version-1.0.0-blue) ![Platform](https://img.shields.io/badge/platform-Android-green) ![License](https://img.shields.io/badge/license-MIT-orange)

## 📱 The Problem
In generic "Smart Home" environments, users often have dozens of connected devices (IoT bulbs, cameras, old laptops) they have forgotten about. These devices often leave dangerous ports (like Telnet/23 or RDP/3389) open, creating backdoors for attackers. Most mobile network scanners are either paid, ad-heavy, or require Root access.

## 🚀 The Solution
**NetScope** is a lightweight, open-source auditor that:
1.  **Maps the Subnet:** Identifies every active IP on your Wi-Fi.
2.  **Audits Ports:** Multithreaded scanning of critical ports (SSH, HTTP, DNS).
3.  **Identifies Services:** Translates raw port numbers into human-readable service names.

## 🛠️ Technical Architecture

### 1. Subnet Discovery (Ping Sweep)
Since Android 10+ restricts access to the system ARP table, NetScope uses a **Java Thread Pool** to perform an ICMP Echo (Ping) sweep across the entire `/24` subnet (254 IPs) in seconds.

### 2. Port Analysis (Socket Multithreading)
Scanning ports sequentially is too slow. NetScope implements a `FixedThreadPool` executor to launch **20 concurrent socket connections**.
* **Algorithm:** `Connect` Scan (TCP 3-way handshake).
* **Timeout Optimization:** Aggressive 200ms timeout to discard closed ports instantly.

## 📸 Screenshots
| Network Map | Port Audit |
|:---:|:---:|
| <img src="docs/screen1.jpg" width="250"> | <img src="docs/screen2.jpg" width="250"> |

*(Place your screenshots in a folder named 'docs' and push them)*

## 📥 Installation
Since this app is not on the Play Store, you must side-load it:
1.  Go to the **[Releases](../../releases)** page.
2.  Download the `app-release.apk`.
3.  Open the file on your phone and allow "Install from Unknown Sources".

## ⚠️ Disclaimer
This tool is for **educational and defensive use only**. Do not use this tool on networks you do not own or do not have permission to scan.

---
*Built with ❤️ using Java & Android SDK*
