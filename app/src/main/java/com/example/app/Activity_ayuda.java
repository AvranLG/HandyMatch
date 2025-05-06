package com.example.app;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

public class Activity_ayuda extends AppCompatActivity {

    private final String correoSoporte = "soporte.handymatch@gmail.com";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ayuda);

        TextView correoTexto = findViewById(R.id.correo_texto);
        Button btnEnviarCorreo = findViewById(R.id.btn_enviar_correo);

        // Copiar correo al portapapeles
        correoTexto.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Correo soporte", correoSoporte);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Correo copiado al portapapeles", Toast.LENGTH_SHORT).show();
        });

        // Intentar abrir Gmail directamente al hacer clic en el botón
        btnEnviarCorreo.setOnClickListener(v -> {
            String subject = Uri.encode("Soporte HandyMatch");
            String body = Uri.encode("Hola, necesito ayuda con...");
            Uri uri = Uri.parse("mailto:" + correoSoporte + "?subject=" + subject + "&body=" + body);

            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(uri);

            try {
                startActivity(Intent.createChooser(intent, "Enviar correo con..."));
            } catch (android.content.ActivityNotFoundException ex) {
                Toast.makeText(this, "No se encontró una app de correo", Toast.LENGTH_SHORT).show();
            }
        });



        // Cambiar el color de la barra de estado
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.setStatusBarColor(Color.parseColor("#FFFFFF"));
    }
}
