package com.example.app;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.InputStream;

public class RegistroActivity extends AppCompatActivity {

    private static final int PICK_IMAGE = 1;
    private ImageView profileImage;

    // Declarar las variables EditText
    private EditText nombreText, apellidosText, correoText, contrasenaText, telefonoText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Toast.makeText(this, "La aplicación ha iniciado correctamente", Toast.LENGTH_SHORT).show();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registro); // Establecer el layout

        // Inicializar los EditText
        nombreText = findViewById(R.id.direccionText);
        apellidosText = findViewById(R.id.apellidosText);
        correoText = findViewById(R.id.correoText);
        contrasenaText = findViewById(R.id.passwordText);
        telefonoText = findViewById(R.id.numeroText);

        // Referencia al ImageView de la foto de perfil
        profileImage = findViewById(R.id.profileImage);

        // Configurar el clic en la imagen para abrir la galería
        profileImage.setOnClickListener(view -> openGallery());
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            setImageToCircle(imageUri);
        }
    }

    private void setImageToCircle(Uri imageUri) {
        try {
            InputStream imageStream = getContentResolver().openInputStream(imageUri);
            Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
            Drawable drawable = new BitmapDrawable(getResources(), selectedImage);
            profileImage.setImageDrawable(drawable);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Método para abrir la segunda actividad (DireccionActivity)
    public void abrirDireccion(View v) {
        // Recoger los datos de los EditText
        String nombre = nombreText.getText().toString();
        String apellidos = apellidosText.getText().toString();
        String correo = correoText.getText().toString();
        String contrasena = contrasenaText.getText().toString();
        String telefono = telefonoText.getText().toString();

        // Validar los datos antes de enviarlos (opcional)
        if (nombre.isEmpty() || apellidos.isEmpty() || correo.isEmpty() || contrasena.isEmpty() || telefono.isEmpty()) {
            // Mostrar un mensaje de error si algún campo está vacío
            Toast.makeText(this, "Todos los campos son obligatorios", Toast.LENGTH_SHORT).show();
            return; // No proceder si hay campos vacíos
        }

        // Crear un Intent para abrir la segunda actividad (DireccionActivity)
        Intent i = new Intent(this, DireccionActivity.class);

        // Pasar los datos al Intent
        i.putExtra("nombre", nombre);
        i.putExtra("apellidos", apellidos);
        i.putExtra("correo", correo);
        i.putExtra("contrasena", contrasena);
        i.putExtra("telefono", telefono);

        // Abrir la segunda actividad
        startActivity(i);
    }

    public void abrirLogin(View v) {
        Intent i = new Intent(this, LoginActivity.class);
        startActivity(i);
    }
}
