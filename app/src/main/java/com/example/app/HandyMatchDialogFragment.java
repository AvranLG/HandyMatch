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

        Button btnConfirmarHandy = view.findViewById(R.id.btnConfirmarHandy);
        btnConfirmarHandy.setOnClickListener(v -> {
            String uidActual = FirebaseAuth.getInstance().getCurrentUser().getUid();
            String uidOtro = idUsuarioEmpleador;

            if (uidActual == null || uidOtro == null) {
                Log.e("HandyMatch", "UIDs no válidos");
                return;
            }

            String idConversacion = uidActual.compareTo(uidOtro) < 0 ?
                    uidActual + "_" + uidOtro : uidOtro + "_" + uidActual;

            DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();

            // Crear nodo vacío para mensajes
            dbRef.child("mensajes").child(idConversacion).setValue(null);

            // Crear datos de la conversación
            Map<String, Object> conversacionData = new HashMap<>();
            conversacionData.put("idConversacion", idConversacion);
            conversacionData.put("ultimoMensaje", "");
            conversacionData.put("timestamp", ServerValue.TIMESTAMP);

            // Guardar para ambos usuarios
            dbRef.child("conversaciones_usuario").child(uidActual).child(uidOtro).setValue(conversacionData);
            dbRef.child("conversaciones_usuario").child(uidOtro).child(uidActual).setValue(conversacionData);

            Log.d("HandyMatch", "Conversación creada entre " + uidActual + " y " + uidOtro);
            dismiss(); // opcional, cerrar el diálogo
        });


        return view;
    }
}
