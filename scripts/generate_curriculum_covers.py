from __future__ import annotations

import hashlib
import json
import re
from pathlib import Path

from PIL import Image, ImageDraw, ImageEnhance, ImageFont, ImageOps


WORKSPACE = Path(__file__).resolve().parents[1]
CATALOG = (
    WORKSPACE
    / "SmartCourse-Server"
    / "src"
    / "main"
    / "java"
    / "xyz"
    / "refrain"
    / "onlineedu"
    / "config"
    / "CurriculumCourseCatalog.java"
)
SOURCE_DIR = WORKSPACE / "uploads" / "demo" / "curriculum" / "source"
UNIQUE_DIR = WORKSPACE / "uploads" / "demo" / "curriculum" / "unique"
OUTPUTS_DIR = WORKSPACE.parent / "outputs"

CATEGORY_COLORS = {
    "通识教育必修": (34, 116, 165),
    "通识教育选修": (38, 132, 115),
    "学科基础": (113, 76, 159),
    "专业发展必修": (186, 88, 45),
    "专业发展选修": (40, 106, 176),
    "综合实践": (171, 72, 83),
}

SUPPLEMENTAL_SOURCES = {
    "思想道德与法治": "civic-law-seminar.jpg",
    "中国近现代史纲要": "civic-modern-history.jpg",
    "马克思主义基本原理": "civic-philosophy.jpg",
    "毛泽东思想和中国特色社会主义理论体系概论": "civic-social-theory.jpg",
    "习近平新时代中国特色社会主义思想概论": "civic-public-policy.jpg",
    "形势与政策": "civic-roundtable.jpg",
    "军事理论": "military-theory.jpg",
    "军事技能": "military-skills.jpg",
    "体育 A": "sports-running.jpg",
    "体育 B": "sports-basketball.jpg",
    "体育 C": "sports-badminton.jpg",
    "体育 D": "sports-volleyball.jpg",
    "体育 E": "sports-table-tennis.jpg",
    "体育 F": "sports-stretching.jpg",
    "学术语言与沟通 I": "language-presentation.jpg",
    "学术语言与沟通 II": "language-library.jpg",
    "学术语言与研究方法 I": "language-discussion.jpg",
    "学术语言与研究方法 II": "language-lab.jpg",
    "大学生职业发展与就业指导 A": "career-fair.jpg",
    "大学生职业发展与就业指导 B": "resume-coaching.jpg",
    "大学生创业基础": "startup-workshop.jpg",
    "专业导论与个人发展管理": "pitch-rehearsal.jpg",
    "工程职业实践": "career-workshop.jpg",
}


def parse_catalog() -> list[dict[str, str]]:
    text = CATALOG.read_text(encoding="utf-8")
    constants = dict(
        re.findall(r'private static final String\s+(\w+)\s*=\s*"([^"]+)";', text)
    )
    pattern = re.compile(
        r'^\s*c\("([^"]*)", "([^"]+)", (\w+), "([^"]+)", ([0-9.]+), '
        r'(\d+), "([^"]+)", "([^"]+)", "([^"]*)", (\w+), "([^"]+)"\),?$',
        re.MULTILINE,
    )
    rows = []
    for match in pattern.finditer(text):
        (
            code,
            title,
            category_key,
            course_type,
            credit,
            lessons,
            semester,
            assessment,
            remarks,
            cover_key,
            teacher,
        ) = match.groups()
        rows.append(
            {
                "code": code,
                "title": title,
                "category": constants[category_key],
                "course_type": course_type,
                "credit": credit,
                "lessons": lessons,
                "semester": semester,
                "assessment": assessment,
                "remarks": remarks,
                "base_cover": constants[cover_key],
                "teacher": teacher,
            }
        )
    if len(rows) != 79:
        raise RuntimeError(f"Expected 79 catalog rows, found {len(rows)}")
    return rows


def java_hash(value: str) -> int:
    result = 0
    for character in value:
        result = (31 * result + ord(character)) & 0xFFFFFFFF
    return result


