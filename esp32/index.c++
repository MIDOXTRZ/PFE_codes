#include <HardwareSerial.h>

HardwareSerial mySerial(1);  // UART1 pour la communication avec Raspberry Pi

// Moteurs
#define IN1 25
#define IN2 26
#define ENA 12
#define IN3 27
#define IN4 14
#define ENB 13

// Capteurs
#define LEFT_SENSOR 23
#define RIGHT_SENSOR 33

// États
bool isMoving = false;
bool isFollowingLine = false;
bool isStopped = false;

int speed_control = 255;
int baseSpeed = 120;
int correctionSpeed = 160;
const unsigned long stopDelay = 3000;
unsigned long stopStartTime = 0;

void setup() {
  mySerial.begin(115200, SERIAL_8N1, 16, 17); // RX:16, TX:17
  Serial.begin(115200);

  // Moteurs
  pinMode(IN1, OUTPUT);
  pinMode(IN2, OUTPUT);
  pinMode(IN3, OUTPUT);
  pinMode(IN4, OUTPUT);
  pinMode(ENA, OUTPUT);
  pinMode(ENB, OUTPUT);

  // Capteurs
  pinMode(LEFT_SENSOR, INPUT);
  pinMode(RIGHT_SENSOR, INPUT);

  Serial.println("ESP32 prêt à recevoir les commandes !");
}

void loop() {
  // Vérifier si le délai de STOP est terminé
  if (isStopped && (millis() - stopStartTime >= stopDelay)) {
    isStopped = false;
    Serial.println("Fin de la période d'arrêt");
  }

  // Vérifier si un message est reçu
  if (mySerial.available() && !isStopped) {
    String message = mySerial.readStringUntil('\n');
    message.trim();
    Serial.print("Commande reçue : ");
    Serial.println(message);

    // Analyser les commandes
    if (message == "GO") {
      startFollowingLine();
    } 
    else if (message == "PAUSE") {
      pauseMoving();
    } 
    else if (message == "STOP") {
      triggerStop();
    } 
    else if (message == "FORWARD") {
      forward(speed_control);
      isFollowingLine = false; // Quitte le mode suivi de ligne
    } 
    else if (message == "STOP_UP") {
      stopNow();
    } 
    else if (message == "BACKWARD") {
      backward(speed_control);
      isFollowingLine = false; // Quitte le mode suivi de ligne
    } 
    else if (message == "LEFT") {
      turnLeft(speed_control);
      isFollowingLine = false; // Quitte le mode suivi de ligne
    } 
    else if (message == "RIGHT") {
      turnRight(speed_control);
      isFollowingLine = false; // Quitte le mode suivi de ligne
    }
  }

  // Suivi de ligne si activé et pas en état STOP
  if (isFollowingLine && !isStopped) {
    followLine();
  }
}

// === Commandes principales ===

void startFollowingLine() {
  if (!isFollowingLine && !isStopped) {
    isFollowingLine = true;
    isMoving = true;
    Serial.println("Suivi de ligne ACTIVÉ");
  }
}

void stopFollowingLine() {
  isFollowingLine = false;
  stopMotors();
  Serial.println("Arrêt du suivi de ligne !");
}

void triggerStop() {
  stopMotors();
  isStopped = true;
  stopStartTime = millis();
  Serial.println("STOP pendant 3 secondes !");
}

void pauseMoving() {
  if (isMoving && !isStopped) {
    isFollowingLine = false;
    stopMotors();
    Serial.println("Suivi de ligne PAUSÉ");
  }
}

void stopNow() {
  stopMotors();
  Serial.println("Arrêt immédiat !");
}

// === Fonctions Moteurs ===

void motorControl(bool in1, bool in2, bool in3, bool in4, int speedA, int speedB) {
  digitalWrite(IN1, in1);
  digitalWrite(IN2, in2);
  digitalWrite(IN3, in3);
  digitalWrite(IN4, in4);
  analogWrite(ENA, speedA);
  analogWrite(ENB, speedB);
}

void stopMotors() {
  motorControl(LOW, LOW, LOW, LOW, 0, 0);
  isMoving = false;
}

void forward(int speed) {
  if (!isStopped) {
    motorControl(HIGH, LOW, HIGH, LOW, speed, speed);
    Serial.println("Avancer");
    isMoving = true;
  }
}

void backward(int speed) {
  if (!isStopped) {
    motorControl(LOW, HIGH, LOW, HIGH, speed, speed);
    Serial.println("Reculer");
    isMoving = true;
  }
}

void turnLeft(int speed) {
  if (!isStopped) {
    motorControl(LOW, HIGH, HIGH, LOW, speed, speed);
    Serial.println("Tourner à gauche");
    isMoving = true;
  }
}

void turnRight(int speed) {
  if (!isStopped) {
    motorControl(HIGH, LOW, LOW, HIGH, speed, speed);
    Serial.println("Tourner à droite");
    isMoving = true;
  }
}

// === Suivi de ligne ===

void followLine() {
  int left = digitalRead(LEFT_SENSOR);   // Lire la valeur du capteur gauche
  int right = digitalRead(RIGHT_SENSOR);  // Lire la valeur du capteur droit

  Serial.print("Capteurs | Gauche: ");
  Serial.print(left);
  Serial.print(" | Droite: ");
  Serial.println(right);

  // Logique modifiée selon vos spécifications
  if (left == LOW && right == LOW) {
    // Les deux capteurs voient du blanc (pas de ligne) -> Avancer
    forward(baseSpeed);
    Serial.println("Pas de ligne (AVANCE)");
  }
  else if (left == HIGH && right == HIGH) {
    // Les deux capteurs voient du noir -> Reculer
    stopMotors();
    Serial.println("Ligne détectée (RECULE)");
  }
  else if (left == HIGH && right == LOW) {
    // Seul le capteur gauche voit du noir -> Tourner à gauche
    turnRight(correctionSpeed);
    Serial.println("Tourne à GAUCHE");
  }
  else if (left == LOW && right == HIGH) {
    // Seul le capteur droit voit du noir -> Tourner à droite
    turnLeft(correctionSpeed);
    Serial.println("Tourne à DROITE");
  }

  delay(50); // Délai réduit pour une meilleure réactivité
}