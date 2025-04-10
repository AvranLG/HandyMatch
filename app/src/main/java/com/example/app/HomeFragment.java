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

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private Button btnPublicarTrabajo;
    private ImageButton btnNotificacion;
    private ChipGroup chipGroup;
    private RecyclerView recyclerView;
    private PublicacionAdapter publicacionAdapter;
    private List<Publicacion> listaPublicaciones;

    public HomeFragment() {
        // Constructor vacío requerido
    }

    public static HomeFragment newInstance() {
        return new HomeFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflar el layout para este fragmento
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);


        // Inicializar vistas
        btnPublicarTrabajo = view.findViewById(R.id.btPublicarTrabajo);
        btnNotificacion = view.findViewById(R.id.btn_notificacion);
        chipGroup = view.findViewById(R.id.chipGroup);
        recyclerView = view.findViewById(R.id.recyclerViewPublicaciones);

        // Configurar RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        listaPublicaciones = new ArrayList<>();
        publicacionAdapter = new PublicacionAdapter(listaPublicaciones, getContext());
        recyclerView.setAdapter(publicacionAdapter);

        // Cargar las publicaciones desde Firebase
        DatabaseReference publicacionesRef = FirebaseDatabase.getInstance().getReference("publicaciones");

        publicacionesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                listaPublicaciones.clear(); // Limpiar lista antes de agregar nuevas publicaciones

                Log.d("HomeFragment", "Cantidad de publicaciones en Firebase: " + dataSnapshot.getChildrenCount());

                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Publicacion publicacion = snapshot.getValue(Publicacion.class);
                    if (publicacion != null) {
                        listaPublicaciones.add(publicacion);
                        Log.d("HomeFragment", "Publicación cargada: " + publicacion.toString());
                    } else {
                        Log.e("HomeFragment", "Error al convertir publicación: " + snapshot.getKey());
                    }
                }

                Log.d("HomeFragment", "Total publicaciones cargadas: " + listaPublicaciones.size());

                // Actualizar el adaptador con las nuevas publicaciones
                publicacionAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Manejar errores de Firebase
                Log.e("HomeFragment", "Error al cargar publicaciones", databaseError.toException());
            }
        });

        // Configurar listeners
        btnPublicarTrabajo.setOnClickListener(v -> {
            // Acción para publicar trabajo
            if (getActivity() != null) {
                Intent intent = new Intent(getActivity(), Publicar.class);
                startActivity(intent);
            } else {
                Log.e("Error", "El fragment no está adjunto a una actividad.");
            }
        });

        btnNotificacion.setOnClickListener(v -> {
            // Acción para ver notificaciones
        });

        // Configurar el chip seleccionado por defecto
        Chip chipTodo = view.findViewById(R.id.chipTodo);
        chipTodo.setChecked(true);

        // Configurar listener para los chips
        chipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            // Manejar la selección de categoría
            // Por ejemplo:
            if (checkedId == R.id.chipTodo) {
                // Filtrar por "Todo"
            } else if (checkedId == R.id.chipHogar) {
                // Filtrar por "Hogar"
            }
            // etc.
        });
    }

    // Metodo para agregar una nueva publicación
    public void agregarPublicacion(Publicacion publicacion) {
        listaPublicaciones.add(publicacion);
        publicacionAdapter.notifyItemInserted(listaPublicaciones.size() - 1); // Notificar que se insertó un nuevo ítem
    }
}
