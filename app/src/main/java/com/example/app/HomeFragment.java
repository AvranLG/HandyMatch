package com.example.app;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DataSnapshot;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HomeFragment extends Fragment {

    private Button btnPublicarTrabajo;
    private ImageButton btnPublicaciones;
    private ChipGroup chipGroup;
    private RecyclerView recyclerView;
    private PublicacionAdapter publicacionAdapter;
    private List<Publicacion> listaPublicaciones;
    private List<Publicacion> listaOriginal = new ArrayList<>();

    public HomeFragment() {
        // Constructor vacío requerido
    }

    public static HomeFragment newInstance() {
        return new HomeFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Inicializar vistas
        btnPublicarTrabajo = view.findViewById(R.id.fabPublicar);
        btnPublicaciones = view.findViewById(R.id.btn_menu);
        chipGroup = view.findViewById(R.id.chipGroup);
        recyclerView = view.findViewById(R.id.recyclerViewPublicaciones);

        // Configurar RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        listaPublicaciones = new ArrayList<>();
        publicacionAdapter = new PublicacionAdapter(listaPublicaciones, getContext(), false);
        recyclerView.setAdapter(publicacionAdapter);

        // Cargar las publicaciones desde Firebase
        DatabaseReference publicacionesRef = FirebaseDatabase.getInstance().getReference("publicaciones");

        publicacionesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                listaPublicaciones.clear();
                listaOriginal.clear();

                Log.d("HomeFragment", "Cantidad de publicaciones en Firebase: " + dataSnapshot.getChildrenCount());

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Publicacion publicacion = snapshot.getValue(Publicacion.class);
                    publicacion.setIdPublicacion(snapshot.getKey());
                    if (publicacion != null && "publicada".equalsIgnoreCase(publicacion.getEstadoPublicacion())) {
                        listaOriginal.add(publicacion);
                        Log.d("HomeFragment", "Publicación publicada cargada: " + publicacion.toString());
                    }
                }

                Collections.reverse(listaOriginal);

                Log.d("HomeFragment", "Total publicaciones cargadas y filtradas: " + listaOriginal.size());

                filtrarPorCategoria(getCategoriaSeleccionada());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("HomeFragment", "Error al cargar publicaciones", databaseError.toException());
            }
        });

        // Botón para publicar trabajo
        btnPublicarTrabajo.setOnClickListener(v -> {
            if (getActivity() != null) {
                Intent intent = new Intent(getActivity(), Publicar.class);
                startActivity(intent);
            } else {
                Log.e("Error", "El fragment no está adjunto a una actividad.");
            }
        });

        // Botón para abrir el menú hamburguesa
        btnPublicaciones.setOnClickListener(v -> {
            if (getActivity() instanceof HomeActivity) {
                ((HomeActivity) getActivity()).abrirDrawer();
            } else {
                Log.e("Error", "La actividad no es HomeActivity o es null.");
            }
        });

        // Seleccionar chip "todo" por defecto
        Chip chipTodo = view.findViewById(R.id.chipTodo);
        chipTodo.setChecked(true);

        // Manejar selección de chip
        chipGroup.setOnCheckedChangeListener((group, checkedId) -> filtrarPorCategoria(getCategoriaSeleccionada()));
    }

    public void agregarPublicacion(Publicacion publicacion) {
        listaPublicaciones.add(publicacion);
        publicacionAdapter.notifyItemInserted(listaPublicaciones.size() - 1);
    }

    private void filtrarPorCategoria(String categoria) {
        listaPublicaciones.clear();
        if (categoria.equals("todo")) {
            listaPublicaciones.addAll(listaOriginal);
        } else {
            for (Publicacion pub : listaOriginal) {
                if (pub.getCategoria() != null && pub.getCategoria().toLowerCase().equals(categoria)) {
                    listaPublicaciones.add(pub);
                }
            }
        }
        publicacionAdapter.notifyDataSetChanged();
    }

    private String getCategoriaSeleccionada() {
        int chipId = chipGroup.getCheckedChipId();
        if (chipId == View.NO_ID || chipId == R.id.chipTodo) {
            return "todo";
        } else {
            Chip chipSeleccionado = chipGroup.findViewById(chipId);
            return chipSeleccionado.getText().toString().toLowerCase();
        }
    }
}
