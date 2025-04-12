package com.example.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class ServicioMensajeriaFirebase extends FirebaseMessagingService {

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        Log.d("FCM_TOKEN", "Token generado: " + token);

        // Guardar el token en Firebase
        guardarTokenEnFirebase(token);
    }

    private void guardarTokenEnFirebase(String token) {
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("usuarios").child(uid);
            ref.child("fcmToken").setValue(token);
        } else {
            Log.d("FCM_TOKEN", "Usuario no autenticado, no se guarda el token.");
        }
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        // Mostrar TODO lo que llega
        Log.d("FCM", "Mensaje recibido: DATA -> " + remoteMessage.getData());

        if (remoteMessage.getNotification() != null) {
            Log.d("FCM", "Mensaje recibido: NOTIFICACION -> " +
                    remoteMessage.getNotification().getTitle() + " - " +
                    remoteMessage.getNotification().getBody());
        }

        // Mostrar notificación si llega por la consola de Firebase
        String titulo = "Notificación";
        String cuerpo = "Contenido";

        if (remoteMessage.getNotification() != null) {
            titulo = remoteMessage.getNotification().getTitle();
            cuerpo = remoteMessage.getNotification().getBody();
        }

        mostrarNotificacion(titulo, cuerpo);
    }



    private void mostrarNotificacion(String titulo, String cuerpo) {
        String channelId = "default";

        // Crear canal si es necesario (Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Canal Notificaciones",
                    NotificationManager.IMPORTANCE_HIGH
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info) // puedes cambiar el ícono aquí
                .setContentTitle(titulo)
                .setContentText(cuerpo)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(Notification.DEFAULT_ALL);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(1, builder.build());
        }
    }



}
