import io
import os
import random
import sqlite3
from datetime import datetime
from flask import Flask, Response, jsonify, request
from flask_cors import CORS

app = Flask(__name__)
CORS(app)

DB_PATH = os.path.join(os.path.dirname(os.path.abspath(__file__)), "telemetry.db")

# Memóriában futó globális szótár a robot állapotának tárolására
robot_state = {
    "mode": "MANUAL",
    "state": "STOP",
    "bat": 8.4,
    "s1": 35,
    "s2": 42,
    "s3": 0,
    "pwmA": 0,
    "pwmB": 0,
}


def calculate_battery_percent(v: float) -> int:
    """
    Pontosan a mobilalkalmazás (BatteryState.kt) számítási logikája:
    - FULL_VOLTAGE = 8.4V (100%)
    - NOMINAL_VOLTAGE = 7.4V (50%)
    - MIN_VOLTAGE = 6.0V (0%)
    """
    full_v = 8.4
    nominal_v = 7.4
    min_v = 6.0
    if v >= full_v:
        return 100
    if v <= min_v:
        return 0
    if v >= nominal_v:
        ratio = (v - nominal_v) / (full_v - nominal_v)
        return max(0, min(100, int(50 + ratio * 50)))
    ratio = (v - min_v) / (nominal_v - min_v)
    return max(0, min(100, int(ratio * 50)))


def init_db():
    """SQLite adatbázis és a logs tábla inicializálása induláskor."""
    with sqlite3.connect(DB_PATH) as conn:
        cursor = conn.cursor()
        cursor.execute(
            """
            CREATE TABLE IF NOT EXISTS logs (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                timestamp TEXT NOT NULL,
                mode TEXT NOT NULL,
                state TEXT NOT NULL,
                bat REAL NOT NULL,
                s1 INTEGER NOT NULL,
                s2 INTEGER NOT NULL,
                s3 INTEGER NOT NULL,
                pwmA INTEGER NOT NULL,
                pwmB INTEGER NOT NULL
            )
            """
        )
        conn.commit()


def log_telemetry(state_snapshot):
    """Az aktuális robot állapot rögzítése az SQLite logs táblába."""
    now = datetime.now()
    timestamp = now.strftime("%Y-%m-%d %H:%M:%S.%f")[:-3]
    with sqlite3.connect(DB_PATH) as conn:
        cursor = conn.cursor()
        cursor.execute(
            """
            INSERT INTO logs (timestamp, mode, state, bat, s1, s2, s3, pwmA, pwmB)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """,
            (
                timestamp,
                state_snapshot["mode"],
                state_snapshot["state"],
                state_snapshot["bat"],
                state_snapshot["s1"],
                state_snapshot["s2"],
                state_snapshot["s3"],
                state_snapshot["pwmA"],
                state_snapshot["pwmB"],
            ),
        )
        conn.commit()


@app.route("/api/telemetry", methods=["GET"])
def get_telemetry():
    """
    Telemetriai végpont:
    - Minimálisan csökkenti az akkumulátor feszültségét (bat)
    - Szimulálja a szenzorok (s1, s2, s3) nyers értékeit
    - Elmenti az aktuális állapotot az SQLite adatbázisba
    - JSON formátumban visszaadja az állapotot
    """
    robot_state["bat"] = round(max(6.0, robot_state["bat"] - 0.01), 2)

    # s1: Bal távolság (cm), s2: Jobb távolság (cm), s3: Vonal/szél érzékelő bitmask (0..3)
    robot_state["s1"] = max(5, min(80, robot_state["s1"] + random.randint(-4, 4)))
    robot_state["s2"] = max(5, min(80, robot_state["s2"] + random.randint(-4, 4)))
    robot_state["s3"] = 1 if random.random() < 0.08 else 0

    log_telemetry(robot_state)
    return jsonify(robot_state), 200


@app.route("/api/command", methods=["POST"])
def post_command():
    """
    Irányítási végpont:
    JSON body-ban várja a parancsot (F, B, L, R, S), majd frissíti
    a globális robot_state state, pwmA és pwmB értékeit.
    """
    data = request.get_json(silent=True) or {}
    cmd = (data.get("command") or data.get("cmd") or "").strip().upper()

    command_map = {
        "F": {"state": "FORWARD", "pwmA": 255, "pwmB": 255},
        "B": {"state": "BACKWARD", "pwmA": 200, "pwmB": 200},
        "L": {"state": "LEFT", "pwmA": 90, "pwmB": 220},
        "R": {"state": "RIGHT", "pwmA": 220, "pwmB": 90},
        "S": {"state": "STOP", "pwmA": 0, "pwmB": 0},
    }

    if "mode" in data and data["mode"] in ("MANUAL", "AUTO"):
        robot_state["mode"] = data["mode"]
        if not cmd:
            return jsonify({"status": "ok", "robot_state": robot_state}), 200

    if cmd not in command_map:
        return (
            jsonify(
                {
                    "status": "error",
                    "message": "Érvénytelen parancs. Engedélyezett értékek: F, B, L, R, S",
                }
            ),
            400,
        )

    robot_state.update(command_map[cmd])

    return jsonify({"status": "ok", "command": cmd, "robot_state": robot_state}), 200


@app.route("/api/export/csv", methods=["GET"])
def export_csv():
    """
    Kiadja az SQLite adatbázisban naplózott adatokat pontosan a mobilalkalmazás
    (TelemetryCsvLogger.kt) RFC-4180 CSV formátumában:
    Timestamp_ms,DateTime,Dist_Left_cm,Dist_Right_cm,Line_Left,Line_Right,Battery_Volts,Battery_Percent
    """
    output = io.StringIO()
    output.write(
        "Timestamp_ms,DateTime,Dist_Left_cm,Dist_Right_cm,Line_Left,Line_Right,Battery_Volts,Battery_Percent\r\n"
    )

    with sqlite3.connect(DB_PATH) as conn:
        cursor = conn.cursor()
        rows = cursor.execute(
            "SELECT timestamp, bat, s1, s2, s3 FROM logs ORDER BY id ASC"
        ).fetchall()

    for ts_str, bat, s1, s2, s3 in rows:
        try:
            dt = datetime.strptime(ts_str, "%Y-%m-%d %H:%M:%S.%f")
        except ValueError:
            dt = datetime.now()
        ts_ms = int(dt.timestamp() * 1000)
        dt_formatted = dt.strftime("%Y-%m-%d %H:%M:%S.%f")[:-3]
        line_left = 1 if (s3 in (1, 3)) else 0
        line_right = 1 if (s3 in (2, 3)) else 0
        bat_pct = calculate_battery_percent(float(bat))

        output.write(
            f'{ts_ms},"{dt_formatted}",{s1},{s2},{line_left},{line_right},{float(bat):.2f},{bat_pct}\r\n'
        )

    file_ts = datetime.now().strftime("%Y%m%d_%H%M%S")
    filename = f"sumo_telemetry_{file_ts}.csv"

    return Response(
        output.getvalue(),
        mimetype="text/csv; charset=utf-8",
        headers={
            "Content-Disposition": f'attachment; filename="{filename}"',
            "Access-Control-Expose-Headers": "Content-Disposition",
        },
    )


# Adatbázis inicializálása induláskor
init_db()

if __name__ == "__main__":
    app.run(host="127.0.0.1", port=5000, debug=True)
