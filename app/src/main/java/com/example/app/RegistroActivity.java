package com.example.app;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.yalantis.ucrop.UCrop;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

public class RegistroActivity extends AppCompatActivity {

    private static final int PICK_IMAGE = 1;
    private static final int MIN_AGE = 18;
    private static final String TAG = "RegistroActivity";

    // Vistas
    private ImageView profileImage, identityImage;
    private EditText nombreText, apellidosText, emailText, contrasenaText, telefonoText, fechaNacimientoText;
    private TextInputLayout[] inputLayouts;

    // Variables de estado
    private Uri imageUri, ineUri;
    private boolean analizandoIne = false;
    private AlertDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registro);
        initViews();
        setupRestoredState(savedInstanceState);
        setupListeners();

        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.setStatusBarColor(Color.parseColor("#FFFFFF"));
    }

    private void initViews() {
        profileImage = findViewById(R.id.profileImage);
        identityImage = findViewById(R.id.identityImage);

        nombreText = findViewById(R.id.direccionText);
        apellidosText = findViewById(R.id.apellidosText);
        emailText = findViewById(R.id.tituloText);
        contrasenaText = findViewById(R.id.passwordText);
        telefonoText = findViewById(R.id.numeroText);
        fechaNacimientoText = findViewById(R.id.fechaNacimientoText);

        inputLayouts = new TextInputLayout[]{
                findViewById(R.id.nombreContainer),
                findViewById(R.id.apellidosContainer),
                findViewById(R.id.emailContainer),
                findViewById(R.id.pwdContainer),
                findViewById(R.id.numeroContainer)
        };
    }

    private void setupRestoredState(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            imageUri = savedInstanceState.getParcelable("imageUri");
            ineUri = savedInstanceState.getParcelable("ineUri");
            restoreImageState();
        }
    }

    private void restoreImageState() {
        try {
            if (imageUri != null) {
                setCircularImage(imageUri);
                findViewById(R.id.logoImagen).setVisibility(View.GONE);
            }
            if (ineUri != null) {
                identityImage.setImageURI(ineUri);
                identityImage.setVisibility(View.VISIBLE);
                findViewById(R.id.identityPlaceholder).setVisibility(View.GONE);
                findViewById(R.id.btnRemoveIdentity).setEnabled(true);
            }
        } catch (IOException e) {
            Log.e(TAG, "Error restoring images", e);
        }
    }

    private void setupListeners() {
        // DatePicker
        fechaNacimientoText.setOnClickListener(v -> showDatePicker());

        // Image handlers
        profileImage.setOnClickListener(v -> openGallery(false));
        findViewById(R.id.btnSeleccionarIne).setOnClickListener(v -> openGallery(true));
        findViewById(R.id.btnRemoveIdentity).setOnClickListener(v -> removeIdentityImage());

        // Botón de registro
        findViewById(R.id.btnregistro).setOnClickListener(v -> validateAndRegister());
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) -> {
            String selectedDate = String.format("%02d/%02d/%04d", day, month + 1, year);
            fechaNacimientoText.setText(selectedDate);
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void openGallery(boolean isForIne) {
        analizandoIne = isForIne;
        startActivityForResult(new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI), PICK_IMAGE);
    }

    private void removeIdentityImage() {
        identityImage.setImageURI(null);
        identityImage.setVisibility(View.GONE);
        findViewById(R.id.identityPlaceholder).setVisibility(View.VISIBLE);
        findViewById(R.id.btnRemoveIdentity).setEnabled(false);
        ineUri = null;
        showToast("Identificación removida");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == PICK_IMAGE) handleImageSelection(data.getData());
            else if (requestCode == UCrop.REQUEST_CROP) handleCroppedImage(UCrop.getOutput(data));
        }
    }

    private void handleImageSelection(Uri sourceUri) {
        UCrop.of(sourceUri, Uri.fromFile(new File(getCacheDir(), "cropped_" + System.currentTimeMillis() + ".jpg")))
                .withAspectRatio(1, 1)
                .withMaxResultSize(800, 800)
                .withOptions(getCropOptions())
                .start(this);
    }

    private UCrop.Options getCropOptions() {
        UCrop.Options options = new UCrop.Options();
        options.setCircleDimmedLayer(true);
        options.setShowCropFrame(false);
        options.setShowCropGrid(false);
        options.setHideBottomControls(true);
        options.setCompressionQuality(80);
        return options;
    }

    private void handleCroppedImage(Uri resultUri) {
        if (resultUri == null) return;

        try {
            if (analizandoIne) {
                handleINEImage(resultUri);
            } else {
                handleProfileImage(resultUri);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing image", e);
            showError("Error al procesar imagen");
        }
    }

    private void handleINEImage(Uri resultUri) {
        ineUri = resultUri;
        identityImage.setImageURI(resultUri);
        identityImage.setVisibility(View.VISIBLE);
        findViewById(R.id.identityPlaceholder).setVisibility(View.GONE);
        findViewById(R.id.btnRemoveIdentity).setEnabled(true);
        analyzeINE(resultUri);
    }

    private void handleProfileImage(Uri resultUri) throws IOException {
        imageUri = resultUri;
        setCircularImage(resultUri);
        findViewById(R.id.logoImagen).setVisibility(View.GONE);
    }

    private void setCircularImage(Uri imageUri) throws IOException {
        try (InputStream imageStream = getContentResolver().openInputStream(imageUri)) {
            Bitmap bitmap = BitmapFactory.decodeStream(imageStream);
            profileImage.setImageBitmap(createCircularBitmap(bitmap));
        }
    }

    private Bitmap createCircularBitmap(Bitmap bitmap) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);
        paint.setDither(true);

        canvas.drawARGB(0, 0, 0, 0);
        canvas.drawCircle(bitmap.getWidth() / 2f, bitmap.getHeight() / 2f,
                Math.min(bitmap.getWidth(), bitmap.getHeight()) / 2f, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, 0, 0, paint);

        return output;
    }

    private void analyzeINE(Uri imageUri) {
        showProgress("Analizando identificación...");

        try {
            TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                    .process(InputImage.fromFilePath(this, imageUri))
                    .addOnSuccessListener(this::handleOCRSuccess)
                    .addOnFailureListener(this::handleOCRFailure);
        } catch (IOException e) {
            dismissProgress();
            showError("Error al leer la imagen");
        }
    }

    private void handleOCRSuccess(Text result) {
        dismissProgress();
        String text = result.getText().toUpperCase(); // El texto extraído de la imagen.
        Log.d(TAG, "Texto OCR: " + text);

        // Verifica si el texto extraído corresponde a un formato válido de INE.
        boolean valid = text.matches(".*[A-Z]{4}[0-9]{6}[A-Z]{6}[0-9A-Z]{2}.*") ||
                text.matches(".*[A-Z]{6}[0-9]{8}[A-Z]{1}.*") ||
                text.contains("CLAVE DE ELECTOR");

        // Si el INE es válido
        if (valid) {
            showToast("✅ Identificación válida detectada");
        } else {
            // Si el INE no es válido
            showError("❌ No se detectó una identificación válida");
            removeIdentityImage();  // Remueve la imagen de la identificación
        }
    }


    private void handleOCRFailure(Exception e) {
        dismissProgress();
        showError("Error al analizar identificación");
        Log.e(TAG, "OCR Error", e);
        removeIdentityImage();
    }

    private void validateAndRegister() {
        if (!validateForm()) return;
        if (!checkNetwork()) return;

        showProgress("Verificando datos...");
        checkExistingUsers(emailText.getText().toString().trim(),
                telefonoText.getText().toString().trim());
    }

    private boolean validateForm() {
        boolean isValid = true;
        clearErrors();

        // Validación de campos requeridos
        isValid &= validateField(nombreText, R.id.nombreContainer, "Nombre requerido");
        isValid &= validateField(apellidosText, R.id.apellidosContainer, "Apellidos requeridos");
        isValid &= validateField(emailText, R.id.emailContainer, "Correo requerido");
        isValid &= validateField(contrasenaText, R.id.pwdContainer, "Contraseña requerida");
        isValid &= validateField(telefonoText, R.id.numeroContainer, "Teléfono requerido");

        // Validación de formato de correo
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(emailText.getText()).matches()) {
            setError(R.id.emailContainer, "Formato de correo inválido");
            isValid = false;
        }

        // Validación de contraseña
        if (contrasenaText.getText().length() < 8 ||
                !contrasenaText.getText().toString().matches("^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d).+$")) {
            setError(R.id.pwdContainer, "Mínimo 8 caracteres, 1 mayúscula, 1 minúscula y 1 número");
            isValid = false;
        }

        // Validación de fecha
        if (!validateBirthDate()) isValid = false;

        return isValid;
    }

    private boolean validateBirthDate() {
        try {
            Date birthDate = new SimpleDateFormat("dd/MM/yyyy").parse(fechaNacimientoText.getText().toString());
            Calendar minAgeDate = Calendar.getInstance();
            minAgeDate.add(Calendar.YEAR, -MIN_AGE);

            if (birthDate.after(minAgeDate.getTime())) {
                showError("Debes ser mayor de edad");
                return false;
            }
            return true;
        } catch (Exception e) {
            showError("Fecha inválida");
            return false;
        }
    }

    private void checkExistingUsers(String email, String phone) {
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("usuarios");
        AtomicBoolean emailExists = new AtomicBoolean(false);
        AtomicBoolean phoneExists = new AtomicBoolean(false);
        AtomicBoolean checksCompleted = new AtomicBoolean(false);

        // Verificar email
        dbRef.orderByChild("email").equalTo(email).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                emailExists.set(snapshot.exists());
                if (snapshot.exists()) setError(R.id.emailContainer, "Correo ya registrado");
                completeVerification(emailExists, phoneExists, checksCompleted);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                handleDatabaseError("correo", error);
            }
        });

        // Verificar teléfono
        dbRef.orderByChild("telefono").equalTo(phone).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                phoneExists.set(snapshot.exists());
                if (snapshot.exists()) setError(R.id.numeroContainer, "Teléfono ya registrado");
                completeVerification(emailExists, phoneExists, checksCompleted);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                handleDatabaseError("teléfono", error);
            }
        });
    }

    private void completeVerification(AtomicBoolean emailExists, AtomicBoolean phoneExists, AtomicBoolean completed) {
        if (!completed.getAndSet(true)) return;
        dismissProgress();

        if (!emailExists.get() && !phoneExists.get()) {
            proceedWithRegistration();
        } else {
            showError(emailExists.get() && phoneExists.get() ?
                    "Correo y teléfono ya registrados" :
                    emailExists.get() ? "Correo ya registrado" : "Teléfono ya registrado");
        }
    }

    private void proceedWithRegistration() {
        Bundle data = new Bundle();
        data.putString("nombre", nombreText.getText().toString().trim());
        data.putString("apellidos", apellidosText.getText().toString().trim());
        data.putString("correo", emailText.getText().toString().trim());
        data.putString("contrasena", hashPassword(contrasenaText.getText().toString().trim()));
        data.putString("telefono", telefonoText.getText().toString().trim());
        data.putString("fechaNacimiento", fechaNacimientoText.getText().toString().trim());

        if (imageUri != null) data.putString("imagenUri", imageUri.toString());
        if (ineUri != null) data.putString("ineUri", ineUri.toString());

        startActivity(new Intent(this, DireccionActivity.class).putExtras(data));
    }

    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Base64.encodeToString(digest.digest(password.getBytes(StandardCharsets.UTF_8)), Base64.NO_WRAP);
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "Error hashing password", e);
            return null;
        }
    }

    // Helpers de UI
    private void showProgress(String message) {
        dismissProgress();
        progressDialog = new AlertDialog.Builder(this)
                .setMessage(message)
                .setCancelable(false)
                .show();
    }

    private void dismissProgress() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    private void clearErrors() {
        for (TextInputLayout layout : inputLayouts) layout.setError(null);
    }

    private boolean validateField(EditText field, int layoutId, String error) {
        if (field.getText().toString().trim().isEmpty()) {
            setError(layoutId, error);
            return false;
        }
        return true;
    }

    private void setError(int layoutId, String error) {
        ((TextInputLayout) findViewById(layoutId)).setError(error);
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private boolean checkNetwork() {
        NetworkInfo activeNetwork = ((ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        if (activeNetwork == null || !activeNetwork.isConnected()) {
            showError("No hay conexión a internet");
            return false;
        }
        return true;
    }

    private void handleDatabaseError(String field, DatabaseError error) {
        dismissProgress();
        showError("Error verificando " + field);
        Log.e(TAG, "Database error (" + field + "): " + error.getMessage(), error.toException());
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("imageUri", imageUri);
        outState.putParcelable("ineUri", ineUri);
    }

    @Override
    protected void onDestroy() {
        dismissProgress();
        super.onDestroy();
    }

    public void abrirLogin(View v) {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }
}