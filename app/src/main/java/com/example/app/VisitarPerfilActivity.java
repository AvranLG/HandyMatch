package com.example.app;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

public class VisitarPerfilActivity extends AppCompatActivity {

    private ImageView profileImage;

    private ImageView verifiedBadge;

    private TextView nameText, apellidosText, phoneText, emailText, direccionText, codigoPostalText, coloniaText, estadoText, ciudadText, referenciaText;
    private DatabaseReference userRef;
    private FirebaseAuth auth;
    private ImageButton btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_visitar_perfil);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        btnBack = findViewById(R.id.btn_back);
        profileImage = findViewById(R.id.profileImage);
        nameText = findViewById(R.id.nombreText);
        apellidosText = findViewById(R.id.apellidosText);
        phoneText = findViewById(R.id.telefonoText);
        emailText = findViewById(R.id.emailText);
        verifiedBadge = findViewById(R.id.verifiedBadge);


        direccionText = findViewById(R.id.edit_calle_y_numero);
        codigoPostalText = findViewById(R.id.edit_postal);
        coloniaText = findViewById(R.id.edit_colonia);
        estadoText = findViewById(R.id.edit_estado);
        ciudadText = findViewById(R.id.edit_ciudad);
        referenciaText = findViewById(R.id.edit_referencia);

        View layoutDireccionFields = findViewById(R.id.layoutDomicilioFields);
        layoutDireccionFields.setVisibility(View.GONE);

        auth = FirebaseAuth.getInstance();

        // Obtener la UID del usuario a visitar desde el Intent
        String uidRecibido = getIntent().getStringExtra("idUsuario");
        if (uidRecibido == null) {
            Log.e("VisitarPerfil", "No se recibió la UID del usuario");
            finish(); // Cierra la actividad si no se recibe la UID
            return;
        }

        userRef = FirebaseDatabase.getInstance().getReference("usuarios").child(uidRecibido);

        cargarDatosUsuario();

        ImageView ivToggle = findViewById(R.id.ivToggle);
        LinearLayout layoutDomicilio = findViewById(R.id.layoutDomicilioFields);

        ivToggle.setOnClickListener(v -> {
            if (layoutDomicilio.getVisibility() == View.GONE) {
                layoutDomicilio.setAlpha(0f);
                layoutDomicilio.setTranslationY(-30);
                layoutDomicilio.setVisibility(View.VISIBLE);

                layoutDomicilio.animate().alpha(1f).translationY(0).setDuration(300).start();
                ivToggle.animate().rotation(90f).setDuration(200).start();
            } else {
                layoutDomicilio.animate().alpha(0f).translationY(-30).setDuration(200)
                        .withEndAction(() -> layoutDomicilio.setVisibility(View.GONE)).start();
                ivToggle.animate().rotation(0f).setDuration(200).start();
            }
        });


        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void cargarDatosUsuario() {
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Log.e("VisitarPerfil", "No hay datos en Firebase");
                    return;
                }

                String nombre = snapshot.child("nombre").getValue(String.class);
                String apellidos = snapshot.child("apellidos").getValue(String.class);
                String telefono = snapshot.child("telefono").getValue(String.class);
                String email = snapshot.child("email").getValue(String.class);
                String imagenUrl = snapshot.child("imagenUrl").getValue(String.class);
                String direccion = snapshot.child("direccion").getValue(String.class);
                String codigoPostal = snapshot.child("codigo_postal").getValue(String.class);
                String colonia = snapshot.child("colonia").getValue(String.class);
                String estado = snapshot.child("estado").getValue(String.class);
                String ciudad = snapshot.child("ciudad").getValue(String.class);
                String referencia = snapshot.child("referencia").getValue(String.class);

                // Gestionar la visibilidad de la insignia

                Boolean verificado = snapshot.child("verificado").getValue(Boolean.class);
                boolean mostrarInsignia = verificado != null && verificado;

                if (verifiedBadge != null) {
                    verifiedBadge.setVisibility(mostrarInsignia ? View.VISIBLE : View.GONE);
                }

                nameText.setText(nombre != null ? nombre : "Nombre no disponible");
                apellidosText.setText(apellidos != null ? apellidos : "Apellidos no disponibles");
                phoneText.setText(telefono != null ? telefono : "Teléfono no disponible");
                emailText.setText(email != null ? email : "Correo no disponible");
                direccionText.setText(direccion != null ? direccion : "Dirección no disponible");
                codigoPostalText.setText(codigoPostal != null ? codigoPostal : "CP no disponible");
                coloniaText.setText(colonia != null ? colonia : "Colonia no disponible");
                estadoText.setText(estado != null ? estado : "Estado no disponible");
                ciudadText.setText(ciudad != null ? ciudad : "Ciudad no disponible");
                referenciaText.setText(referencia != null ? referencia : "Referencia no disponible");

                if (imagenUrl != null && !imagenUrl.isEmpty()) {
                    Glide.with(VisitarPerfilActivity.this)
                            .load(imagenUrl)
                            .placeholder(R.drawable.usuario)
                            .error(R.drawable.usuario)
                            .transform(new CircleCrop())
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .into(profileImage);
                } else {
                    profileImage.setImageResource(R.drawable.usuario);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("VisitarPerfil", "Error al cargar datos: " + error.getMessage());
            }
        });
    }

    private void abrirEditarPerfil() {
        startActivity(new Intent(this, EditarPerfilActivity.class));
    }
}
