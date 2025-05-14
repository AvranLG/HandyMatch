package com.example.app;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.database.*;

public class item_chat extends AppCompatActivity {

    private ImageView verifiedBadge;
    private DatabaseReference userRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_chat);

        verifiedBadge = findViewById(R.id.verifiedBadge);

        // Obtenemos la UID del usuario desde el intent
        String uidRecibido = getIntent().getStringExtra("idUsuario");
        if (uidRecibido == null) {
            Log.e("item_chat", "No se recibió la UID del usuario");
            finish();
            return;
        }

        userRef = FirebaseDatabase.getInstance().getReference("usuarios").child(uidRecibido);

        cargarDatosUsuario();
    }

    private void cargarDatosUsuario() {
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Log.e("item_chat", "No hay datos del usuario");
                    return;
                }

                // Gestionar la visibilidad de la insignia
                Boolean verificado = snapshot.child("verificado").getValue(Boolean.class);
                boolean mostrarInsignia = verificado != null && verificado;

                if (verifiedBadge != null) {
                    verifiedBadge.setVisibility(mostrarInsignia ? View.VISIBLE : View.GONE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("item_chat", "Error al cargar datos: " + error.getMessage());
            }
        });
    }
}
