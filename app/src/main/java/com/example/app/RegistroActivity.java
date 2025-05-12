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
import com.google.firebase.auth.FirebaseAuth;
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

    // Variables para verificación de identidad
    private String extractedBirthDate = null;
    private boolean identityVerified = false;

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
            extractedBirthDate = savedInstanceState.getString("extractedBirthDate");
            identityVerified = savedInstanceState.getBoolean("identityVerified", false);
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

            // Verificar coherencia con fecha extraída de la identificación
            if (extractedBirthDate != null) {
                verifyBirthDateCoherence(selectedDate);
            }
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
        extractedBirthDate = null;
        identityVerified = false;
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

                if(analizandoIne){
                    UCrop.of(sourceUri, Uri.fromFile(new File(getCacheDir(), "cropped_" + System.currentTimeMillis() + ".jpg")))
                            .withAspectRatio(1, 1)
                            .withMaxResultSize(800, 800)
                            .withOptions(getCropOptions2())
                            .start(this);
                } else{
                    UCrop.of(sourceUri, Uri.fromFile(new File(getCacheDir(), "cropped_" + System.currentTimeMillis() + ".jpg")))
                            .withAspectRatio(1, 1)
                            .withMaxResultSize(800, 800)
                            .withOptions(getCropOptions())
                            .start(this);
                }

    }

    //Este es para la imagen de perfil
    private UCrop.Options getCropOptions() {
        UCrop.Options options = new UCrop.Options();
        options.setCircleDimmedLayer(true);
        options.setShowCropFrame(false);
        options.setShowCropGrid(false);
        options.setHideBottomControls(true);
        options.setCompressionQuality(80);
        return options;
    }

    //Este es para la INE
    private UCrop.Options getCropOptions2() {
        UCrop.Options options = new UCrop.Options();
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
        String text = result.getText().toUpperCase();
        Log.d(TAG, "Texto OCR: " + text);

        // Patrones para validación de documentos
        String claveElectorPattern = ".*[A-Z]{4}[0-9]{6}[A-Z]{6}[0-9A-Z]{2}.*";
        String curpPattern = ".*[A-Z]{4}[0-9]{6}[A-Z]{6}[0-9A-Z]{2}.*"; // O el patrón específico de CURP

        // Verifica si el texto contiene Clave de Elector o CURP
        boolean hasClaveElector = text.matches(claveElectorPattern) || text.contains("CLAVE DE ELECTOR");
        boolean hasCURP = text.matches(curpPattern) || text.contains("CURP");

        // Si contiene al menos uno de los dos documentos válidos
        if (hasClaveElector || hasCURP) {
            identityVerified = true;
            extractBirthDateFromID(text);

            // Si ya hay una fecha de nacimiento ingresada, verificar coherencia
            if (!fechaNacimientoText.getText().toString().isEmpty() && extractedBirthDate != null) {
                verifyBirthDateCoherence(fechaNacimientoText.getText().toString());
            } else if (extractedBirthDate != null) {
                // Si se extrajo la fecha pero el usuario no ha ingresado una, sugerir la fecha extraída
                suggestBirthDate();
            }

            showToast("✅ Identificación válida detectada");
        } else {
            showError("❌ No se detectó una identificación válida");
            removeIdentityImage();
        }
    }

    private void extractBirthDateFromID(String text) {
        extractedBirthDate = null;

        // Intentar extraer la fecha de la CURP
        Pattern curpPattern = Pattern.compile("[A-Z]{4}(\\d{2})(\\d{2})(\\d{2})[HM][A-Z]{5}[0-9A-Z]{2}");
        Matcher curpMatcher = curpPattern.matcher(text);

        if (curpMatcher.find()) {
            try {
                int year = Integer.parseInt(curpMatcher.group(1));
                int month = Integer.parseInt(curpMatcher.group(2));
                int day = Integer.parseInt(curpMatcher.group(3));

                // Ajustar año (considerar si es 1900s o 2000s)
                year = (year > 25) ? 1900 + year : 2000 + year; // Ajusta según tus necesidades

                // Formatear a DD/MM/YYYY
                extractedBirthDate = String.format(Locale.getDefault(), "%02d/%02d/%04d", day, month, year);
                Log.d(TAG, "Fecha extraída de CURP: " + extractedBirthDate);
                return;
            } catch (Exception e) {
                Log.e(TAG, "Error al extraer fecha de CURP", e);
            }
        }

        // Intentar buscar fecha directamente en formato común (DD/MM/YYYY o YYYY/MM/DD)
        Pattern directDatePattern = Pattern.compile("(\\d{1,2})/(\\d{1,2})/(\\d{2,4})");
        Matcher directDateMatcher = directDatePattern.matcher(text);

        if (directDateMatcher.find()) {
            extractedBirthDate = directDateMatcher.group();
            Log.d(TAG, "Fecha encontrada directamente: " + extractedBirthDate);

            // Normalizar formato si es necesario
            try {
                SimpleDateFormat inputFormat;
                if (extractedBirthDate.matches("\\d{2}/\\d{2}/\\d{2}")) {
                    inputFormat = new SimpleDateFormat("dd/MM/yy", Locale.getDefault());
                } else {
                    inputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                }

                Date date = inputFormat.parse(extractedBirthDate);
                SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                extractedBirthDate = outputFormat.format(date);
            } catch (ParseException e) {
                Log.e(TAG, "Error al formatear fecha", e);
            }
        }
    }

    private void suggestBirthDate() {
        if (extractedBirthDate == null) return;

        new AlertDialog.Builder(this)
                .setTitle("Fecha de Nacimiento Detectada")
                .setMessage("Se ha detectado la fecha de nacimiento: " + extractedBirthDate + "\n¿Desea utilizarla?")
                .setPositiveButton("Sí", (dialog, which) -> {
                    fechaNacimientoText.setText(extractedBirthDate);
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void verifyBirthDateCoherence(String userEnteredDate) {
        if (extractedBirthDate == null || userEnteredDate == null) return;

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date userDate = sdf.parse(userEnteredDate);
            Date idDate = sdf.parse(extractedBirthDate);

            // Permitir un margen de error de un día
            long diffMillis = Math.abs(userDate.getTime() - idDate.getTime());
            long diffDays = diffMillis / (24 * 60 * 60 * 1000);

            if (diffDays > 1) {
                // Las fechas no coinciden
                new AlertDialog.Builder(this)
                        .setTitle("Advertencia")
                        .setMessage("La fecha ingresada (" + userEnteredDate +
                                ") no coincide con la fecha detectada en su identificación (" +
                                extractedBirthDate + ").\n\n¿Desea utilizar la fecha de la identificación?")
                        .setPositiveButton("Sí", (dialog, which) -> {
                            fechaNacimientoText.setText(extractedBirthDate);
                        })
                        .setNegativeButton("No", (dialog, which) -> {
                            showToast("Verifique su fecha de nacimiento");
                        })
                        .show();
            }
        } catch (ParseException e) {
            Log.e(TAG, "Error al comparar fechas", e);
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

        // Verificación de coherencia entre fecha ingresada y la de identificación
        if (isValid && extractedBirthDate != null && !fechaNacimientoText.getText().toString().isEmpty()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                Date userDate = sdf.parse(fechaNacimientoText.getText().toString());
                Date idDate = sdf.parse(extractedBirthDate);

                long diffMillis = Math.abs(userDate.getTime() - idDate.getTime());
                long diffDays = diffMillis / (24 * 60 * 60 * 1000);

                if (diffDays > 1) {
                    new AlertDialog.Builder(this)
                            .setTitle("Verificación de Identidad")
                            .setMessage("Los datos no coinciden.\n" +
                                    "Por favor verifique su información antes de continuar.")
                            .setPositiveButton("Entendido", null)
                            .show();
                    return false;
                }
            } catch (ParseException e) {
                Log.e(TAG, "Error al comparar fechas en validación", e);
            }
        }

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
        showProgress("Creando cuenta...");

        String email = emailText.getText().toString().trim();
        String password = contrasenaText.getText().toString().trim();

        FirebaseAuth.getInstance()
                .createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Usuario autenticado correctamente
                        FirebaseAuth auth = FirebaseAuth.getInstance();
                        String uid = auth.getCurrentUser().getUid();

                        // Guardar los datos en la base de datos
                        guardarDatosEnFirebase(uid);

                    } else {
                        dismissProgress();
                        showError("Error al crear cuenta: " + task.getException().getMessage());
                        Log.e(TAG, "FirebaseAuth error", task.getException());
                    }
                });
    }

    private void guardarDatosEnFirebase(String uid) {
        Map<String, Object> datosUsuario = new HashMap<>();
        datosUsuario.put("nombre", nombreText.getText().toString().trim());
        datosUsuario.put("apellidos", apellidosText.getText().toString().trim());
        datosUsuario.put("email", emailText.getText().toString().trim());
        datosUsuario.put("contrasena", hashPassword(contrasenaText.getText().toString().trim()));
        datosUsuario.put("telefono", telefonoText.getText().toString().trim());
        datosUsuario.put("fechaNacimiento", fechaNacimientoText.getText().toString().trim());
        datosUsuario.put("verificado", Boolean.TRUE.equals(identityVerified));

        if (imageUri != null) datosUsuario.put("imagenUri", imageUri.toString());
        if (ineUri != null) datosUsuario.put("ineUri", ineUri.toString());

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("usuarios").child(uid);

        ref.setValue(datosUsuario)
                .addOnSuccessListener(unused -> {
                    dismissProgress();
                    // Solo pasamos a la siguiente actividad - ya no necesitamos pasar todos los datos
                    // porque están guardados en Firebase y podemos recuperarlos por el UID
                    Intent intent = new Intent(RegistroActivity.this, DireccionActivity.class);
                    startActivity(intent);
                    finish(); // Cerrar esta actividad para evitar volver atrás
                })
                .addOnFailureListener(e -> {
                    dismissProgress();
                    showError("Error al guardar usuario: " + e.getMessage());
                    Log.e(TAG, "Error al guardar en Firebase", e);
                });
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
        outState.putString("extractedBirthDate", extractedBirthDate);
        outState.putBoolean("identityVerified", identityVerified);
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