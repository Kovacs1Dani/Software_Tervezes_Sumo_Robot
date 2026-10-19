from flask import Flask, jsonify, request
from flask_cors import CORS
import threading
import time
import csv
import os
from datetime import datetime

app = Flask(__name__)
CORS(app)

class RobotSimulator:
    def __init__(self):
        self.battery = 8.4
        self.mode = "MANUAL"
        # Vonalérzékelő teljesen eltávolítva a szenzorok közül
        self.sensors = {"s1_front": 80, "s2_left": 100, "s3_right": 100}
        self.motors = {"pwm_a": 0, "pwm_b": 0, "direction": "STOP"}
        self.log_messages = ["Rendszer elindítva, vonalérzékelő kikapcsolva."]

    def update_state(self):
        drain_rate = 0.005 if self.motors["direction"] == "STOP" else 0.015
        if self.battery > 6.0:
            self.battery -= drain_rate

        if self.motors["direction"] == "FORWARD" and self.sensors["s1_front"] > 5:
            self.sensors["s1_front"] -= 2
            if self.sensors["s1_front"] <= 15:
                self.log_messages.append("Figyelem: Akadály a közelben!")
        elif self.motors["direction"] == "REVERSE" and self.sensors["s1_front"] < 100:
            self.sensors["s1_front"] += 2

robot = RobotSimulator()

def simulation_loop():
    csv_file = 'telemetry_log.csv'
    file_exists = os.path.isfile(csv_file)
    
    with open(csv_file, 'a', newline='') as f:
        writer = csv.writer(f)
        if not file_exists:
            writer.writerow(['timestamp', 'battery_v', 'direction', 's1_front'])
            
        while True:
            robot.update_state()
            writer.writerow([
                datetime.now().isoformat(), 
                round(robot.battery, 2), 
                robot.motors['direction'], 
                robot.sensors['s1_front']
            ])
            f.flush()
            time.sleep(1)

threading.Thread(target=simulation_loop, daemon=True).start()

@app.route('/api/status', methods=['GET'])
def get_status():
    msg = robot.log_messages.pop(0) if robot.log_messages else None
    
    return jsonify({
        "battery": round(robot.battery, 2),
        "distance_cm": robot.sensors["s1_front"],
        "line_sensor": "Nincs", # Így küldjük át, hogy a weboldal ne fagyjon le
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
        
    robot.log_messages.append(f"Parancs végrehajtva: {cmd}")
    return jsonify({"status": "success", "executed": cmd})

if __name__ == '__main__':
    print("Flask szerver indul a 8080-as porton...")
    app.run(port=8080, debug=False, use_reloader=False)