"""Pont entre l'application Android (Kotlin) et la bibliothèque pronotepy.

Les identifiants de session sont écrits dans un fichier privé de l'application
juste après chaque connexion, car Pronote renouvelle le jeton à chaque fois.
"""
import datetime as _dt
import json
import os

import pronotepy


def _client_class(url):
    return pronotepy.ParentClient if "parent" in (url or "").lower() else pronotepy.Client


def _save(path, client, uuid):
    name = ""
    try:
        name = client.info.name or ""
    except Exception:
        pass
    data = {
        "url": client.pronote_url,
        "username": client.username,
        "password": client.password,
        "uuid": uuid,
        "name": name,
    }
    tmp = path + ".tmp"
    with open(tmp, "w", encoding="utf-8") as f:
        json.dump(data, f)
    os.replace(tmp, path)
    return data


def qr_login(creds_path, qr_text, pin, uuid):
    """Première connexion avec le QR code affiché par Pronote et le code à 4 chiffres."""
    try:
        qr = json.loads(qr_text)
    except ValueError:
        raise ValueError("Ce QR code n'est pas un QR code de connexion Pronote")
    for key in ("login", "jeton", "url"):
        if key not in qr:
            raise ValueError("QR code Pronote incomplet (%s manquant)" % key)
    client = _client_class(qr["url"]).qrcode_login(qr, pin, uuid)
    if not client.logged_in:
        raise RuntimeError("Connexion refusée par Pronote")
    return _save(creds_path, client, uuid)["name"]


def _fmt(value):
    return value.strftime("%Y-%m-%dT%H:%M:%S")


def fetch(creds_path, start_iso, days):
    """Se reconnecte avec le jeton enregistré et renvoie les cours en JSON."""
    with open(creds_path, encoding="utf-8") as f:
        c = json.load(f)
    client = _client_class(c["url"]).token_login(c["url"], c["username"], c["password"], c["uuid"])
    if not client.logged_in:
        raise RuntimeError("Session Pronote expirée")
    data = _save(creds_path, client, c["uuid"])

    start = _dt.date.fromisoformat(start_iso)
    end = start + _dt.timedelta(days=int(days))
    out = []
    for lesson in client.lessons(start, end):
        subject = getattr(getattr(lesson, "subject", None), "name", None) or ""
        out.append({
            "start": _fmt(lesson.start),
            "end": _fmt(lesson.end),
            "subject": subject,
            "teacher": getattr(lesson, "teacher_name", None) or "",
            "room": getattr(lesson, "classroom", None) or "",
            "canceled": bool(getattr(lesson, "canceled", False)),
            "status": getattr(lesson, "status", None) or "",
        })
    return json.dumps({"name": data["name"], "lessons": out}, ensure_ascii=False)
