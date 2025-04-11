import cv2
import socket
import threading
import serial
import time
import numpy as np
import pytesseract
from picamera2 import Picamera2
from flask import Flask, Response

app = Flask(__name__)
picam2 = Picamera2()
picam2.configure(picam2.create_video_configuration(main={"size": (220, 200)}))
picam2.start()
time.sleep(2)
try:
    uart = serial.Serial('/dev/ttyS0', baudrate=115200, timeout=1)
    print("UART ready")
except:
    uart = None
camera_state = "STOPPED"
stop_detected = False
flask_active = False
autonomous_mode = False
flask_thread = None
detection_active = False
detection_thread = None
words_to_detect = ["S", "T", "O", "P", "ST", "STOP"]


def correct_color_inversion(frame):
    corrected = frame.copy()
    corrected[:, :, 0] = frame[:, :, 2]
    corrected[:, :, 2] = frame[:, :, 0]
    return corrected


def extract_red_regions(frame):
    frame = correct_color_inversion(frame)
    hsv = cv2.cvtColor(frame, cv2.COLOR_BGR2HSV)
    lower_red1 = np.array([0, 120, 70])
    upper_red1 = np.array([10, 255, 255])
    lower_red2 = np.array([170, 120, 70])
    upper_red2 = np.array([180, 255, 255])
    mask1 = cv2.inRange(hsv, lower_red1, upper_red1)
    mask2 = cv2.inRange(hsv, lower_red2, upper_red2)
    mask = cv2.bitwise_or(mask1, mask2)
    kernel = np.ones((7, 7), np.uint8)
    mask = cv2.morphologyEx(mask, cv2.MORPH_OPEN, kernel)
    mask = cv2.morphologyEx(mask, cv2.MORPH_CLOSE, kernel)
    return mask


def detect_text_in_roi(roi):
    try:
        gray = cv2.cvtColor(roi, cv2.COLOR_BGR2GRAY)
        processed = cv2.threshold(gray, 0, 255,
                                  cv2.THRESH_BINARY | cv2.THRESH_OTSU)[1]
        custom_config = r'--oem 3 --psm 7 -c tessedit_char_whitelist=ABCDEFGHIJKLMNOPQRSTUVWXYZ'
        text = pytesseract.image_to_string(processed,
                                           config=custom_config).strip()
        print(f"Text detected: {text}")
        cv2.imwrite("/tmp/last_roi.jpg", roi)
        return any(word.upper() in text.upper() for word in words_to_detect)
    except Exception as e:
        print(f"OCR error: {e}")
        return False


def detect_stop_sign():
    global stop_detected, detection_active, autonomous_mode
    detection_active = True
    stop_detected = False
    print("Detection started")
    cv2.destroyAllWindows()
    while detection_active:
        if not autonomous_mode:
            print("Autonomous mode off, stopping detection.")
            break
        try:
            frame = picam2.capture_array()
        except Exception as e:
            print(f"Camera capture error: {e}")
            continue
        try:
            red_mask = extract_red_regions(frame)
            red_pixels = cv2.countNonZero(red_mask)
            print(f"Red pixels: {red_pixels}")
            if detection_active:
                cv2.imshow("Camera", frame)
                cv2.imshow("Red Mask", red_mask)
            if red_pixels > 1000:
                contours, _ = cv2.findContours(red_mask, cv2.RETR_EXTERNAL,
                                               cv2.CHAIN_APPROX_SIMPLE)
                if contours:
                    largest = max(contours, key=cv2.contourArea)
                    x, y, w, h = cv2.boundingRect(largest)
                    roi = frame[y:y + h, x:x + w]
                    if detect_text_in_roi(roi) and not stop_detected:
                        print("STOP detected - sending command")
                        if uart:
                            uart.write(b"STOP\n")
                        stop_detected = True
                        debug_frame = frame.copy()
                        cv2.rectangle(debug_frame, (x, y), (x + w, y + h),
                                      (0, 255, 0), 2)
                        cv2.putText(debug_frame, "STOP", (x, y - 10),
                                    cv2.FONT_HERSHEY_SIMPLEX, 0.9, (0, 255, 0),
                                    2)
                        cv2.imshow("Detection", debug_frame)
            elif stop_detected:
                print("Sending GO")
                if uart:
                    uart.write(b"GO\n")
                stop_detected = False
        except Exception as e:
            print(f"Processing error: {e}")
        if not detection_active:
            print("Detection flag turned off, exiting.")
            break
        if cv2.waitKey(1) & 0xFF == ord('q'):
            break
        time.sleep(0.1)
    cv2.destroyAllWindows()
    detection_active = False
    print("Detection stopped")


