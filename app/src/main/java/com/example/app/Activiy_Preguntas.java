package com.example.app;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class Activiy_Preguntas extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_preguntas);

        // Ajuste de márgenes del sistema
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Obtener el TextView
        TextView tvCorreo = findViewById(R.id.tvCorreoSoporte);

        // Asignar evento de clic para copiar al portapapeles
        tvCorreo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String texto = tvCorreo.getText().toString().trim();
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Correo de soporte", texto);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(Activiy_Preguntas.this, "Correo copiado al portapapeles", Toast.LENGTH_SHORT).show();
            }
        });

        // Cambiar el color de la barra de estado
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.setStatusBarColor(Color.parseColor("#FFFFFF"));
    }

}