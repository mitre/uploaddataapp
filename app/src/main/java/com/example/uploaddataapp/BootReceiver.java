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

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        context.startService(new Intent(context, RecordIntentService.class));
    }
}
