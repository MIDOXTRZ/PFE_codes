package com.example.atcarremote;

import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

public class TelecommandeControlActivity extends AppCompatActivity {

    private static final String IP_ADDRESS = "192.168.43.61"; // Adresse IP du serveur
    private static final int PORT = 12345; // Port de communication pour le contrôle

    private WebView cameraPreview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_telecommande);

        // Initialisation du WebView pour afficher le flux vidéo
        cameraPreview = findViewById(R.id.vediostream);
        WebSettings webSettings = cameraPreview.getSettings();
        webSettings.setJavaScriptEnabled(true);  // Assurez-vous que JavaScript est activé si nécessaire
        webSettings.setPluginState(WebSettings.PluginState.ON);  // Activer les plugins pour la gestion de vidéos

        cameraPreview.setWebViewClient(new WebViewClient());  // Utilisation de WebViewClient pour gérer les liens
        cameraPreview.loadUrl("http://192.168.43.61:5000/video_feed"); // URL du flux vidéo

        // Contrôles pour la télécommande (mouvement)
        ImageView imageViewUp = findViewById(R.id.imageViewUp);
        ImageView imageViewLeft = findViewById(R.id.imageViewLeft);
        ImageView imageViewRight = findViewById(R.id.imageViewRight);
        ImageView imageViewDown = findViewById(R.id.imageViewDown);

        // Gestion des événements tactiles pour les boutons de contrôle
        imageViewUp.setOnTouchListener(getTouchListener("FORWARD"));
        imageViewLeft.setOnTouchListener(getTouchListener("LEFT"));
        imageViewRight.setOnTouchListener(getTouchListener("RIGHT"));
        imageViewDown.setOnTouchListener(getTouchListener("BACKWARD"));
    }

    // Méthode pour créer un listener pour chaque bouton de contrôle
    private View.OnTouchListener getTouchListener(final String command) {
        return (v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                sendCommand(command);  // Envoyer la commande quand l'utilisateur appuie
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                sendCommand("STOP");  // Arrêter quand l'utilisateur relâche le bouton
            }
            return true;
        };
    }

    // Méthode pour envoyer une commande au serveur via un socket
    private void sendCommand(String command) {
        new Thread(() -> {
            try {
                // Connexion au serveur via un socket TCP/IP
                Socket socket = new Socket();
                socket.connect(new InetSocketAddress(IP_ADDRESS, PORT), 2000);  // Connexion à l'IP et au port spécifiés
                OutputStream out = socket.getOutputStream();
                out.write(command.getBytes());  // Envoi de la commande
                out.flush();
                socket.close();  // Fermeture de la connexion
            } catch (Exception e) {
                e.printStackTrace();
                // Affichage d'un message d'erreur en cas d'échec de la connexion
                runOnUiThread(() -> Toast.makeText(this, "Erreur de connexion", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}
