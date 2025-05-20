package com.example.app;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.app.Publicacion;
import com.example.app.PublicacionAdapter;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.database.*;

import java.util.ArrayList;


public class BuscarFragment extends Fragment {

    private int busquedaActualId = 0;


    private RecyclerView recyclerView;
    private PublicacionAdapter adapter;
    private ArrayList<Publicacion> listaPublicaciones;
    private TextInputEditText buscarText;

    private String modoBusqueda = "publicaciones"; // o "usuarios"
    private String textoBusquedaActual = "";

    private DatabaseReference refPublicaciones;
    private DatabaseReference refUsuarios;

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

        Chip chipPublicaciones = view.findViewById(R.id.chipPublicaciones);
        Chip chipUsuarios = view.findViewById(R.id.chipUsuarios);
        ChipGroup chipGroup = view.findViewById(R.id.chipGroup);

        chipPublicaciones.setChecked(true); // por defecto

        refPublicaciones = FirebaseDatabase.getInstance().getReference("publicaciones");
        refUsuarios = FirebaseDatabase.getInstance().getReference("usuarios");

        // Cambiar modo de búsqueda según el chip seleccionado
        chipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipPublicaciones) {
                modoBusqueda = "publicaciones";
                buscarPorTitulo(textoBusquedaActual);
            } else if (checkedId == R.id.chipUsuarios) {
                modoBusqueda = "usuarios";
                buscarPorUsuario(textoBusquedaActual);
            }
        });

        // Detectar texto ingresado
        buscarText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                textoBusquedaActual = s.toString().trim();
                if (modoBusqueda.equals("publicaciones")) {
                    buscarPorTitulo(textoBusquedaActual);
                } else {
                    buscarPorUsuario(textoBusquedaActual);
                }
            }
        });

        // Cargar publicaciones al iniciar
        buscarPorTitulo("");

        return view;
    }

    private void buscarPorTitulo(String texto) {
        String textoLower = texto.toLowerCase();
        Query query = refPublicaciones.orderByChild("tituloLower")
                .startAt(textoLower)
                .endAt(textoLower + "\uf8ff");

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                listaPublicaciones.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    Publicacion pub = ds.getValue(Publicacion.class);
                    if (pub != null) {
                        listaPublicaciones.add(pub);
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void buscarPorUsuario(String texto) {
        final int idBusqueda = ++busquedaActualId; // Aumenta el contador para cada nueva búsqueda

        Log.d("BuscarFragment", "Buscando usuarios con nombre que empieza con: " + texto);

        Query query = refUsuarios.orderByChild("nombre")
                .startAt(texto)
                .endAt(texto + "\uf8ff");

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Verifica si esta búsqueda sigue siendo la actual
                if (idBusqueda != busquedaActualId) {
                    Log.d("BuscarFragment", "Búsqueda obsoleta. Ignorada.");
                    return;
                }

                listaPublicaciones.clear();

                if (!snapshot.exists()) {
                    Log.d("BuscarFragment", "No se encontraron usuarios con ese nombre.");
                    adapter.notifyDataSetChanged();
                    return;
                }

                for (DataSnapshot ds : snapshot.getChildren()) {
                    String idUsuario = ds.getKey();
                    Log.d("BuscarFragment", "Usuario encontrado: ID = " + idUsuario);

                    cargarPublicacionesDeUsuario(idUsuario, idBusqueda); // ahora pasa el ID de búsqueda
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("BuscarFragment", "Error al buscar usuarios: " + error.getMessage());
            }
        });
    }


    private void cargarPublicacionesDeUsuario(String idUsuario, int idBusqueda) {
        Log.d("BuscarFragment", "Cargando publicaciones de usuario: " + idUsuario);

        Query query = refPublicaciones.orderByChild("idUsuario").equalTo(idUsuario);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Ignorar si ya hay una nueva búsqueda en curso
                if (idBusqueda != busquedaActualId) {
                    Log.d("BuscarFragment", "Ignorando publicaciones de búsqueda anterior");
                    return;
                }

                if (!snapshot.exists()) {
                    Log.d("BuscarFragment", "Este usuario no tiene publicaciones.");
                }

                for (DataSnapshot ds : snapshot.getChildren()) {
                    Publicacion pub = ds.getValue(Publicacion.class);
                    if (pub != null && !listaPublicaciones.contains(pub)) {
                        Log.d("BuscarFragment", "Publicación encontrada: " + pub.getTitulo());
                        listaPublicaciones.add(pub);
                    }
                }

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("BuscarFragment", "Error al cargar publicaciones: " + error.getMessage());
            }
        });
    }


}
