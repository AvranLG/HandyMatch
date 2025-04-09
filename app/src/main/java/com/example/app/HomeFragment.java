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

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

public class HomeFragment extends Fragment {

    private Button btnPublicarTrabajo;
    private ImageButton btnNotificacion;
    private ChipGroup chipGroup;

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

        // Configurar listeners
        btnPublicarTrabajo.setOnClickListener(v -> {
            // Acción para publicar trabajo
            if (getActivity() != null) {
                Intent intent = new Intent(getActivity(), Publicar.class);
                startActivity(intent);
            } else {
                Log.e("Error", "El fragment no está adjunto a una actividad.");
            }

            // Por ejemplo, navegar a otro fragmento:
            // requireActivity().getSupportFragmentManager().beginTransaction()
            //     .replace(R.id.fragmentContainer, new PublicarTrabajoFragment())
            //     .addToBackStack(null)
            //     .commit();
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

}