def run_flask():
    global flask_active
    flask_active = True
    print("Flask running")
    app.run(host='0.0.0.0', port=5000, threaded=True, use_reloader=False)


def generate_frames():
    while flask_active:
        frame = picam2.capture_array()
        _, buffer = cv2.imencode('.jpg', frame)
        yield (b'--frame\r\nContent-Type: image/jpeg\r\n\r\n' +
               buffer.tobytes() + b'\r\n')


@app.route('/video_feed')
def video_feed():
    if not flask_active:
        return "Inactive", 403
    return Response(generate_frames(),
                    mimetype='multipart/x-mixed-replace; boundary=frame')


def detection_main_loop():
    global camera_state, stop_detected, picam2
    print("Thread de dtection principal dmarr")
    while True:
        if camera_state == "STOPPED":
            time.sleep(0.5)
            continue
        if camera_state == "PAUSED":
            time.sleep(0.5)
            continue
        try:
            frame = picam2.capture_array()
            if autonomous_mode and camera_state == "RUNNING":
                red_mask = extract_red_regions(frame)
                red_pixels = cv2.countNonZero(red_mask)
                if red_pixels > 1000:
                    print(f"Red pixels: {red_pixels}")
                    contours, _ = cv2.findContours(red_mask, cv2.RETR_EXTERNAL,
                                                   cv2.CHAIN_APPROX_SIMPLE)
                    if contours:
                        largest = max(contours, key=cv2.contourArea)
                        x, y, w, h = cv2.boundingRect(largest)
                        roi = frame[y:y + h, x:x + w]
                        cv2.imwrite("/tmp/latest_roi.jpg", roi)
                        text_detected = detect_text_in_roi(roi)
                        if text_detected and not stop_detected:
                            print("STOP detect - envoi de la commande")
                            if uart:
                                uart.write(b"STOP\n")
                            stop_detected = True
                elif stop_detected and red_pixels < 500:
                    print("Plus de panneau STOP detecte - Envoi de GO")
                    if uart:
                        uart.write(b"GO\n")
                    stop_detected = False
        except Exception as e:
            print(f"Erreur dans la boucle de dtection: {e}")
        time.sleep(0.1)


detection_main_thread = threading.Thread(target=detection_main_loop,
                                         daemon=True)
detection_main_thread.start()


def handle_client(client_socket):
    global camera_state, autonomous_mode, flask_active, flask_thread
    data = client_socket.recv(1024).decode('utf-8').strip()
    print(f"Command: {data}")
    if data == "Mode Telecommande":
        autonomous_mode = False
        camera_state = "STOPPED"
        if not flask_active:
            flask_active = True
            flask_thread = threading.Thread(target=run_flask)
            flask_thread.daemon = True
            flask_thread.start()
        client_socket.send(b"REMOTE_ACTIVE")
    elif data == "Mode Autonome":
        autonomous_mode = True
        flask_active = False
        client_socket.send(b"AUTONOMOUS_ACTIVE")
    elif data == "GO":
        if autonomous_mode:
            camera_state = "RUNNING"
            if uart:
                uart.write(b"GO\n")
        client_socket.send(b"SUIVIE_LIGNE")
    elif data == "PAUSE":
        camera_state = "PAUSED"
        if uart:
            uart.write(b"PAUSE\n")
    elif data == "STOP":
        if uart: uart.write(b"STOP_UP\n")
        client_socket.send(b"STOP_ACTIVE")
    elif data == "FORWARD":
        if uart: uart.write(b"FORWARD\n")
        client_socket.send(b"FORWARD_ACTIVE")
    elif data == "BACKWARD":
        if uart: uart.write(b"BACKWARD\n")
        client_socket.send(b"BACKWARD_ACTIVE")
    elif data == "LEFT":
        if uart: uart.write(b"LEFT\n")
        client_socket.send(b"LEFT_ACTIVE")
    elif data == "RIGHT":
        if uart: uart.write(b"RIGHT\n")
        client_socket.send(b"RIGHT_ACTIVE")
    client_socket.close()


def tcp_server():
    server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server_socket.bind(('0.0.0.0', 12345))
    server_socket.listen(5)
    print("TCP server listening...")
    while True:
        client_socket, addr = server_socket.accept()
        threading.Thread(target=handle_client,
                         args=(client_socket, ),
                         daemon=True).start()


tcp_thread = threading.Thread(target=tcp_server)
tcp_thread.daemon = True
tcp_thread.start()
try:
    while True:
        time.sleep(1)
except KeyboardInterrupt:
    print("Shutting down")
