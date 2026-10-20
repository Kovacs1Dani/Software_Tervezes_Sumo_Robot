from flask import Flask, jsonify, request
from flask_cors import CORS
import threading
import time
import csv
import os
import random
from datetime import datetime

app = Flask(__name__)
CORS(app)

class RobotSimulator:
    def __init__(self):
        self.battery = 8.4
        self.mode = "MANUAL"
        # Visszahoztuk a vonalérzékelőt a szumó logikához
        self.sensors = {"s1_front": 80, "line": "Fekete"}
        self.motors = {"pwm_a": 0, "pwm_b": 0, "direction": "STOP"}
        self.log_messages = ["Rendszer elindítva, szumó szimuláció aktív."]

    def update_state(self):
        # 1. Akku merülése
        drain_rate = 0.005 if self.motors["direction"] == "STOP" else 0.015
        if self.battery > 6.0:
            self.battery -= drain_rate

        # Alapértelmezett állapot: bent vagyunk a fekete pálya közepén
        current_line = "Fekete"

        # 2. Szumó Mozgás Logika
        if self.motors["direction"] == "FORWARD":
            # Közeledünk valamihez (távolság csökken)
            if self.sensors["s1_front"] > 5:
                self.sensors["s1_front"] -= 5
                
                # Ha nagyon közel van, az valószínűleg a másik robot
                if self.sensors["s1_front"] == 15:
                    self.log_messages.append("Célpont befogva! Támadás!")

            # Mozgás közben van rá esély, hogy elérjük a dojo szélét (fehér vonal)
            if random.random() < 0.15: # 15% esély
                current_line = "Fehér"
                self.log_messages.append("VESZÉLY: Fehér vonal (Dojo széle) érzékelve!")

        elif self.motors["direction"] == "REVERSE":
            # Tolatunk, távolodunk
            if self.sensors["s1_front"] < 100:
                self.sensors["s1_front"] += 5
        
        elif self.motors["direction"] in ["LEFT", "RIGHT"]:
            # Forgás közben a szenzor "pásztázza" a pályát, így a távolság ugrál
            self.sensors["s1_front"] = random.randint(30, 100)

        self.sensors["line"] = current_line

robot = RobotSimulator()

# Háttérfolyamat a CSV logoláshoz
def simulation_loop():
    csv_file = 'telemetry_log.csv'
    file_exists = os.path.isfile(csv_file)
    
    with open(csv_file, 'a', newline='') as f:
        writer = csv.writer(f)
        if not file_exists:
            writer.writerow(['timestamp', 'battery_v', 'direction', 'distance_cm', 'line_sensor'])
            
        while True:
            robot.update_state()
            writer.writerow([
                datetime.now().isoformat(), 
                round(robot.battery, 2), 
                robot.motors['direction'], 
                robot.sensors['s1_front'],
                robot.sensors['line']
            ])
            f.flush()
            time.sleep(1)

threading.Thread(target=simulation_loop, daemon=True).start()

# --- API VÉGPONTOK (Ez adja a szabványosított "sort" Daninak) ---

@app.route('/api/status', methods=['GET'])
def get_status():
    msg = robot.log_messages.pop(0) if robot.log_messages else None
    
    return jsonify({
        "battery": round(robot.battery, 2),
        "distance_cm": robot.sensors["s1_front"],
        "line_sensor": robot.sensors["line"], 
        "log_message": msg
    })

@app.route('/api/command', methods=['POST'])
def receive_command():
    data = request.json
    cmd = data.get('command')
    
    if cmd == 'FORWARD':
        robot.motors.update({"direction": "FORWARD", "pwm_a": 150, "pwm_b": 150})
    elif cmd == 'REVERSE':
        robot.motors.update({"direction": "REVERSE", "pwm_a": -150, "pwm_b": -150})
    elif cmd == 'LEFT':
        robot.motors.update({"direction": "LEFT", "pwm_a": -100, "pwm_b": 100})
    elif cmd == 'RIGHT':
        robot.motors.update({"direction": "RIGHT", "pwm_a": 100, "pwm_b": -100})
    elif cmd == 'STOP':
        robot.motors.update({"direction": "STOP", "pwm_a": 0, "pwm_b": 0})
        
    robot.log_messages.append(f"Parancs: {cmd}")
    return jsonify({"status": "success", "executed": cmd})

if __name__ == '__main__':
    print("Szumó Mock Szerver indul a 8080-as porton...")
    app.run(port=8080, debug=False, use_reloader=False)