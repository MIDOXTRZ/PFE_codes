package com.example.atcarremote;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class ConnectionActivity extends AppCompatActivity {

    private EditText editTextDeviceName, editTextIP, editTextPort;
    private Button buttonConnect;
    private ProgressBar progressBar;
    private TextView textViewStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connection);

        editTextDeviceName = findViewById(R.id.editTextDeviceName);
        editTextIP = findViewById(R.id.editTextIP);
        editTextPort = findViewById(R.id.editTextPort);
        buttonConnect = findViewById(R.id.buttonConnect);
        progressBar = findViewById(R.id.progressBar);
        textViewStatus = findViewById(R.id.textViewStatus);

        buttonConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connectToDevice();
            }
        });
    }

    private void connectToDevice() {
        final String deviceName = editTextDeviceName.getText().toString().trim();
        final String ip = editTextIP.getText().toString().trim();
        final String port = editTextPort.getText().toString().trim();

        if (deviceName.isEmpty() || ip.isEmpty() || port.isEmpty()) {
            Toast.makeText(this, "Veuillez remplir tous les champs !", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        textViewStatus.setText("Connexion en cours...");
        textViewStatus.setTextColor(getResources().getColor(android.R.color.holo_orange_light));
        buttonConnect.setEnabled(false);

        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean isConnected = checkConnection(ip, Integer.parseInt(port));

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setVisibility(View.GONE);
                        if (isConnected) {
                            textViewStatus.setText("Connexion réussie !");
                            textViewStatus.setTextColor(getResources().getColor(android.R.color.holo_green_light));
                            showConnectionInfoDialog(deviceName, ip, port);
                        } else {
                            textViewStatus.setText("Échec de la connexion !");
                            textViewStatus.setTextColor(getResources().getColor(android.R.color.holo_red_light));
                            Toast.makeText(ConnectionActivity.this, "Impossible de se connecter au Raspberry Pi !", Toast.LENGTH_SHORT).show();
                        }
                        buttonConnect.setEnabled(true);
                    }
                });
            }
        }).start();
    }

    private boolean checkConnection(String ip, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(ip, port), 2000);  // Timeout de 2 secondes
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private void showConnectionInfoDialog(String deviceName, String ip, String port) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Informations de connexion")
                .setMessage("Nom de l'appareil : " + deviceName + "\nIP : " + ip + "\nPort : " + port)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(ConnectionActivity.this, ModeSelectionActivity.class);
                        startActivity(intent);
                        finish();
                    }
                })
                .setCancelable(false)
                .show();
    }
}
