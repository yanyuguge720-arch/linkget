"""LinkGet parser service.

Optional backend for the Android app. The app works with its on-device Douyin /
Bilibili parsers; configure a base URL in 我的 → 解析服务 to use this service
instead (useful for Bilibili 1080P when server-side cookies are available).

Run:
    pip install -r requirements.txt
    uvicorn app:app --host 0.0.0.0 --port 8000

The endpoint follows the PRD contract:
    POST /api/parse {"url": "..."} -> {"code":200,"msg":"...","data":{...}}
"""

from __future__ import annotations

import re
from typing import Any

import yt_dlp
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel

app = FastAPI(title="LinkGet Parser", version="0.1.0")
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

UA = (
    "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 "
    "(KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
)


class ParseRequest(BaseModel):
    url: str


def _duration(seconds: Any) -> str:
    try:
        total = int(float(seconds or 0))
    except (TypeError, ValueError):
        return ""
    if total <= 0:
        return ""
    return f"{total // 3600:02d}:{(total % 3600) // 60:02d}:{total % 60:02d}"


def _size(value: Any) -> int | None:
    try:
        size = int(value)
        return size if size > 0 else None
    except (TypeError, ValueError):
        return None


def _quality(height: Any) -> str:
    try:
        h = int(height)
        return f"{h}P" if h > 0 else "原画"
    except (TypeError, ValueError):
        return "原画"


def _pick_formats(info: dict[str, Any]) -> list[dict[str, Any]]:
    formats = info.get("formats") or []
    videos: dict[str, dict[str, Any]] = {}
    audio_url: str | None = None
    best_audio_size: int | None = None

    audio_candidates = [
        f for f in formats
        if f.get("acodec") not in (None, "none") and f.get("vcodec") in (None, "none")
    ]
    audio_candidates.sort(key=lambda f: (f.get("abr") or 0, f.get("tbr") or 0), reverse=True)
    if audio_candidates:
        best = audio_candidates[0]
        audio_url = best.get("url")
        best_audio_size = _size(best.get("filesize") or best.get("filesize_approx"))

    for f in formats:
        url = f.get("url")
        if not url:
            continue
        vcodec = f.get("vcodec")
        acodec = f.get("acodec")
        if vcodec in (None, "none"):
            continue
        height = f.get("height") or 0
        quality = _quality(height)
        combined = acodec not in (None, "none")
        candidate = {
            "type": "video",
            "quality": quality,
            "duration": _duration(info.get("duration")),
            "size": _size(f.get("filesize") or f.get("filesize_approx")),
            "download_url": url,
            "need_merge": not combined and audio_url is not None,
            "audio_url": audio_url if not combined else None,
        }
        current = videos.get(quality)
        if current is None:
            videos[quality] = candidate
        elif current.get("need_merge") and combined:
            videos[quality] = candidate

    def rank(item: dict[str, Any]) -> int:
        match = re.search(r"(\d+)P", item.get("quality", ""))
        return int(match.group(1)) if match else 0

    chosen = sorted(videos.values(), key=rank, reverse=True)[:4]

    if audio_url:
        chosen.append(
            {
                "type": "audio",
                "duration": _duration(info.get("duration")),
                "size": best_audio_size,
                "download_url": audio_url,
            }
        )
    return chosen


def _work(info: dict[str, Any], source_url: str) -> dict[str, Any]:
    resource_list = _pick_formats(info)
    if not resource_list:
        raise ValueError("没有解析到可下载资源")

    thumbnails = info.get("thumbnails") or []
    cover = info.get("thumbnail")
    if not cover and thumbnails:
        cover = thumbnails[-1].get("url")

    return {
        "id": str(info.get("id") or ""),
        "title": info.get("title") or "未命名作品",
        "author": info.get("uploader") or info.get("creator") or info.get("channel"),
        "cover": cover,
        "duration": _duration(info.get("duration")),
        "source_url": source_url,
        "resource_list": resource_list,
    }


@app.get("/api/health")
def health() -> dict[str, Any]:
    return {"code": 200, "msg": "ok"}


@app.post("/api/parse")
def parse(request: ParseRequest) -> dict[str, Any]:
    url = request.url.strip()
    if not url:
        return {"code": 400, "msg": "缺少链接", "data": None}
    options = {
        "quiet": True,
        "no_warnings": True,
        "skip_download": True,
        "noplaylist": True,
        "format": "bv*+ba/b",
        "http_headers": {"User-Agent": UA},
    }
    try:
        with yt_dlp.YoutubeDL(options) as ydl:
            info = ydl.extract_info(url, download=False)
            if info is None:
                raise ValueError("没有解析到内容")
            if "entries" in info:
                entries = [e for e in (info.get("entries") or []) if e]
                if not entries:
                    raise ValueError("没有解析到内容")
                info = entries[0]
            data = _work(info, url)
        return {"code": 200, "msg": "解析成功", "data": data}
    except Exception as exc:
        return {"code": 500, "msg": f"解析失败：{exc}", "data": None}