def unique_name(row: dict[str, str]) -> str:
    return f"{java_hash(row['code'] + '|' + row['title']):x}.jpg"


def local_from_api(uri: str) -> Path:
    prefix = "/api/pub/image/"
    if not uri.startswith(prefix):
        raise ValueError(uri)
    return WORKSPACE / "uploads" / uri[len(prefix) :]


def create_cover(row: dict[str, str], out: Path) -> None:
    identity = row["code"] + "|" + row["title"]
    digest = hashlib.sha256(identity.encode("utf-8")).digest()
    source_name = SUPPLEMENTAL_SOURCES.get(row["title"])
    source = SOURCE_DIR / source_name if source_name else local_from_api(row["base_cover"])
    with Image.open(source) as source_image:
        image = source_image.convert("RGB")

    target_width, target_height = 480, 270
    scale = max((target_width + 88) / image.width, (target_height + 54) / image.height)
    image = image.resize(
        (round(image.width * scale), round(image.height * scale)),
        Image.Resampling.LANCZOS,
    )
    max_x = max(0, image.width - target_width)
    max_y = max(0, image.height - target_height)
    x = digest[0] * max_x // 255 if max_x else 0
    y = digest[1] * max_y // 255 if max_y else 0
    image = image.crop((x, y, x + target_width, y + target_height))
    if digest[2] % 2:
        image = ImageOps.mirror(image)
    image = ImageEnhance.Color(image).enhance(0.88 + digest[3] / 255 * 0.28)
    image = ImageEnhance.Contrast(image).enhance(0.94 + digest[4] / 255 * 0.18)
    image = ImageEnhance.Brightness(image).enhance(0.94 + digest[5] / 255 * 0.12)

    canvas = image.convert("RGBA")
    overlay = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
    draw = ImageDraw.Draw(overlay)
    accent = CATEGORY_COLORS[row["category"]]
    draw.rectangle((0, target_height - 34, target_width, target_height), fill=(*accent, 218))

    regular = ImageFont.truetype(r"C:\Windows\Fonts\msyh.ttc", 17)
    draw.text((15, target_height - 28), row["category"], font=regular, fill=(255, 255, 255, 255))
    Image.alpha_composite(canvas, overlay).convert("RGB").save(
        out, quality=90, optimize=True
    )


def save_preview(manifest: list[dict[str, str]]) -> Path:
    preview = Image.new("RGB", (4 * 480, 4 * 270), (240, 244, 248))
    for index, row in enumerate(manifest[:16]):
        with Image.open(UNIQUE_DIR / row["file"]) as tile:
            preview.paste(tile.convert("RGB"), ((index % 4) * 480, (index // 4) * 270))
    OUTPUTS_DIR.mkdir(exist_ok=True)
    preview_path = OUTPUTS_DIR / "curriculum-unique-covers-preview.jpg"
    preview.save(preview_path, quality=90, optimize=True)
    return preview_path


def main() -> None:
    UNIQUE_DIR.mkdir(parents=True, exist_ok=True)
    rows = parse_catalog()
    manifest = []
    for row in rows:
        filename = unique_name(row)
        create_cover(row, UNIQUE_DIR / filename)
        manifest.append({**row, "file": filename})
    (UNIQUE_DIR / "manifest.json").write_text(
        json.dumps(manifest, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )
    preview_path = save_preview(manifest)

    hashes: dict[str, list[str]] = {}
    for row in manifest:
        file_hash = hashlib.sha256((UNIQUE_DIR / row["file"]).read_bytes()).hexdigest()
        hashes.setdefault(file_hash, []).append(row["title"])
    duplicates = [titles for titles in hashes.values() if len(titles) > 1]
    print(
        f"generated={len(manifest)} unique_files={len(hashes)} "
        f"duplicates={len(duplicates)} preview={preview_path}"
    )


if __name__ == "__main__":
    main()
