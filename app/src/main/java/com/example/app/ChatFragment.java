package com.example.app;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

public class ChatFragment extends Fragment {

    private RecyclerView recyclerView;
    private ConversacionesAdapter adapter;
    private List<Conversacion> conversacionesList;
    private DatabaseReference conversacionesRef;

    public ChatFragment() {
        // Constructor público requerido
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        recyclerView = view.findViewById(R.id.rvConversaciones);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        conversacionesList = new ArrayList<>();
        adapter = new ConversacionesAdapter(conversacionesList, getContext(), null);
        recyclerView.setAdapter(adapter);

        String uidActual = FirebaseAuth.getInstance().getCurrentUser().getUid();

        conversacionesRef = FirebaseDatabase.getInstance()
                .getReference("conversaciones_usuario")
                .child(uidActual);

        // Actualización eficiente sin duplicados
        conversacionesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Crear una lista temporal
                List<Conversacion> nuevasConversaciones = new ArrayList<>();

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String uidOtro = snapshot.getKey();
                    String ultimoMensaje = snapshot.child("ultimoMensaje").getValue(String.class);
                    Long timestamp = snapshot.child("timestamp").getValue(Long.class);

                    FirebaseDatabase.getInstance().getReference("usuarios").child(uidOtro)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot userSnapshot) {
                                    String nombre = userSnapshot.child("nombre").getValue(String.class);
                                    String fotoUrl = userSnapshot.child("imagenUrl").getValue(String.class);

                                    Conversacion conversacion = new Conversacion(
                                            uidOtro,
                                            nombre != null ? nombre : "Usuario",
                                            fotoUrl != null ? fotoUrl : "",
                                            ultimoMensaje != null ? ultimoMensaje : "",
                                            timestamp != null ? timestamp : 0
                                    );

                                    // Agregar a la lista temporal
                                    nuevasConversaciones.add(conversacion);

                                    // Verificar si se han cargado todas las conversaciones
                                    if (nuevasConversaciones.size() == dataSnapshot.getChildrenCount()) {
                                        // Reemplazar la lista completa solo cuando se termine de cargar
                                        conversacionesList.clear();
                                        conversacionesList.addAll(nuevasConversaciones);
                                        adapter.notifyDataSetChanged();
                                    }
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {
                                    Log.e("ChatFragment", "Error al cargar usuario: " + databaseError.getMessage());
                                }
                            });
                }

                // Si no hay conversaciones, limpiar directamente
                if (!dataSnapshot.hasChildren()) {
                    conversacionesList.clear();
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("ChatFragment", "Error al cargar conversaciones: " + databaseError.getMessage());
            }
        });

        return view;
    }
}
