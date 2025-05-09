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
        database.getReference("usuarios").child(uidOtroUsuario).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String nombre = snapshot.child("nombre").getValue(String.class);
                String fotoUrl = snapshot.child("imagenUrl").getValue(String.class);
                tvNombreUsuario.setText(nombre);
                Glide.with(DetalleConversacionActivity.this).load(fotoUrl).circleCrop().into(imgUsuario);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("Chat", "Error al cargar datos del usuario: " + error.getMessage());
            }
        });


        // Cargar los datos del usuario autenticado
        String uidAuth = mAuth.getCurrentUser().getUid();
        database.getReference("usuarios").child(uidAuth).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String fotoUrlAuth = snapshot.child("imagenUrl").getValue(String.class);
                Glide.with(DetalleConversacionActivity.this).load(fotoUrlAuth).circleCrop().into(imgUsuarioAuth);
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

            DatabaseReference refEmisor = database.getReference("conversaciones_usuario").child(uidAuth).child(uidOtroUsuario).child("mensajes");
            DatabaseReference refReceptor = database.getReference("conversaciones_usuario").child(uidOtroUsuario).child(uidAuth).child("mensajes");

            refEmisor.push().setValue(mensaje);
            refReceptor.push().setValue(mensaje);
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
