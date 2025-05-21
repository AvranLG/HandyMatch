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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;

import java.util.HashMap;
import java.util.Map;


public class HandyMatchDialogFragment extends DialogFragment {

    private String idUsuarioEmpleador;

    public static HandyMatchDialogFragment newInstance(String idUsuario, String idPublicacion) {
        HandyMatchDialogFragment fragment = new HandyMatchDialogFragment();
        Bundle args = new Bundle();
        args.putString("idUsuario", idUsuario);
        args.putString("idPublicacion", idPublicacion);
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

// Botón de confirmar HandyMatch
        Button btnConfirmarHandy = view.findViewById(R.id.btnConfirmarHandy);
        btnConfirmarHandy.setOnClickListener(v -> {
            String uidActual = FirebaseAuth.getInstance().getCurrentUser().getUid();
            String uidOtro = idUsuarioEmpleador;
            String idPublicacion = getArguments().getString("idPublicacion"); // Asegurar obtener idPublicacion

            if (uidActual == null || uidOtro == null || idPublicacion == null) {
                Log.e("HandyMatch", "Datos incompletos");
                return;
            }

            DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();

            // 🔹 Crear conversación (código sin cambios)
            // ... (mantén tu lógica existente de creación de conversación)

            // 🔹 Enviar solicitud - PARTE CORREGIDA
            DatabaseReference solicitudesRef = dbRef.child("usuarios")
                    .child(uidOtro)
                    .child("solicitudes");

            String idSolicitud = solicitudesRef.push().getKey();

            Map<String, Object> datosSolicitud = new HashMap<>();
            datosSolicitud.put("idSolicitud", idSolicitud);
            datosSolicitud.put("idUsuarioPostulante", uidActual);
            datosSolicitud.put("idPublicacion", idPublicacion); // Campo crucial faltante
            datosSolicitud.put("timestamp", ServerValue.TIMESTAMP);

            // Guardar bajo el ID generado
            solicitudesRef.child(idSolicitud).setValue(datosSolicitud)
                    .addOnSuccessListener(unused -> {
                        Log.d("HandyMatch", "Solicitud guardada con ID: " + idSolicitud);
                        Log.d("HandyMatch", "Detalles: Publicación=" + idPublicacion + ", Postulante=" + uidActual);
                    })
                    .addOnFailureListener(e -> Log.e("HandyMatch", "Error al guardar", e));

            dismiss();
        });



        return view;
    }
}
