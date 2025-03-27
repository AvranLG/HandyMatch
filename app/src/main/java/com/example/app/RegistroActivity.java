package com.example.app;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputLayout;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.io.InputStream;

public class RegistroActivity extends AppCompatActivity {

    private static final int PICK_IMAGE = 1;
    private ImageView profileImage;
    private ImageView logoImage;
    private Uri imageUri; // Guardar la URI de la imagen seleccionada

    private EditText nombreText, apellidosText, correoText, contrasenaText, telefonoText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registro);

        // Inicializar los EditText
        nombreText = findViewById(R.id.direccionText);
        apellidosText = findViewById(R.id.apellidosText);
        correoText = findViewById(R.id.correoText);
        contrasenaText = findViewById(R.id.passwordText);
        telefonoText = findViewById(R.id.numeroText);

        // Referencias de ImageView
        profileImage = findViewById(R.id.profileImage);
        logoImage = findViewById(R.id.logoImagen);

        // Restaurar imagen si la actividad se reinicia
        if (savedInstanceState != null) {
            imageUri = savedInstanceState.getParcelable("imageUri");
            if (imageUri != null) {
                setImageToCircle(imageUri);
            }
        }

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
            startUCrop(imageUri);
        }

        if (requestCode == UCrop.REQUEST_CROP && resultCode == RESULT_OK) {
            Uri resultUri = UCrop.getOutput(data);
            setImageToCircle(resultUri);
            imageUri = resultUri;
        }
    }

    private void startUCrop(Uri imageUri) {
        Uri destinationUri = Uri.fromFile(new File(getCacheDir(), "cropped_image.jpg"));

        UCrop.of(imageUri, destinationUri)
                .withAspectRatio(1, 1)
                .withMaxResultSize(800, 800)
                .withOptions(getUCropOptions())
                .start(this);
    }

    private UCrop.Options getUCropOptions() {
        UCrop.Options options = new UCrop.Options();
        options.setCircleDimmedLayer(true);
        options.setShowCropFrame(false);
        options.setShowCropGrid(true);
        return options;
    }

    private void setImageToCircle(Uri imageUri) {
        try {
            InputStream imageStream = getContentResolver().openInputStream(imageUri);
            Bitmap selectedImage = BitmapFactory.decodeStream(imageStream);
            Bitmap circularImage = getCircularBitmap(selectedImage);

            profileImage.setImageBitmap(circularImage);
            logoImage.setVisibility(View.GONE);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error al cargar la imagen", Toast.LENGTH_SHORT).show();
        }
    }

    private Bitmap getCircularBitmap(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int radius = Math.min(width, height) / 2;

        Bitmap output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);
        paint.setDither(true);

        canvas.drawCircle(width / 2, height / 2, radius, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, 0, 0, paint);

        return output;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (imageUri != null) {
            outState.putParcelable("imageUri", imageUri);
        }
    }

    public void abrirDireccion(View v) {

        boolean error = false;
        // Recoger los datos de los EditText
        String nombre = nombreText.getText().toString().trim();
        String apellidos = apellidosText.getText().toString().trim();
        String correo = correoText.getText().toString().trim();
        String contrasena = contrasenaText.getText().toString().trim();
        String telefono = telefonoText.getText().toString().trim();

        TextInputLayout nombreContainer = findViewById(R.id.nombreContainer);
        TextInputLayout apellidosContainer = findViewById(R.id.apellidosContainer);
        TextInputLayout emailContainer = findViewById(R.id.emailContainer);
        TextInputLayout pwdContainer = findViewById(R.id.pwdContainer);
        TextInputLayout numeroContainer = findViewById(R.id.numeroContainer);

        // Validar los datos antes de enviarlos
        if (nombre.isEmpty() || apellidos.isEmpty() || correo.isEmpty() || contrasena.isEmpty() || telefono.isEmpty()) {
            Toast.makeText(this, "Todos los campos son obligatorios", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validar el nombre (Debe comenzar con letra y permitir espacios)
        if (!nombre.matches("^[A-Za-zÁÉÍÓÚáéíóúÑñ][A-Za-zÁÉÍÓÚáéíóúÑñ ]*$")) {
            nombreContainer.setError("Debe iniciar y contener sólo letras");
            nombreContainer.setErrorEnabled(true); // Habilita el mensaje de error
            error = true;
        } else {
            // Si el nombre es válido, se elimina el error
            nombreContainer.setErrorEnabled(false);
        }

        // Validar los apellidos (Debe comenzar con letra y permitir espacios)
        if (!apellidos.matches("^[A-Za-zÁÉÍÓÚáéíóúÑñ][A-Za-zÁÉÍÓÚáéíóúÑñ ]*$")) {
            apellidosContainer.setError("Debe iniciar y contener sólo letras");
            apellidosContainer.setErrorEnabled(true); // Habilita el mensaje de error
            error = true;
        } else {
            // Si el apellido es válido, se elimina el error
            apellidosContainer.setErrorEnabled(false);
        }

        // Validar el correo electrónico
        if (!correo.matches("^([a-zA-Z][a-zA-Z0-9]*(?:[._-][a-zA-Z0-9]+)*)@(gmail\\.com|hotmail\\.com|yahoo\\.com|outlook\\.com|itocotlan\\.com|ocotlan\\.tecnm\\.mx)$"
        )) {
            emailContainer.setError("Formato inválido");
            emailContainer.setErrorEnabled(true); // Habilita el mensaje de error
            error = true;
        } else {
            // Si el email es válido, se elimina el error
            emailContainer.setErrorEnabled(false);
        }

        // Validar la contraseña (mínimo 8 caracteres, al menos una mayúscula, una minúscula y un número)
        if (contrasena.length() < 8 || !contrasena.matches(".*[A-Z].*") || !contrasena.matches(".*[a-z].*") || !contrasena.matches(".*\\d.*")) {
            pwdContainer.setError("Mínimo 8 caracteres, 1 mayúscula, 1 minúscula y 1 número");
            pwdContainer.setErrorEnabled(true); // Habilita el mensaje de error
            error = true;
        } else {
            // Si la contraseña es válida, se elimina el error
            pwdContainer.setErrorEnabled(false);
        }

        // Validar el teléfono (verificar que tenga exactamente 10 dígitos)
        if (telefono.length() != 10 || !telefono.matches("\\d{10}")) {
            numeroContainer.setError("Debe contener 10 dígitos");
            numeroContainer.setErrorEnabled(true); // Habilita el mensaje de error
            error = true;
        } else {
            // Si el numero es válido, se elimina el error
            numeroContainer.setErrorEnabled(false);
        }

        if(error) return;

        // Crear un Intent para abrir la segunda actividad (DireccionActivity)
        Intent i = new Intent(this, DireccionActivity.class);

        // Convertir la URI de la imagen en String
        String imagenUriString = (imageUri != null) ? imageUri.toString() : "";

        // Pasar los datos al Intent
        i.putExtra("nombre", nombre);
        i.putExtra("apellidos", apellidos);
        i.putExtra("correo", correo);
        i.putExtra("contrasena", contrasena);
        i.putExtra("telefono", telefono);
        i.putExtra("imagenUri", imagenUriString);  // Enviar la URI de la imagen

        // Abrir la segunda actividad
        startActivity(i);
    }


    public void abrirLogin(View v) {
        Intent i = new Intent(this, LoginActivity.class);
        startActivity(i);
    }
}
