import http.server
import socketserver
import json
import random
import time

PORT = 8080

class MockRobotHandler(http.server.SimpleHTTPRequestHandler):
    def do_OPTIONS(self):
        self.send_response(200, "ok")
        self.send_header('Access-Control-Allow-Origin', '*')
        self.send_header('Access-Control-Allow-Methods', 'GET, OPTIONS')
        self.send_header("Access-Control-Allow-Headers", "X-Requested-With")
        self.send_header("Access-Control-Allow-Headers", "Content-Type")
        self.end_headers()

    def do_GET(self):
        if self.path == '/api/status':
            self.send_response(200)
            self.send_header('Content-type', 'application/json')
            self.send_header('Access-Control-Allow-Origin', '*')
            self.end_headers()

            # Szimulált robot adatok generálása
            data = {
                "battery": round(random.uniform(6.5, 8.4), 2),
                "distance_cm": random.randint(5, 100),
                "line_sensor": random.choice(["Fehér", "Fekete"]),
                "log_message": random.choice([
                    "Motor A PWM 150", 
                    "Fordulás balra...", 
                    "Akadály érzékelve!", 
                    "Vonalkeresés...",
                    None, None, None # Hogy ne legyen minden pillanatban új log
                ])
            }
            self.wfile.write(json.dumps(data).encode())
        else:
            self.send_error(404)

with socketserver.TCPServer(("", PORT), MockRobotHandler) as httpd:
    print(f"Mock Szerver fut a {PORT}-es porton.")
    print(f"Böngészőben nyisd meg az index.html fájlt a csatlakozáshoz.")
    try:
        httpd.serve_forever()
    except KeyboardInterrupt:
        print("\nSzerver leállítva.")