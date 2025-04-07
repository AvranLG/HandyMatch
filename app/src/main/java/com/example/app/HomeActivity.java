package com.example.app;

import android.os.Bundle;
import android.view.WindowManager;
import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class HomeActivity extends AppCompatActivity {

    private int lastSelectedIndex = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);

        // Para evitar que la barra de navegación se sobreponga al contenido
      //  getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
      //          WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        // Manejar el botón de retroceso con OnBackPressedDispatcher
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                mostrarDialogoSalida();
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Configurar la barra de navegación
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);

        // Aplicar colores de la configuración anterior
        bottomNavigationView.setItemIconTintList(ContextCompat.getColorStateList(this, R.color.fondosyletras));
        bottomNavigationView.setItemTextColor(ContextCompat.getColorStateList(this, R.color.fondosyletras));

        // Cargar fragment inicial (Home)
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment(), false);
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
            lastSelectedIndex = 0; // Inicializar con la posición del Home
        }

        // Configurar listener para la navegación
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            int currentIndex = getIndexFromItemId(id);

            // Determina la dirección basada en la posición del ítem en el menú
            boolean isGoingToRight = lastSelectedIndex != -1 && currentIndex > lastSelectedIndex;

            if (id == R.id.nav_home) {
                loadFragment(new HomeFragment(), isGoingToRight);
                lastSelectedIndex = currentIndex;
                return true;
            } else if (id == R.id.nav_lupa) {
                loadFragment(new BuscarFragment(), isGoingToRight);
                lastSelectedIndex = currentIndex;
                return true;
            } else if (id == R.id.nav_ubicacion) {
                // loadFragment(new UbicacionFragment(), isGoingToRight);
                lastSelectedIndex = currentIndex;
                return true;
            } else if (id == R.id.nav_perfil) {
                // loadFragment(new PerfilFragment(), isGoingToRight);
                lastSelectedIndex = currentIndex;
                return true;
            } else if (id == R.id.nav_mensaje) {
                // loadFragment(new MensajesFragment(), isGoingToRight);
                lastSelectedIndex = currentIndex;
                return true;
            }
            return false;
        });
    }

    /**
     * Obtiene el índice basado en el ID del ítem seleccionado
     * @param itemId ID del ítem del menú
     * @return índice que representa la posición del ítem en el menú
     */
    private int getIndexFromItemId(int itemId) {
        if (itemId == R.id.nav_home) return 0;
        if (itemId == R.id.nav_lupa) return 1;
        if (itemId == R.id.nav_ubicacion) return 2;
        if (itemId == R.id.nav_perfil) return 3;
        if (itemId == R.id.nav_mensaje) return 4;
        return -1;
    }

    /**
     * Carga un fragmento en el contenedor principal
     * @param fragment Fragmento a cargar
     */
    private void loadFragment(Fragment fragment, boolean isGoingToRight) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();

        // Aplica las animaciones de transición (deslizar)
        if (isGoingToRight) {
            // Si la navegación es hacia la derecha
            transaction.setCustomAnimations(
                    R.anim.slide_in_right,  // Fragmento que entra desde la izquierda
                    R.anim.slide_out_left   // Fragmento que sale hacia la derecha
            );
        } else {
            // Si la navegación es hacia la izquierda
            transaction.setCustomAnimations(
                    R.anim.slide_in_left,   // Fragmento que entra desde la derecha
                    R.anim.slide_out_right  // Fragmento que sale hacia la izquierda
            );
        }

        // Reemplazar el fragmento actual con el nuevo fragmento
        transaction.replace(R.id.fragmentContainer, fragment);
        transaction.commit();
    }

    // Sobrecarga del metodo para cargar el fragmento inicial sin animación
    private void loadFragment(Fragment fragment) {
        loadFragment(fragment, false);
    }

    private void mostrarDialogoSalida() {
        new AlertDialog.Builder(this)
                .setTitle("Salir de HandyMatch")
                .setMessage("¿Seguro que quieres salir de HandyMatch?")
                .setPositiveButton("Sí", (dialog, which) -> finishAffinity()) // Cierra la app
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss()) // Cancela el cierre
                .show();
    }
}