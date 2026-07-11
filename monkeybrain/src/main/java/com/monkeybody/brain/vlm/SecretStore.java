package com.monkeybody.brain.vlm;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;

/** 使用 Android Keystore AES-GCM 加密 API Key，源码和日志中不出现密钥。 */
final class SecretStore {
    private static final String ALIAS = "monkeybody_siliconflow_api_key";
    private static final String PREFS = "encrypted_secrets";
    private final Context context;
    SecretStore(Context context) { this.context = context.getApplicationContext(); }

    void save(String secret) throws Exception {
        SecretKey key = key();
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] encrypted = cipher.doFinal(secret.getBytes(StandardCharsets.UTF_8));
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString("value", Base64.encodeToString(encrypted, Base64.NO_WRAP))
                .putString("iv", Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP)).apply();
    }
    String read() throws Exception {
        String value = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("value", null);
        String iv = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("iv", null);
        if (value == null || iv == null) return null;
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP)));
        return new String(cipher.doFinal(Base64.decode(value, Base64.NO_WRAP)), StandardCharsets.UTF_8);
    }
    boolean configured() { return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).contains("value"); }
    void clear() { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply(); }

    private SecretKey key() throws Exception {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore"); store.load(null);
        KeyStore.Entry entry = store.getEntry(ALIAS, null);
        if (entry instanceof KeyStore.SecretKeyEntry) return ((KeyStore.SecretKeyEntry) entry).getSecretKey();
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());
        return generator.generateKey();
    }
}
