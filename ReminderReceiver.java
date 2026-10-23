package com.rrmotor.remindergantioli;

import android.app.*;
import android.content.*;
import android.os.Build;

public class ReminderReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        String name=intent==null?"Pelanggan":intent.getStringExtra("customerName");
        NotificationManager nm=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        if(Build.VERSION.SDK_INT>=26 && nm.getNotificationChannel("rr_due")==null) {
            nm.createNotificationChannel(new NotificationChannel("rr_due","Pengingat Ganti Oli",NotificationManager.IMPORTANCE_HIGH));
        }
        Notification notification=new Notification.Builder(context,"rr_due")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("RR MOTOR — Jatuh Tempo Ganti Oli")
            .setContentText("Pengingat jadwal ganti oli: "+name)
            .setStyle(new Notification.BigTextStyle().bigText("Periksa daftar pelanggan RR MOTOR yang sudah jatuh tempo dan kirim pengingat WhatsApp."))
            .setAutoCancel(true).build();
        nm.notify((int)(System.currentTimeMillis()%100000),notification);
    }
}
