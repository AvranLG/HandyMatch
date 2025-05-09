package com.example.app;

import android.content.Context;
import android.content.Intent;
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

        conversacionesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                conversacionesList.clear();

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

                                    conversacionesList.add(conversacion);
                                    adapter.notifyDataSetChanged();
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {
                                    Log.e("ChatFragment", "Error al cargar usuario: " + databaseError.getMessage());
                                }
                            });
                }

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("ChatFragment", "Error al cargar conversaciones: " + databaseError.getMessage());
            }
        });

        return view;
    }
}
