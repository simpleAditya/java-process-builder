package com.example.process.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/*
 * Commands for quick testing.
 *
 * http://localhost:8080/browser/open?browser=Firefox&url=https://www.facebook.com/
 * http://localhost:8080/browser/open?browser=Google%20Chrome&url=https://www.facebook.com/
 *
 * http://localhost:8080/browser/close?browser=Firefox
 * http://localhost:8080/browser/close?browser=Google%20Chrome
 *
 * http://localhost:8080/browser/current-tab?browser=Google%20Chrome
 * http://localhost:8080/browser/current-tab?browser=Firefox
 *
 * http://localhost:8080/browser/clear-history?browser=Google%20Chromehttp://localhost:8080/browser/clear-history?browser=Google%20Chrome
 * http://localhost:8080/browser/clear-history?browser=Firefox
 *
 * */

@RestController
@RequestMapping("/browser")
public class ProcessController {
    private static final String OS = System.getProperty("os.name").toLowerCase();

    @GetMapping("/open")
    public ResponseEntity<String> openBrowser(@RequestParam String browser, @RequestParam String url)
            throws IOException {
        String[] command;

        if (OS.contains("windows")) {
            command = new String[]{"cmd", "/c", "start", browser, url};
        } else if (OS.contains("nix") || OS.contains("nux") || OS.contains("mac")) {
            command = new String[]{"open", "-a", browser, url};
        } else {
            throw new IllegalArgumentException("Unsupported OS: " + OS);
        }
        var processBuilder = new ProcessBuilder(command);
        System.out.println("Opening url " + url + " in browser " + browser + " in OS: " + OS);
        processBuilder.start();

        return ResponseEntity.ok("success");
    }

    @GetMapping("/close")
    public ResponseEntity<String> closeBrowser(@RequestParam String browser) throws IOException {
        String[] command1;

        if (OS.contains("windows")) {
            command1 = new String[]{"cmd", "/c", "tasklist | findstr /i " + browser};
        } else if (OS.contains("nix") || OS.contains("nux") || OS.contains("mac")) {
            command1 = new String[]{"pgrep", "-f", browser};
        } else {
            throw new IllegalArgumentException("Unsupported OS: " + OS);
        }
        var processBuilder = new ProcessBuilder(command1);
        System.out.println("Closing browser " + browser + " in OS: " + OS);
        var process = processBuilder.start();

        var inputStream = process.getInputStream();
        var bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

        var processId = "";
        while (bufferedReader.readLine() != null) {
            String[] columns = bufferedReader.readLine().trim().split("\\s+");
            if (columns.length > 1 && columns[0].split("\\.")[0].equalsIgnoreCase(browser)) {
                processId = columns[0];
                break;
            }
        }
        System.out.println("Process id " + processId);
        bufferedReader.close();

        String[] command2;

        if (OS.contains("windows")) {
            command2 = new String[]{"cmd", "/c", "taskkill /PID " + processId + " /F"};
        } else if (OS.contains("nix") || OS.contains("nux") || OS.contains("mac")) {
            command2 = new String[]{"kill", processId};
        } else {
            throw new IllegalArgumentException("Unsupported OS: " + OS);
        }
        processBuilder = new ProcessBuilder(command2);
        processBuilder.start();

        return ResponseEntity.ok("success");
    }

    @GetMapping("/current-tab")
    public ResponseEntity<String> currentTab(@RequestParam String browser) throws IOException {
        String[] command;

        if (OS.contains("windows")) {
            command = new String[]{};
        } else if (OS.contains("nix") || OS.contains("nux") || OS.contains("mac")) {
            command = new String[]{"osascript", "-e", "tell application \"" + browser +
                    "\" to get url of active tab of the front window"};
        } else {
            throw new IllegalArgumentException("Unsupported OS: " + OS);
        }
        var processBuilder = new ProcessBuilder(command);
        System.out.println("Fetching active tab url from browser " + browser + " in OS: " + OS);
        Process process = processBuilder.start();

        InputStream inputStream = process.getInputStream();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

        String activeTabUrl = bufferedReader.readLine();
        bufferedReader.close();

        System.out.println("Active browser tab url " + activeTabUrl);
        return ResponseEntity.ok(activeTabUrl);
    }

    @GetMapping("/clear-history")
    public ResponseEntity<String> clearHistory(@RequestParam String browser) throws IOException {

        return switch (browser.toLowerCase()) {
            case "chrome" -> {
                if (OS.contains("windows")) {
                    String[] command = new String[]{"cmd", "/c", "del /s /q " +
                            "C:\\...\\AppData\\Local\\Google\\Chrome\\User Data\\Default\\*"};
                    processRequest(command, browser);
                } else if (OS.contains("nix") || OS.contains("nux") || OS.contains("mac")) {
                    String[] command = new String[]{"/bin/sh", "-c", "rm -rf ~/.config/google-chrome/Default/*"};
                    processRequest(command, browser);
                } else {
                    throw new IllegalArgumentException("Unsupported OS: " + OS);
                }
                yield ResponseEntity.ok("success");
            }
            case "firefox" -> {
                if (OS.contains("windows")) {
                    String[] command1 = new String[]{"cmd", "/c", "del /s /q " +
                            "C:\\...\\AppData\\Local\\Mozilla\\Firefox\\Profiles\\iacaelrl.default-release\\places.sqlite*"};
                    processRequest(command1, browser);
                    String[] command2 = new String[]{"cmd", "/c", "del /s /q " +
                            "C:\\...\\AppData\\Roaming\\Mozilla\\Firefox\\Profiles\\iacaelrl.default-release\\places.sqlite*"};
                    processRequest(command2, browser);
                } else if (OS.contains("nix") || OS.contains("nux") || OS.contains("mac")) {
                    String[] command = new String[]{"/bin/sh", "-c", "rm -rf ~/.cache/mozilla/firefox/*"};
                    processRequest(command, browser);
                } else {
                    throw new IllegalArgumentException("Unsupported OS: " + OS);
                }
                yield ResponseEntity.ok("success");
            }
            default -> throw new IllegalArgumentException("Unsupported browser for Windows: " + browser);
        };
    }

    private static void processRequest(String[] command, String browser) throws IOException {
        var processBuilder = new ProcessBuilder(command);
        System.out.println("Cleaning cache for browser " + browser + " in OS: " + OS);
        processBuilder.start();
    }
}
