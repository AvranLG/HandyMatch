package com.example.app;

import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class SolicitudesActivity extends AppCompatActivity {

    private ImageButton btnRetroceso;
    private RecyclerView recyclerView;
    private SolicitudAdapter adapter;
    private List<Solicitud> listaSolicitudes;

    private static final String TAG = "SolicitudesActivityyyyy";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_solicitudes);

        Log.d(TAG, "onCreate iniciado");

        btnRetroceso = findViewById(R.id.btn_back);
        recyclerView = findViewById(R.id.rvPublicaciones);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        listaSolicitudes = new ArrayList<>();
        adapter = new SolicitudAdapter(listaSolicitudes, this);
        recyclerView.setAdapter(adapter);

        btnRetroceso.setOnClickListener(v -> {
            Log.d(TAG, "Botón de retroceso presionado");
            onBackPressed();
        });

        cargarSolicitudes();
    }

    private void cargarSolicitudes() {
        Log.d(TAG, "Iniciando carga de solicitudes...");

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Log.e(TAG, "Usuario no autenticado");
            Toast.makeText(this, "No estás autenticado", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Log.d(TAG, "UID autenticado: " + uid);

        FirebaseDatabase.getInstance().getReference("usuarios")
                .child(uid)
                .child("solicitudes")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Log.d(TAG, "onDataChange llamado. snapshot.exists(): " + snapshot.exists());

                        listaSolicitudes.clear();

                        if (!snapshot.exists()) {
                            Log.d(TAG, "No se encontraron solicitudes.");
                            Toast.makeText(SolicitudesActivity.this, "No tienes solicitudes", Toast.LENGTH_SHORT).show();
                            adapter.notifyDataSetChanged();
                            return;
                        }

                        int totalSolicitudes = 0;

                        for (DataSnapshot solicitudSnap : snapshot.getChildren()) {
                            totalSolicitudes++;
                            Log.d(TAG, "Procesando solicitud con key: " + solicitudSnap.getKey());

                            Solicitud solicitud = new Solicitud();

                            // idUsuarioPostulante
                            if (solicitudSnap.child("idUsuarioPostulante").exists()) {
                                String idUsuarioPostulante = solicitudSnap.child("idUsuarioPostulante").getValue(String.class);
                                solicitud.setIdUsuarioPostulante(idUsuarioPostulante);
                                Log.d(TAG, "idUsuarioPostulante: " + idUsuarioPostulante);
                            } else {
                                Log.w(TAG, "Falta campo idUsuarioPostulante");
                            }

                            // idPublicacion
                            if (solicitudSnap.child("idPublicacion").exists()) {
                                String idPublicacion = solicitudSnap.child("idPublicacion").getValue(String.class);
                                solicitud.setIdPublicacion(idPublicacion);
                                Log.d(TAG, "idPublicacion: " + idPublicacion);
                            } else {
                                Log.w(TAG, "Falta campo idPublicacion");
                            }

                            // timestamp (opcional)
                            if (solicitudSnap.child("timestamp").exists()) {
                                Long timestamp = solicitudSnap.child("timestamp").getValue(Long.class);
                                solicitud.setTimestamp(timestamp);
                                Log.d(TAG, "timestamp: " + timestamp);
                            } else {
                                Log.d(TAG, "Campo timestamp no encontrado (opcional)");
                            }

                            // Validación final
                            if (solicitud.getIdUsuarioPostulante() == null || solicitud.getIdPublicacion() == null) {
                                Log.w(TAG, "Solicitud inválida. No se añadirá a la lista.");
                                continue;
                            }

                            Log.d(TAG, "Solicitud válida añadida a la lista.");
                            listaSolicitudes.add(solicitud);
                        }

                        Log.d(TAG, "Solicitudes procesadas: " + totalSolicitudes + ", válidas: " + listaSolicitudes.size());

                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error al cargar solicitudes", error.toException());
                        Toast.makeText(SolicitudesActivity.this, "Error al cargar solicitudes", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
