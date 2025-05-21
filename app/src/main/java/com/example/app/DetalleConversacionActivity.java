package com.example.app;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DetalleConversacionActivity extends AppCompatActivity {

    private ImageView imgUsuario, imgUsuarioAuth, btnEnviar;
    private TextView tvNombreUsuario;
    private EditText etMensaje;
    private RecyclerView rvMensajes;
    private ImageView verifiedBadge;
    private ImageView verifiedBadge1;

    private ImageButton btnRetroceso;
    private FirebaseAuth mAuth;
    private FirebaseDatabase database;
    private List<Mensaje> listaMensajes;
    private MensajeAdapter mensajeAdapter;
    private String uidAuth, uidOtroUsuario, idUsuarioEmpleador;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalle_conversacion);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        imgUsuario = findViewById(R.id.imgUsuario);
        imgUsuarioAuth = findViewById(R.id.imgUsuarioAuth);
        tvNombreUsuario = findViewById(R.id.tvNombreUsuario);
        etMensaje = findViewById(R.id.etMensaje);
        btnEnviar = findViewById(R.id.btnEnviar);
        btnRetroceso = findViewById(R.id.imageButton3);
        rvMensajes = findViewById(R.id.rvMensajes);
        verifiedBadge = findViewById(R.id.verifiedBadge);
        verifiedBadge1 = findViewById(R.id.verifiedBadge1);



        mAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        uidAuth = mAuth.getCurrentUser().getUid();

        uidOtroUsuario = getIntent().getStringExtra("uidOtroUsuario");

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvMensajes.setLayoutManager(layoutManager);
        listaMensajes = new ArrayList<>();
        mensajeAdapter = new MensajeAdapter(listaMensajes, this);
        rvMensajes.setAdapter(mensajeAdapter);

        cargarDatosUsuario();
        cargarMensajes();

        btnEnviar.setOnClickListener(v -> enviarMensaje());
        btnRetroceso.setOnClickListener(v -> onBackPressed());
    }

    private void cargarDatosUsuario() {
        // Cargar datos del otro usuario
        database.getReference("usuarios").child(uidOtroUsuario).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String nombre = snapshot.child("nombre").getValue(String.class);
                String fotoUrl = snapshot.child("imagenUrl").getValue(String.class);
                tvNombreUsuario.setText(nombre);
                Glide.with(DetalleConversacionActivity.this).load(fotoUrl).circleCrop().into(imgUsuario);

                Boolean verificadoOtro = snapshot.child("verificado").getValue(Boolean.class);
                boolean mostrarInsigniaOtro = verificadoOtro != null && verificadoOtro;

                if (verifiedBadge != null) {
                    verifiedBadge.setVisibility(mostrarInsigniaOtro ? View.VISIBLE : View.GONE);
                    Log.d("Insignia_OtroUsuario", "Insignia " + (mostrarInsigniaOtro ? "visible" : "oculta"));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Chat", "Error al cargar datos del otro usuario: " + error.getMessage());
            }
        });

        // Cargar datos del usuario autenticado
        String uidAuth = mAuth.getCurrentUser().getUid();
        database.getReference("usuarios").child(uidAuth).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String fotoUrlAuth = snapshot.child("imagenUrl").getValue(String.class);
                Glide.with(DetalleConversacionActivity.this).load(fotoUrlAuth).circleCrop().into(imgUsuarioAuth);

                Boolean verificadoAuth = snapshot.child("verificado").getValue(Boolean.class);
                boolean mostrarInsigniaAuth = verificadoAuth != null && verificadoAuth;

                if (verifiedBadge1 != null) {
                    verifiedBadge1.setVisibility(mostrarInsigniaAuth ? View.VISIBLE : View.GONE);
                    Log.d("Insignia_UsuarioAuth", "Insignia " + (mostrarInsigniaAuth ? "visible" : "oculta"));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Chat", "Error al cargar datos del usuario autenticado: " + error.getMessage());
            }
        });
    }


    private void enviarMensaje() {
        String texto = etMensaje.getText().toString().trim();
        if (!texto.isEmpty()) {
            long timestamp = System.currentTimeMillis();
            Mensaje mensaje = new Mensaje(texto, uidAuth, timestamp);

            // Referencias para guardar el mensaje en ambas conversaciones (emisor y receptor)
            DatabaseReference refEmisor = database.getReference("conversaciones_usuario").child(uidAuth).child(uidOtroUsuario);
            DatabaseReference refReceptor = database.getReference("conversaciones_usuario").child(uidOtroUsuario).child(uidAuth);

            // Guardar el mensaje en ambos nodos
            refEmisor.child("mensajes").push().setValue(mensaje);
            refReceptor.child("mensajes").push().setValue(mensaje);

            // Actualizar el último mensaje y el timestamp en ambas conversaciones
            Map<String, Object> ultimoMensajeMap = new HashMap<>();
            ultimoMensajeMap.put("ultimoMensaje", texto);
            ultimoMensajeMap.put("timestamp", timestamp);

            refEmisor.updateChildren(ultimoMensajeMap);
            refReceptor.updateChildren(ultimoMensajeMap);

            // Limpiar el campo de texto
            etMensaje.setText("");
        }
    }


    private void cargarMensajes() {
        DatabaseReference mensajesRef = database.getReference("conversaciones_usuario").child(uidAuth).child(uidOtroUsuario).child("mensajes");
        mensajesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                listaMensajes.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    Mensaje mensaje = data.getValue(Mensaje.class);
                    listaMensajes.add(mensaje);
                }
                mensajeAdapter.notifyDataSetChanged();
                rvMensajes.scrollToPosition(listaMensajes.size() - 1);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(DetalleConversacionActivity.this, "Error al cargar mensajes", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
