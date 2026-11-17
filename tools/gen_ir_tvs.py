#!/usr/bin/env python3
"""
Genera app/src/main/assets/teles_ir.json a partir de la carpeta TVs/ de Flipper-IRDB
(https://github.com/Lucaslhm/Flipper-IRDB, dominio público CC0).

Uso:  python3 tools/gen_ir_tvs.py RUTA/A/Flipper-IRDB/TVs app/src/main/assets/teles_ir.json

Se queda con un control por cada señal de encendido distinta (protocolo + código), prefiriendo
el archivo que trae más botones, y ordena primero AOC/Philips (misma fábrica, TPV) y las marcas
más comunes en Chile. La app los usa en «Búsqueda automática».
"""
import json
import os
import re
import sys

# Nombre normalizado del botón en Flipper → botón de la app.
SYNONYMS = {
    "POWER": ["power", "pwr", "onoff", "powertoggle", "standby", "poweronoff"],
    "VOL_UP": ["volup", "volumeup", "volplus", "volumeplus"],
    "VOL_DOWN": ["voldn", "voldown", "volumedown", "volminus", "volumeminus"],
    "MUTE": ["mute"],
    "CH_UP": ["chnext", "chup", "channelup", "chplus", "progup", "pup"],
    "CH_DOWN": ["chprev", "chdown", "chdn", "channeldown", "chminus", "progdown", "pdown"],
    "INPUT": ["input", "source", "src", "tvav", "avtv", "inputsource", "av"],
    "HOME": ["home", "smarttv", "smarthub"],
    "SETTINGS": ["settings", "menu", "setup"],
    "UP": ["up", "arrowup", "navup", "cursorup"],
    "DOWN": ["down", "arrowdown", "navdown", "cursordown"],
    "LEFT": ["left", "arrowleft", "navleft", "cursorleft"],
    "RIGHT": ["right", "arrowright", "navright", "cursorright"],
    "OK": ["ok", "enter", "select", "center"],
    "BACK": ["back", "return", "ret"],
    "EXIT": ["exit"],
    "NETFLIX": ["netflix"],
    "PRIME": ["prime", "primevideo", "amazon", "amazonprime"],
    "DISNEY": ["disney", "disneyplus"],
    "YOUTUBE": ["youtube"],
    "NUM_0": ["0", "num0", "digit0"],
}
for d in range(1, 10):
    SYNONYMS[f"NUM_{d}"] = [str(d), f"num{d}", f"digit{d}"]
LOOKUP = {alias: key for key, aliases in SYNONYMS.items() for alias in aliases}

PRIORITY = ["aoc", "philips", "tcl", "hisense", "samsung", "lg", "sony", "panasonic", "jvc",
            "daewoo", "hyundai", "master", "kalley", "caixun", "challenger", "noblex", "rca",
            "sharp", "toshiba", "vizio", "polaroid", "haier", "xiaomi", "skyworth", "insignia"]


def rev8(b):
    return int(f"{b:08b}"[::-1], 2)


def parse_hex_bytes(text):
    return [int(x, 16) for x in text.split()]


def to_code(protocol, address, command):
    """Devuelve (protocolo_app, código) o None si la app no sabe enviarlo."""
    a = parse_hex_bytes(address)
    c = parse_hex_bytes(command)
    if protocol == "NEC":
        A, C = a[0], c[0]
        return "NEC", (rev8(A) << 24) | (rev8(~A & 0xFF) << 16) | (rev8(C) << 8) | rev8(~C & 0xFF)
    if protocol == "NECext":
        return "NEC", (rev8(a[0]) << 24) | (rev8(a[1]) << 16) | (rev8(c[0]) << 8) | rev8(c[1])
    if protocol == "RC6":
        return "RC6", (a[0] << 8) | c[0]
    if protocol in ("RC5", "RC5X"):
        return "RC5", ((a[0] & 0x1F) << 8) | (c[0] & 0x7F)
    if protocol == "Samsung32":
        A, C = a[0], c[0]
        return "SAMSUNG", (rev8(A) << 24) | (rev8(A) << 16) | (rev8(C) << 8) | rev8(~C & 0xFF)
    return None


def parse_file(path):
    blocks, cur = [], {}
    with open(path, encoding="utf-8", errors="replace") as f:
        for line in f:
            line = line.strip()
            if line.startswith("#") or not line:
                if cur:
                    blocks.append(cur)
                    cur = {}
                continue
            if ":" in line:
                k, v = line.split(":", 1)
                if k.strip() == "name" and cur:
                    blocks.append(cur)
                    cur = {}
                cur[k.strip()] = v.strip()
    if cur:
        blocks.append(cur)
    return [b for b in blocks if b.get("type") == "parsed" and "name" in b]


def main(src, dst):
    sets = []
    for root, _, files in os.walk(src):
        for name in sorted(files):
            if not name.endswith(".ir"):
                continue
            path = os.path.join(root, name)
            codes, protocols = {}, set()
            for b in parse_file(path):
                raw = b["name"].lower().replace("+", "plus").replace("-", "minus")
                norm = re.sub(r"[^a-z0-9]", "", raw)
                key = LOOKUP.get(norm)
                if key is None or key in codes:
                    continue
                conv = to_code(b.get("protocol", ""), b.get("address", ""), b.get("command", ""))
                if conv is None:
                    continue
                protocols.add(conv[0])
                codes[key] = conv[1]
            if "POWER" not in codes or len(protocols) != 1:
                continue
            brand = os.path.relpath(root, src).split(os.sep)[0]
            sets.append({
                "marca": brand,
                "control": name[:-3],
                "protocolo": protocols.pop(),
                "codigos": codes,
            })

    # Botones que le faltan a un control: se completan con otros controles de la misma
    # «marca» de señal (mismo protocolo y dirección), usando el código más repetido.
    from collections import Counter, defaultdict

    def brand_of(protocol, code):
        return code >> 16 if protocol in ("NEC", "SAMSUNG") else code >> 8

    pool = defaultdict(lambda: defaultdict(Counter))
    for s in sets:
        b = (s["protocolo"], brand_of(s["protocolo"], s["codigos"]["POWER"]))
        for k, v in s["codigos"].items():
            if brand_of(s["protocolo"], v) == b[1]:
                pool[b][k][v] += 1

    # Un control por señal de encendido; gana el que trae más botones.
    best = {}
    for s in sets:
        k = (s["protocolo"], s["codigos"]["POWER"])
        if k not in best or len(s["codigos"]) > len(best[k]["codigos"]):
            best[k] = s

    def order(s):
        b = s["marca"].lower()
        rank = next((i for i, p in enumerate(PRIORITY) if b.startswith(p)), len(PRIORITY))
        return (rank, b, -len(s["codigos"]))

    for s in best.values():
        b = (s["protocolo"], brand_of(s["protocolo"], s["codigos"]["POWER"]))
        added = []
        for k, counter in pool[b].items():
            if k not in s["codigos"]:
                s["codigos"][k] = counter.most_common(1)[0][0]
                added.append(k)
        if added:
            s["completados"] = sorted(added)

    result = sorted(best.values(), key=order)
    for s in result:
        s["codigos"] = {k: f"0x{v:08X}" for k, v in s["codigos"].items()}
    os.makedirs(os.path.dirname(dst), exist_ok=True)
    with open(dst, "w", encoding="utf-8") as f:
        json.dump({"fuente": "Flipper-IRDB (CC0)", "teles": result}, f, ensure_ascii=False, separators=(",", ":"))
    print(f"{len(result)} controles distintos ({len(sets)} archivos útiles) → {dst}")


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
