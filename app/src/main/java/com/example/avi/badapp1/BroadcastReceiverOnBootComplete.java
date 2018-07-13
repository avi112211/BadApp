package com.example.avi.badapp1;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class BroadcastReceiverOnBootComplete extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent intent1 = new Intent(context,MyService.class);
        context.startService(intent1);

        /*
        Toast.makeText(context, "before", Toast.LENGTH_LONG).show();
        if (intent.getAction().equalsIgnoreCase(Intent.ACTION_BOOT_COMPLETED)) {
            Toast.makeText(context, "after", Toast.LENGTH_LONG).show();
            Intent serviceIntent = new Intent(context, MyService.class);
            context.startService(serviceIntent);
        }
        */
    }
}