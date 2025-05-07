package com.example.app;

import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
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

public class DetalleConversacionFragment extends Fragment {

    private ImageView imgUsuario, imgUsuarioAuth;
    private TextView tvNombreUsuario;
    private FirebaseDatabase database;
    private FirebaseAuth mAuth;
    private EditText etMensaje;
    private ImageView btnEnviar;
    private String uidOtroUsuario;
    private String uidAuth;
    private RecyclerView rvMensajes;
    private List<Mensaje> listaMensajes;
    private MensajeAdapter mensajeAdapter;

    public DetalleConversacionFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflar el layout para este fragmento
        View view = inflater.inflate(R.layout.fragment_detalle_conversacion, container, false);

        // Inicializar vistas
        imgUsuario = view.findViewById(R.id.imgUsuario);
        imgUsuarioAuth = view.findViewById(R.id.imgUsuarioAuth);
        tvNombreUsuario = view.findViewById(R.id.tvNombreUsuario);
        etMensaje = view.findViewById(R.id.etMensaje);
        btnEnviar = view.findViewById(R.id.btnEnviar);

        rvMensajes = view.findViewById(R.id.rvMensajes);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        layoutManager.setStackFromEnd(true); // Esto hará que se muestre desde abajo
        rvMensajes.setLayoutManager(layoutManager);

        listaMensajes = new ArrayList<>();
        mensajeAdapter = new MensajeAdapter(listaMensajes, getContext());
        rvMensajes.setAdapter(mensajeAdapter);

        // Inicializar FirebaseAuth y FirebaseDatabase
        mAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();

        // Obtener el UID del usuario autenticado
        uidAuth = mAuth.getCurrentUser().getUid();
        Log.d("Chat", "uidAuth: " + uidAuth);

        // Obtener el UID del otro usuario del Bundle
        uidOtroUsuario = getArguments().getString("uidOtroUsuario");
        Log.d("DetalleConversacionFragment", "UID del otro usuario: " + uidOtroUsuario);

        // Si el UID del otro usuario no es nulo, cargar los datos
        if (uidOtroUsuario != null) {
            // Cargar datos del otro usuario (foto y nombre)
            database.getReference("usuarios").child(uidOtroUsuario)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            String nombreUsuario = dataSnapshot.child("nombre").getValue(String.class);
                            String fotoUrlUsuario = dataSnapshot.child("imagenUrl").getValue(String.class);

                            tvNombreUsuario.setText(nombreUsuario);
                            Glide.with(getContext())
                                    .load(fotoUrlUsuario)
                                    .circleCrop()
                                    .into(imgUsuario);
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            Log.e("DetalleConversacionFragment", "Error al cargar datos: " + databaseError.getMessage());
                        }
                    });
        }

        // Cargar la imagen del usuario autenticado (utilizando uidAuth)
        database.getReference("usuarios").child(uidAuth)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        String fotoUrlAuth = dataSnapshot.child("imagenUrl").getValue(String.class);
                        Glide.with(getContext())
                                .load(fotoUrlAuth)
                                .circleCrop()
                                .into(imgUsuarioAuth);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.e("DetalleConversacionFragment", "Error al cargar imagen del usuario autenticado: " + databaseError.getMessage());
                    }
                });

        // Cargar los mensajes de la conversación
        DatabaseReference mensajesRef = FirebaseDatabase.getInstance()
                .getReference("conversaciones_usuario")
                .child(uidAuth)
                .child(uidOtroUsuario)
                .child("mensajes");

        mensajesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                listaMensajes.clear();
                for (DataSnapshot mensajeSnap : snapshot.getChildren()) {
                    Mensaje mensaje = mensajeSnap.getValue(Mensaje.class);
                    listaMensajes.add(mensaje);
                }
                mensajeAdapter.notifyDataSetChanged();

                // Scroll al último mensaje
                if (listaMensajes.size() > 0) {
                    rvMensajes.scrollToPosition(listaMensajes.size() - 1);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Error al cargar mensajes", Toast.LENGTH_SHORT).show();
            }
        });

        // Configuración para asegurar que el RecyclerView se ajuste cuando aparece el teclado
        rvMensajes.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if (bottom < oldBottom) {
                rvMensajes.postDelayed(() -> {
                    if (listaMensajes.size() > 0) {
                        rvMensajes.smoothScrollToPosition(listaMensajes.size() - 1);
                    }
                }, 100);
            }
        });

        // Manejo del envío de mensajes
        btnEnviar.setOnClickListener(v -> {
            String textoMensaje = etMensaje.getText().toString().trim();
            if (!textoMensaje.isEmpty()) {
                long timestamp = System.currentTimeMillis();
                String idConversacion = uidAuth + "_" + uidOtroUsuario;

                Mensaje mensaje = new Mensaje(textoMensaje, uidAuth, timestamp);

                // Rutas para emisor y receptor
                DatabaseReference refEmisor = FirebaseDatabase.getInstance().getReference()
                        .child("conversaciones_usuario").child(uidAuth).child(uidOtroUsuario);
                DatabaseReference refReceptor = FirebaseDatabase.getInstance().getReference()
                        .child("conversaciones_usuario").child(uidOtroUsuario).child(uidAuth);

                refEmisor.child("mensajes").push().setValue(mensaje);
                refReceptor.child("mensajes").push().setValue(mensaje);

                // Actualizar datos de la conversación
                Map<String, Object> datosConversacion = new HashMap<>();
                datosConversacion.put("idConversacion", idConversacion);
                datosConversacion.put("ultimoMensaje", textoMensaje);
                datosConversacion.put("timestamp", timestamp);

                refEmisor.updateChildren(datosConversacion);
                refReceptor.updateChildren(datosConversacion);

                etMensaje.setText(""); // Limpiar el campo de texto
            }
        });

        return view;
    }
}