package com.iscm.iam.service;

import com.iscm.iam.model.Device;
import com.iscm.iam.model.User;
import com.iscm.iam.repository.DeviceRepository;
import com.iscm.iam.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceService {

    private final UserRepository userRepository;
    private final DeviceRepository deviceRepository;

    @Value("${app.device.binding.enabled:true}")
    private boolean deviceBindingEnabled;

    @Value("${app.device.binding.max-trusted:5}")
    private int maxTrustedDevices;

    @Transactional
    public Device registerOrUpdateDevice(UUID userId, String userAgent, String ipAddress) {
        if (!deviceBindingEnabled) {
            return null;
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String deviceFingerprint = generateDeviceFingerprint(userAgent, ipAddress);
        DeviceInfo deviceInfo = parseUserAgent(userAgent);

        Optional<Device> existingDevice = deviceRepository
                .findByUserIdAndDeviceFingerprint(userId, deviceFingerprint);

        Device device;
        if (existingDevice.isPresent()) {
            device = existingDevice.get();
            device.updateLastSeen(ipAddress);
            device.setTrustScore(calculateTrustScore(device, deviceInfo));
        } else {
            device = new Device();
            device.setUser(user);
            device.setDeviceFingerprint(deviceFingerprint);
            device.setDeviceName(deviceInfo.deviceName());
            device.setDeviceType(deviceInfo.deviceType());
            device.setOperatingSystem(deviceInfo.operatingSystem());
            device.setBrowser(deviceInfo.browser());
            device.setFirstSeenAt(LocalDateTime.now());
            device.setLastSeenIp(ipAddress);
            device.setLastSeenAt(LocalDateTime.now());

            // Auto-trust first device
            if (deviceRepository.countByUserId(userId) == 0) {
                device.setIsTrusted(true);
                device.setTrustScore(80);
            }
        }

        return deviceRepository.save(device);
    }

    @Transactional
    public boolean isDeviceTrusted(UUID userId, String deviceFingerprint) {
        if (!deviceBindingEnabled) {
            return true; // Allow all devices if binding is disabled
        }

        return deviceRepository
                .findByUserIdAndDeviceFingerprint(userId, deviceFingerprint)
                .map(Device::getIsTrusted)
                .orElse(false);
    }

    @Transactional
    public void trustDevice(UUID userId, String deviceFingerprint) {
        Device device = deviceRepository
                .findByUserIdAndDeviceFingerprint(userId, deviceFingerprint)
                .orElseThrow(() -> new IllegalArgumentException("Device not found"));

        // Check max trusted devices limit
        long trustedCount = deviceRepository.countByUserIdAndIsTrustedTrue(userId);
        if (trustedCount >= maxTrustedDevices && !device.getIsTrusted()) {
            throw new IllegalStateException("Maximum number of trusted devices reached");
        }

        device.setIsTrusted(true);
        device.setTrustScore(Math.max(device.getTrustScore(), 80));
        deviceRepository.save(device);

        log.info("Device trusted for user: {}, fingerprint: {}", userId, deviceFingerprint);
    }

    @Transactional
    public void revokeTrust(UUID userId, String deviceFingerprint) {
        Device device = deviceRepository
                .findByUserIdAndDeviceFingerprint(userId, deviceFingerprint)
                .orElseThrow(() -> new IllegalArgumentException("Device not found"));

        device.setIsTrusted(false);
        device.setTrustScore(Math.min(device.getTrustScore(), 30));
        deviceRepository.save(device);

        log.info("Device trust revoked for user: {}, fingerprint: {}", userId, deviceFingerprint);
    }

    @Transactional
    public void blockDevice(UUID userId, String deviceFingerprint) {
        Device device = deviceRepository
                .findByUserIdAndDeviceFingerprint(userId, deviceFingerprint)
                .orElseThrow(() -> new IllegalArgumentException("Device not found"));

        device.setIsBlocked(true);
        device.setIsTrusted(false);
        device.setTrustScore(0);
        deviceRepository.save(device);

        log.warn("Device blocked for user: {}, fingerprint: {}", userId, deviceFingerprint);
    }

    public List<Device> getUserDevices(UUID userId) {
        return deviceRepository.findByUserIdOrderByLastSeenAtDesc(userId);
    }

    public boolean isNewDevice(UUID userId, String deviceFingerprint) {
        return !deviceRepository
                .findByUserIdAndDeviceFingerprint(userId, deviceFingerprint)
                .isPresent();
    }

    public boolean isSuspiciousDevice(UUID userId, String deviceFingerprint, String ipAddress) {
        Device device = deviceRepository
                .findByUserIdAndDeviceFingerprint(userId, deviceFingerprint)
                .orElse(null);

        if (device == null) {
            // New device - check if IP is suspicious
            return isSuspiciousIpAddress(userId, ipAddress);
        }

        return device.getIsBlocked() || device.getTrustScore() < 20;
    }

    private boolean isSuspiciousIpAddress(UUID userId, String ipAddress) {
        // Check if this IP has been used with this user before
        return !deviceRepository.findByUserIdAndLastSeenIp(userId, ipAddress).isEmpty();
    }

    private String generateDeviceFingerprint(String userAgent, String ipAddress) {
        try {
            String rawData = userAgent + ipAddress + "ISCM_SALT";
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawData.getBytes());

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("Error generating device fingerprint", e);
            return userAgent.hashCode() + "_" + ipAddress.hashCode();
        }
    }

    private DeviceInfo parseUserAgent(String userAgent) {
        if (userAgent == null) {
            return new DeviceInfo("Unknown Device", "unknown", "Unknown OS", "Unknown Browser");
        }

        String lowerUa = userAgent.toLowerCase();
        String deviceType = "desktop";
        String os = "Unknown OS";
        String browser = "Unknown Browser";
        String deviceName = "Unknown Device";

        // Determine device type
        if (lowerUa.contains("mobile") || lowerUa.contains("android") || lowerUa.contains("iphone")) {
            deviceType = "mobile";
        } else if (lowerUa.contains("tablet") || lowerUa.contains("ipad")) {
            deviceType = "tablet";
        }

        // Parse OS
        if (lowerUa.contains("windows")) {
            os = "Windows";
        } else if (lowerUa.contains("mac") || lowerUa.contains("os x")) {
            os = "macOS";
        } else if (lowerUa.contains("linux")) {
            os = "Linux";
        } else if (lowerUa.contains("android")) {
            os = "Android";
        } else if (lowerUa.contains("ios") || lowerUa.contains("iphone") || lowerUa.contains("ipad")) {
            os = "iOS";
        }

        // Parse browser
        if (lowerUa.contains("chrome")) {
            browser = "Chrome";
        } else if (lowerUa.contains("firefox")) {
            browser = "Firefox";
        } else if (lowerUa.contains("safari")) {
            browser = "Safari";
        } else if (lowerUa.contains("edge")) {
            browser = "Edge";
        }

        deviceName = os + " " + browser + " (" + deviceType + ")";

        return new DeviceInfo(deviceName, deviceType, os, browser);
    }

    private int calculateTrustScore(Device device, DeviceInfo deviceInfo) {
        int score = device.getTrustScore();

        // Increase score for consistent usage
        if (device.getFirstSeenAt().isBefore(LocalDateTime.now().minusDays(30))) {
            score += 10;
        }

        // Adjust based on device type
        if ("mobile".equals(deviceInfo.deviceType())) {
            score += 5; // Mobile devices often more trusted
        }

        return Math.min(100, Math.max(0, score));
    }

    private record DeviceInfo(String deviceName, String deviceType, String operatingSystem, String browser) {}
}