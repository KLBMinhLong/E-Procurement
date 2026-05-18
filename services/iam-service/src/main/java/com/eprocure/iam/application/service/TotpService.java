package com.eprocure.iam.application.service;

import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class TotpService {
    private static final char[] BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".toCharArray();
    private static final String BACKUP_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int SECRET_BYTES = 20;
    private static final int BACKUP_CODE_COUNT = 8;
    private static final int TOTP_DIGITS = 6;
    private static final int TIME_STEP_SECONDS = 30;
    private static final int ALLOWED_WINDOW_STEPS = 1;
    private static final HexFormat HEX = HexFormat.of();

    private final String issuer;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    public TotpService(String issuer) {
        this(issuer, Clock.systemUTC());
    }

    TotpService(String issuer, Clock clock) {
        this.issuer = issuer == null || issuer.isBlank() ? "eProcure" : issuer.trim();
        this.clock = clock;
    }

    public String generateSecret() {
        byte[] bytes = new byte[SECRET_BYTES];
        secureRandom.nextBytes(bytes);
        return encodeBase32(bytes);
    }

    public boolean verifyCode(String secret, String code) {
        if (secret == null || secret.isBlank() || code == null || !code.matches("^\\d{6}$")) {
            return false;
        }
        long currentStep = clock.instant().getEpochSecond() / TIME_STEP_SECONDS;
        for (int offset = -ALLOWED_WINDOW_STEPS; offset <= ALLOWED_WINDOW_STEPS; offset++) {
            String expected = generateCode(secret, currentStep + offset);
            if (MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), code.getBytes(StandardCharsets.UTF_8))) {
                return true;
            }
        }
        return false;
    }

    public String provisioningUri(String accountName, String secret) {
        String safeAccount = accountName == null || accountName.isBlank() ? "user" : accountName.trim();
        String label = urlEncode(issuer + ":" + safeAccount);
        return "otpauth://totp/"
                + label
                + "?secret="
                + urlEncode(secret)
                + "&issuer="
                + urlEncode(issuer)
                + "&digits="
                + TOTP_DIGITS
                + "&period="
                + TIME_STEP_SECONDS;
    }

    public BackupCodeSet generateBackupCodes() {
        Set<String> codes = new LinkedHashSet<>();
        while (codes.size() < BACKUP_CODE_COUNT) {
            codes.add(randomBackupCode());
        }
        List<String> rawCodes = new ArrayList<>(codes);
        List<String> hashes = rawCodes.stream().map(this::hashBackupCode).toList();
        return new BackupCodeSet(rawCodes, hashes);
    }

    String generateCode(String secret, Instant instant) {
        return generateCode(secret, instant.getEpochSecond() / TIME_STEP_SECONDS);
    }

    private String generateCode(String secret, long timeStep) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(decodeBase32(secret), "HmacSHA1"));
            byte[] hash = mac.doFinal(ByteBuffer.allocate(Long.BYTES).putLong(timeStep).array());
            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);
            return String.format(Locale.ROOT, "%06d", binary % 1_000_000);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Unable to generate TOTP code", exception);
        }
    }

    private String randomBackupCode() {
        StringBuilder code = new StringBuilder(9);
        for (int index = 0; index < 8; index++) {
            if (index == 4) {
                code.append("-");
            }
            code.append(BACKUP_ALPHABET.charAt(secureRandom.nextInt(BACKUP_ALPHABET.length())));
        }
        return code.toString();
    }

    private String hashBackupCode(String code) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String normalized = code.trim().toUpperCase(Locale.ROOT);
            return HEX.formatHex(digest.digest(normalized.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private String encodeBase32(byte[] bytes) {
        StringBuilder result = new StringBuilder((bytes.length * 8 + 4) / 5);
        int buffer = 0;
        int bitsLeft = 0;
        for (byte value : bytes) {
            buffer = (buffer << 8) | (value & 0xFF);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                result.append(BASE32_ALPHABET[(buffer >> (bitsLeft - 5)) & 0x1F]);
                bitsLeft -= 5;
            }
        }
        if (bitsLeft > 0) {
            result.append(BASE32_ALPHABET[(buffer << (5 - bitsLeft)) & 0x1F]);
        }
        return result.toString();
    }

    private byte[] decodeBase32(String value) {
        String normalized = value.replace("=", "").replace(" ", "").toUpperCase(Locale.ROOT);
        int buffer = 0;
        int bitsLeft = 0;
        List<Byte> bytes = new ArrayList<>();
        for (int index = 0; index < normalized.length(); index++) {
            int decoded = decodeBase32Char(normalized.charAt(index));
            buffer = (buffer << 5) | decoded;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                bytes.add((byte) ((buffer >> (bitsLeft - 8)) & 0xFF));
                bitsLeft -= 8;
            }
        }
        byte[] result = new byte[bytes.size()];
        for (int index = 0; index < bytes.size(); index++) {
            result[index] = bytes.get(index);
        }
        return result;
    }

    private int decodeBase32Char(char value) {
        if (value >= 'A' && value <= 'Z') {
            return value - 'A';
        }
        if (value >= '2' && value <= '7') {
            return value - '2' + 26;
        }
        throw new IllegalArgumentException("Invalid base32 character");
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
