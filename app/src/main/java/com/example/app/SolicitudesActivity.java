package com.example.app;

import android.os.Bundle;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;

public class SolicitudesActivity extends AppCompatActivity {
    private ImageButton btnRetroceso;
    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_solicitudes);

        // Inicializar vistas
        btnRetroceso = findViewById(R.id.btn_back);
        recyclerView = findViewById(R.id.rvPublicaciones);

        btnRetroceso.setOnClickListener(v -> onBackPressed());

    }
}