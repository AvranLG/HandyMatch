package com.example.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class BuscarFragment extends Fragment {

    // UI components
    private TextInputEditText buscarText;
    private MaterialButton buttonCategoria;
    private MaterialButton buttonUbicacion;
    private MaterialButton buttonPresupuesto;
    private TextView pintura, instalacion, limpieza, reparacion, limpiezaVentana;
    private ImageButton flecha1, flecha2, flecha3, flecha4, flecha5;

    public BuscarFragment() {
        // Constructor vacío requerido
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_buscar, container, false);

        // Inicializar componentes UI
        initializeUI(view);

        // Configurar listeners
        setupListeners();

        return view;
    }

    private void initializeUI(View view) {
        buscarText = view.findViewById(R.id.buscarText);
        buttonCategoria = view.findViewById(R.id.ButtonCategoría);
        buttonUbicacion = view.findViewById(R.id.ButtonUbicación);
        buttonPresupuesto = view.findViewById(R.id.ButtonPresupuesto);

        pintura = view.findViewById(R.id.pintura);
        instalacion = view.findViewById(R.id.instalacion);
        limpieza = view.findViewById(R.id.limpieza);
        reparacion = view.findViewById(R.id.reparacion);
        limpiezaVentana = view.findViewById(R.id.limpiezaVentana);

        flecha1 = view.findViewById(R.id.flecha1);
        flecha2 = view.findViewById(R.id.flecha2);
        flecha3 = view.findViewById(R.id.flecha3);
        flecha4 = view.findViewById(R.id.flecha4);
        flecha5 = view.findViewById(R.id.flecha5);
    }

    private void setupListeners() {
        // Configurar el clic para los filtros
        buttonCategoria.setOnClickListener(v -> {
            // Código para manejar el clic en categoría
        });

        buttonUbicacion.setOnClickListener(v -> {
            // Código para manejar el clic en ubicación
        });

        buttonPresupuesto.setOnClickListener(v -> {
            // Código para manejar el clic en presupuesto
        });

        // Configurar clics en las opciones de trabajo
        flecha1.setOnClickListener(v -> {
            // Código para manejar el clic en pintura exterior
        });

        flecha2.setOnClickListener(v -> {
            // Código para manejar el clic en instalación de puertas
        });

        flecha3.setOnClickListener(v -> {
            // Código para manejar el clic en limpieza de alfombra
        });

        flecha4.setOnClickListener(v -> {
            // Código para manejar el clic en reparación de persianas
        });

        flecha5.setOnClickListener(v -> {
            // Código para manejar el clic en limpieza de ventanas
        });
    }
}