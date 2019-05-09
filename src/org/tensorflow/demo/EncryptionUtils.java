package org.tensorflow.demo;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;

import com.google.crypto.tink.HybridDecrypt;
import com.google.crypto.tink.HybridEncrypt;
import com.google.crypto.tink.JsonKeysetReader;
import com.google.crypto.tink.JsonKeysetWriter;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.hybrid.HybridDecryptFactory;
import com.google.crypto.tink.hybrid.HybridEncryptFactory;
import com.google.crypto.tink.hybrid.HybridKeyTemplates;
import com.google.crypto.tink.integration.android.AndroidKeystoreKmsClient;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;

public class EncryptionUtils {

  private static String MASTER_KEY_URI = "android-keystore://unsurv_masterKey";
  private static String MAIN_KEY_FILENAME = "mainKeyset.json";
  private static String ONE_TIME_KEY_FILENAME = "android-keystore://unsurv_masterKey";


  static void generateMasterKey() throws GeneralSecurityException, IOException {


      // uses internal Android keystore system
      AndroidKeystoreKmsClient.getOrGenerateNewAeadKey(MASTER_KEY_URI); // AES256-GCM

  }

  static void generateHybridKeys(boolean oneTimeKey , Context context) throws GeneralSecurityException, IOException {

    String filename;

    KeysetHandle privateKeysetHandle = KeysetHandle.generateNew(HybridKeyTemplates.ECIES_P256_HKDF_HMAC_SHA256_AES128_GCM);

    if (oneTimeKey) {
      filename = ONE_TIME_KEY_FILENAME;

    } else {
      filename = MAIN_KEY_FILENAME;
    }

    privateKeysetHandle.write(JsonKeysetWriter.withFile(new File(context.getFilesDir(), filename)),
            new AndroidKeystoreKmsClient().getAead(MASTER_KEY_URI));

  }

  static byte[] convertBitmapToBytes(Bitmap bmp){
    // convert Bitmap to byte[]
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    bmp.compress(Bitmap.CompressFormat.JPEG, 90,byteArrayOutputStream);

    return byteArrayOutputStream.toByteArray();

  }

  static byte[] encryptBytes(byte[] bytes, File keyFile) throws GeneralSecurityException, IOException {

    byte[] encryptedBytes;

    // Read encrypted keys for hybrid encryption from storage.
    KeysetHandle hybridKeys = KeysetHandle.read(JsonKeysetReader.withFile(
            keyFile), new AndroidKeystoreKmsClient().getAead(MASTER_KEY_URI));

    KeysetHandle publicKeysetHandle = hybridKeys.getPublicKeysetHandle();

    // 2. Get the primitive.
    HybridEncrypt hybridEncrypt = HybridEncryptFactory.getPrimitive(
            publicKeysetHandle);


    // 3. Use the primitive.
    encryptedBytes = hybridEncrypt.encrypt(bytes, null);

    return encryptedBytes;

  }


  static byte[] decryptBytes(byte[] bytes, File keyFile) throws GeneralSecurityException, IOException{


    // Read encrypted keys for hybrid encryption from storage.
    KeysetHandle privateKeysetHandle = KeysetHandle.read(JsonKeysetReader.withFile(
            keyFile), new AndroidKeystoreKmsClient().getAead(MASTER_KEY_URI));


    // DECRYPTING

    // 2. Get the primitive.
    HybridDecrypt hybridDecrypt = HybridDecryptFactory.getPrimitive(
            privateKeysetHandle);

    // 3. Use the primitive.
    byte[] decryptedBytes = hybridDecrypt.decrypt(bytes, null);

    return decryptedBytes;

  }



  static void saveBytesToFile(byte[] bytes, String filename, String path) throws IOException {

    File file = new File(path + filename);

    FileOutputStream fileOutputStream = new FileOutputStream(file.getPath());

    fileOutputStream.write(bytes);
    fileOutputStream.close();
  }



  byte[] readFileToBytes(File f) throws IOException {
    int size = (int) f.length();
    byte[] bytes = new byte[size];
    byte[] tmpBuff = new byte[size];
    FileInputStream fis = new FileInputStream(f);

    try {

      int read = fis.read(bytes, 0, size);
      if (read < size) {
        int remain = size - read;
        while (remain > 0) {
          read = fis.read(tmpBuff, 0, remain);
          System.arraycopy(tmpBuff, 0, bytes, size - remain, read);
          remain -= read;
        }
      }
    }  catch (IOException e){
      throw e;
    } finally {
      fis.close();
    }

    return bytes;
  }

  void bytesToImage (byte[] bytes, String path, String filename, boolean byteIsEncrypted) throws IOException{

    String fileExtension;

    if (byteIsEncrypted) {
      fileExtension = ".aes";
    } else {
      fileExtension = ".jpg";
    }

    File image = new File(path + filename + fileExtension);

    FileOutputStream fileOutputStream = new FileOutputStream(image.getPath());

    fileOutputStream.write(bytes);
    fileOutputStream.close();

  }

}
