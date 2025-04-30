package com.example.app;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import android.widget.ImageView;
import com.bumptech.glide.Glide;
import jp.wasabeef.glide.transformations.RoundedCornersTransformation;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;



import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.yalantis.ucrop.UCrop;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

public class EditarPerfilActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_PICK = 1001;
    private static final int UCROP_REQUEST_CODE = 69;

    ImageView profileImage, logoImagen;

    DatabaseReference databaseRef;
    FirebaseAuth auth;
    FirebaseUser currentUser;

    private static final String KEY_IMAGE_PATH = "cropped_image_path";
    private String currentImagePath = null;


    TextInputEditText nombreText, apellidosText, numeroText, direccionText, postalText, coloniaText;
    private EditText ciudadText;
    private EditText estadoText;
    private static final String GOOGLE_MAPS_API_KEY = BuildConfig.MAPS_API_KEY;

    // Variable para almacenar la URL de la imagen
    private String fotoUrl = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editar_perfil);

        if (savedInstanceState != null) {
            currentImagePath = savedInstanceState.getString(KEY_IMAGE_PATH);
            if (currentImagePath != null) {
                File file = new File(currentImagePath);
                if (file.exists()) {
                    Bitmap bitmap = BitmapFactory.decodeFile(currentImagePath);
                    profileImage.setImageBitmap(bitmap);
                }
            }
        }


        profileImage = findViewById(R.id.profileImage);
        logoImagen = findViewById(R.id.logoImagen);

        // Escucha para abrir galería
        logoImagen.setOnClickListener(v -> abrirGaleria());

        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();

        nombreText = findViewById(R.id.nombreText);
        apellidosText = findViewById(R.id.apellidosText);
        numeroText = findViewById(R.id.numeroText);
        direccionText = findViewById(R.id.direccionText);
        postalText = findViewById(R.id.postalText);
        coloniaText = findViewById(R.id.coloniaText);
        ciudadText = findViewById(R.id.ciudadText);
        estadoText = findViewById(R.id.estadoText);
        profileImage = findViewById(R.id.profileImage);

        if (currentUser != null) {
            String uid = currentUser.getUid();
            databaseRef = FirebaseDatabase.getInstance().getReference("usuarios").child(uid);

            databaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String nombre = snapshot.child("nombre").getValue(String.class);
                        String apellidos = snapshot.child("apellidos").getValue(String.class);
                        String telefono = snapshot.child("telefono").getValue(String.class);
                        String direccion = snapshot.child("direccion").getValue(String.class);
                        String codigoPostal = snapshot.child("codigo_postal").getValue(String.class);
                        String colonia = snapshot.child("colonia").getValue(String.class);

                        nombreText.setText(nombre);
                        apellidosText.setText(apellidos);
                        numeroText.setText(telefono);
                        direccionText.setText(direccion);
                        postalText.setText(codigoPostal);
                        coloniaText.setText(colonia);
                    } else {
                        Toast.makeText(EditarPerfilActivity.this, "No se encontraron datos del usuario", Toast.LENGTH_SHORT).show();
                    }

                    fotoUrl = snapshot.child("imagenUrl").getValue(String.class); // Guardamos la URL de la imagen

                    if (fotoUrl != null && !fotoUrl.isEmpty()) {
                        Glide.with(EditarPerfilActivity.this)
                                .load(fotoUrl)
                                .apply(RequestOptions.bitmapTransform(new CircleCrop()))
                                .placeholder(R.drawable.circulo_imagen)
                                .error(R.drawable.circulo_imagen)
                                .into(profileImage);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(EditarPerfilActivity.this, "Error al cargar datos", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (currentImagePath != null) {
            outState.putString(KEY_IMAGE_PATH, currentImagePath);
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Si el usuario selecciona una imagen
        if (requestCode == REQUEST_IMAGE_PICK && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri sourceUri = data.getData();

            // Uri temporal donde se guardará la imagen recortada
            Uri destinationUri = Uri.fromFile(new File(getCacheDir(), "croppedImage.jpg"));

            UCrop.Options options = new UCrop.Options();
            options.setShowCropFrame(false); // Elimina el borde cuadrado
            options.setCircleDimmedLayer(true); // Muestra el recorte circular
            options.setHideBottomControls(true); // Oculta botones extra
            options.setCompressionQuality(90);  // Compresión de imagen

            // Configuración de UCrop para recortar la imagen
            UCrop.of(sourceUri, destinationUri)
                    .withAspectRatio(1, 1)  // Establece el aspecto 1:1 (cuadrado)
                    .withMaxResultSize(600, 600)  // Tamaño máximo de la imagen recortada
                    .withOptions(options)  // Aplica las opciones
                    .start(this);  // Inicia el recorte
        }
        // Si el usuario terminó de recortar la imagen
        else if (requestCode == UCrop.REQUEST_CROP && resultCode == RESULT_OK) {
            Uri resultUri = UCrop.getOutput(data);
            if (resultUri != null) {
                currentImagePath = resultUri.getPath();  // Guardamos la ruta para restaurarla luego

                Bitmap bitmap = BitmapFactory.decodeFile(currentImagePath);
                if (bitmap != null) {
                    profileImage.setImageBitmap(bitmap);
                    profileImage.invalidate();
                    profileImage.requestLayout();
                    Toast.makeText(this, "Imagen actualizada correctamente", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "No se pudo cargar la imagen", Toast.LENGTH_SHORT).show();
                }
            }
        }
 else if (resultCode == UCrop.RESULT_ERROR) {
            Throwable cropError = UCrop.getError(data);
            Toast.makeText(this, "Error al recortar: " + cropError.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Restaurar la imagen de perfil si está disponible
        if (fotoUrl != null && !fotoUrl.isEmpty()) {
            Glide.with(this)
                    .load(fotoUrl)
                    .apply(RequestOptions.bitmapTransform(new CircleCrop()))
                    .placeholder(R.drawable.circulo_imagen)
                    .error(R.drawable.circulo_imagen)
                    .into(profileImage);
        }
    }

    private void abrirGaleria() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_IMAGE_PICK);
    }
}
