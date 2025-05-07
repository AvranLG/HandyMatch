package com.example.app;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
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
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        recyclerView = view.findViewById(R.id.rvConversaciones);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Inicializamos la lista de conversaciones
        conversacionesList = new ArrayList<>();

        // Inicializamos el adapter con el listener para manejar los clics
        adapter = new ConversacionesAdapter(conversacionesList, getContext(), new ConversacionesAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Conversacion conversacion) {
                // Cuando se hace clic en una conversación, navegamos al detalle
                goToDetalleConversacion(conversacion);
            }
        });

        recyclerView.setAdapter(adapter);

        // Referencia a la base de datos de Firebase
        String uidActual = FirebaseAuth.getInstance().getCurrentUser().getUid();

        conversacionesRef = FirebaseDatabase.getInstance()
                .getReference("conversaciones_usuario")
                .child(uidActual);

        // Escuchamos los cambios en la base de datos
        conversacionesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // Limpiamos la lista de conversaciones
                conversacionesList.clear();

                // Log para ver cuántos elementos hay en la base de datos
                Log.d("ChatFragment", "Datos recibidos desde Firebase: " + dataSnapshot.getChildrenCount());

                // Recorrer los datos de Firebase
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String uidOtro = snapshot.getKey(); // UID del otro usuario
                    String ultimoMensaje = snapshot.child("ultimoMensaje").getValue(String.class);
                    Long timestamp = snapshot.child("timestamp").getValue(Long.class);

                    // Ahora buscamos los datos del otro usuario
                    FirebaseDatabase.getInstance().getReference("usuarios").child(uidOtro)
                            .addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot userSnapshot) {
                                    String nombre = userSnapshot.child("nombre").getValue(String.class);
                                    String fotoUrl = userSnapshot.child("imagenUrl").getValue(String.class); // ajusta el campo si se llama diferente

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
                                    // Error al cargar los datos del usuario
                                }
                            });
                }

                // Log para verificar el tamaño total de la lista cargada
                Log.d("ChatFragment", "Total de conversaciones cargadas: " + conversacionesList.size());

                // Notificamos al adaptador para actualizar la UI
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Manejo de errores si ocurre algún problema con la lectura
            }
        });

        return view;
    }

    // Metodo para navegar al DetalleConversacionFragment
    private void goToDetalleConversacion(Conversacion conversacion) {
        // Crear una nueva instancia del fragmento
        DetalleConversacionFragment detalleFragment = new DetalleConversacionFragment();

        // Pasar los datos de la conversación como argumentos
        Bundle bundle = new Bundle();
        bundle.putString("uidOtro", conversacion.getUidOtro());
        bundle.putString("nombre", conversacion.getNombre());
        bundle.putString("fotoUrl", conversacion.getFotoUrl());
        detalleFragment.setArguments(bundle);

        // Realizar la transacción de fragmentos
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentContainer, detalleFragment); // Asegúrate de que R.id.fragment_container sea el contenedor correcto
        transaction.addToBackStack(null); // Esto permitirá que puedas volver a este fragmento
        transaction.commit();
    }
}
