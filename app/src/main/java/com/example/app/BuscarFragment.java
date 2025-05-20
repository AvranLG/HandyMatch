package com.example.app;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.app.PublicacionAdapter;
import com.example.app.Publicacion;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class BuscarFragment extends Fragment {

    private RecyclerView recyclerView;
    private PublicacionAdapter adapter;
    private ArrayList<Publicacion> listaPublicaciones;
    private DatabaseReference databaseRef;
    private TextInputEditText buscarText;

    public BuscarFragment() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_buscar, container, false);

        recyclerView = view.findViewById(R.id.recyclerViewPublicaciones);
        buscarText = view.findViewById(R.id.buscarText);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        listaPublicaciones = new ArrayList<>();
        adapter = new PublicacionAdapter(listaPublicaciones, getContext());
        recyclerView.setAdapter(adapter);

        databaseRef = FirebaseDatabase.getInstance().getReference("publicaciones");

        // Buscar automáticamente al escribir
        buscarText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                buscarPorTitulo(s.toString().trim());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Cargar todas al iniciar
        buscarPorTitulo("");

        return view;
    }

    private void buscarPorTitulo(String texto) {
        String textoLower = texto.toLowerCase();
        Query query = databaseRef.orderByChild("tituloLower")
                .startAt(textoLower)
                .endAt(textoLower + "\uf8ff");

        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                listaPublicaciones.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Publicacion pub = dataSnapshot.getValue(Publicacion.class);
                    if (pub != null) {
                        listaPublicaciones.add(pub);
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // manejar error
            }
        });
    }

}
