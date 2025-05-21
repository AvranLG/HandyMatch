package com.example.app;

import android.os.Bundle;
import android.util.Log;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MisPublicacionesActivity extends AppCompatActivity {

    private ImageButton btnRetroceso;
    private RecyclerView recyclerView;
    private PublicacionAdapter publicacionAdapter;
    private List<Publicacion> listaMisPublicaciones;
    private FirebaseAuth firebaseAuth;
    private FirebaseUser usuarioActual;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_mis_publicaciones);

        // Inicializar vistas
        btnRetroceso = findViewById(R.id.btn_back);
        recyclerView = findViewById(R.id.rvPublicaciones);

        // Configurar RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        listaMisPublicaciones = new ArrayList<>();

        // true para mostrar el botón eliminar
        publicacionAdapter = new PublicacionAdapter(listaMisPublicaciones, this, true);
        recyclerView.setAdapter(publicacionAdapter);

        // Autenticación
        firebaseAuth = FirebaseAuth.getInstance();
        usuarioActual = firebaseAuth.getCurrentUser();

        if (usuarioActual != null) {
            cargarMisPublicaciones(usuarioActual.getUid());
        } else {
            Log.e("MisPublicaciones", "No hay usuario autenticado");
        }

        btnRetroceso.setOnClickListener(v -> onBackPressed());
    }

    private void cargarMisPublicaciones(String uidUsuario) {
        DatabaseReference publicacionesRef = FirebaseDatabase.getInstance().getReference("publicaciones");

        publicacionesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                listaMisPublicaciones.clear();

                for (DataSnapshot child : snapshot.getChildren()) {
                    Publicacion publicacion = child.getValue(Publicacion.class);
                    if (publicacion != null && uidUsuario.equals(publicacion.getIdUsuario())) {
                        listaMisPublicaciones.add(publicacion);
                    }
                }

                // Ordenar de más reciente a más antigua
                Collections.reverse(listaMisPublicaciones);

                publicacionAdapter.notifyDataSetChanged();

                Log.d("MisPublicaciones", "Total: " + listaMisPublicaciones.size());
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("MisPublicaciones", "Error al cargar publicaciones", error.toException());
            }
        });
    }
}
