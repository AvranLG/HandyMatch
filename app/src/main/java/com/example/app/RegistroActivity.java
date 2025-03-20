package com.example.app;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;


import androidx.appcompat.app.AppCompatActivity;

import java.io.InputStream;

public class RegistroActivity extends AppCompatActivity {

    private static final int PICK_IMAGE = 1;
    private ImageView profileImage;
    private ImageView logoImage; // El ImageView que contiene el logo
    private Uri imageUri; // Guardar la URI de la imagen seleccionada

    // Declarar las variables EditText
    private EditText nombreText, apellidosText, correoText, contrasenaText, telefonoText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registro); // Establecer el layout

        // Inicializar los EditText
        nombreText = findViewById(R.id.direccionText);
        apellidosText = findViewById(R.id.apellidosText);
        correoText = findViewById(R.id.correoText);
        contrasenaText = findViewById(R.id.passwordText);
        telefonoText = findViewById(R.id.numeroText);

        // Referencia al ImageView de la foto de perfil y al logo
        profileImage = findViewById(R.id.profileImage);
        logoImage = findViewById(R.id.logoImagen); // El ImageView con el logo

        // Recuperar el estado guardado si la actividad se reinicia (ej. rotación)
        if (savedInstanceState != null) {
            imageUri = savedInstanceState.getParcelable("imageUri");
            if (imageUri != null) {
                setImageToCircle(imageUri); // Restaurar la imagen
            }
        }

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
            imageUri = data.getData();
            setImageToCircle(imageUri); // Aquí se establece la imagen seleccionada en el ImageView
        }
    }

    // Método para recortar la imagen y convertirla en un círculo
    private void setImageToCircle(Uri imageUri) {
        try {
            // Obtener el InputStream de la URI de la imagen seleccionada
            InputStream imageStream = getContentResolver().openInputStream(imageUri);
            Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);

            // Crear un bitmap circular recortado
            Bitmap circularImage = getCircularBitmap(selectedImage);

            // Establecer la imagen circular al ImageView
            profileImage.setImageBitmap(circularImage); // Esto reemplaza la imagen de perfil

            // Ocultar el logo cuando se seleccione la imagen
            logoImage.setVisibility(View.GONE); // El logo desaparece

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Función para recortar una imagen en forma circular
    private Bitmap getCircularBitmap(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int radius = Math.min(width, height) / 2;

        // Crear una nueva imagen recortada circularmente
        Bitmap output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        // Dibujar el círculo en el canvas
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);
        paint.setDither(true);

        // Crear la forma circular
        canvas.drawCircle(width / 2, height / 2, radius, paint);

        // Recortar la imagen en el círculo
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, 0, 0, paint);

        return output;
    }

    // Guardar el estado (imagen seleccionada) antes de la rotación
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (imageUri != null) {
            outState.putParcelable("imageUri", imageUri); // Guardar la URI de la imagen
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
