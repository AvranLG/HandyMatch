package com.example.app;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

public class HandyMatchDialogFragment extends DialogFragment {

    private String idUsuarioEmpleador;

    public static HandyMatchDialogFragment newInstance(String idUsuario) {
        HandyMatchDialogFragment fragment = new HandyMatchDialogFragment();
        Bundle args = new Bundle();
        args.putString("idUsuario", idUsuario);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_confirmar_handymatch, container, false);

        idUsuarioEmpleador = getArguments().getString("idUsuario");

        Button btnVisitarPerfil = view.findViewById(R.id.btnVerPerfil);
        btnVisitarPerfil.setOnClickListener(v -> {
            if (idUsuarioEmpleador != null) {
                Intent intent = new Intent(requireActivity(), VisitarPerfilActivity.class);
                intent.putExtra("idUsuario", idUsuarioEmpleador);

                try {
                    if (getActivity() != null) {
                        startActivity(intent);
                        dismiss();
                    } else {
                        Log.e("HandyMatchDialogFragment", "Actividad no disponible, contexto es null");
                    }
                } catch (Exception e) {
                    Log.e("HandyMatchDialogFragment", "Error al iniciar la actividad", e);
                }
            } else {
                Log.e("HandyMatchDialogFragment", "idUsuarioEmpleador es null");
            }
        });
//Botón de cancelar
        Button btnCancelar = view.findViewById(R.id.btnCancelar);
        btnCancelar.setOnClickListener(v -> dismiss());

        return view;
    }
}
