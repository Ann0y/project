package sch.whvcse.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 身份证号加密 / 脱敏工具（需求文档 8「安全」：
 * 身份证号 AES 加密存储 + 展示脱敏，保留前 6 后 4）。
 *
 * 实现要点：
 *   - 使用 AES/GCM，每次加密随机 IV，IV 与密文一起 Base64 存放；
 *   - 密钥来自配置项 app.crypto.aes-key，做一次 SHA-256 派生为 16 字节；
 *   - 密文形如 "v1:" + Base64(iv + cipherText)，带版本前缀便于后续算法升级。
 */
@Component
public class IdCardCrypto {

    /** 脱敏后的占位，用于"解密失败 / 非密文"时兜底展示 */
    private static final String MASK = "********";

    private static final String PREFIX = "v1:";
    private static final int GCM_IV_BYTES = 12;
    private static final int GCM_TAG_BITS = 128;

    private final SecretKeySpec key;
    private final SecureRandom random = new SecureRandom();

    public IdCardCrypto(@Value("${app.crypto.aes-key:driving-school-default-key}") String rawKey) {
        try {
            // 把任意长度的配置密钥派生成固定 16 字节，作为 AES-128 密钥
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawKey.getBytes(StandardCharsets.UTF_8));
            this.key = new SecretKeySpec(Arrays.copyOf(digest, 16), "AES");
        } catch (Exception e) {
            throw new IllegalStateException("初始化身份证加密密钥失败", e);
        }
    }

    /** 加密明文身份证号；已是密文则原样返回 */
    public String encrypt(String plain) {
        if (plain == null || plain.isBlank()) return plain;
        if (isEncrypted(plain)) return plain;
        try {
            byte[] iv = new byte[GCM_IV_BYTES];
            random.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] ct = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));

            byte[] out = new byte[iv.length + ct.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(ct, 0, out, iv.length, ct.length);
            return PREFIX + Base64.getEncoder().encodeToString(out);
        } catch (Exception e) {
            throw new IllegalStateException("身份证加密失败", e);
        }
    }

    /** 解密；失败返回 null（调用方用脱敏兜底） */
    public String decrypt(String stored) {
        if (stored == null || stored.isBlank()) return null;
        if (!isEncrypted(stored)) return stored;
        try {
            byte[] all = Base64.getDecoder().decode(stored.substring(PREFIX.length()));
            byte[] iv = Arrays.copyOfRange(all, 0, GCM_IV_BYTES);
            byte[] ct = Arrays.copyOfRange(all, GCM_IV_BYTES, all.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(ct), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    public boolean isEncrypted(String value) {
        return value != null && value.startsWith(PREFIX);
    }

    /**
     * 展示脱敏：保留前 6 后 4（需求文档 6.1）。
     * 例：310101********1234
     */
    public String mask(String stored) {
        if (stored == null || stored.isBlank()) return "";
        String plain = decrypt(stored);
        if (plain == null) {
            // 解密失败仍按前 6 后 4 处理原始值，避免泄露完整号码
            return maskRaw(stored);
        }
        return maskRaw(plain);
    }

    private String maskRaw(String raw) {
        String s = raw.trim();
        if (s.length() <= 10) return MASK;
        return s.substring(0, 6) + "********" + s.substring(s.length() - 4);
    }
}
