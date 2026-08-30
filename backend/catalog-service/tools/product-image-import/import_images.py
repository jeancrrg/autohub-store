"""
Sobe as imagens de images/*.webp pro catalog-service, associando cada
imagem ao produto cujo slug bate com o nome do arquivo (sem extensao).

Uso:
    python import_images.py

Requer catalog-service, Postgres e MinIO de pe (docker compose up),
com as migrations V1-V4 ja aplicadas.
"""
import sys
from pathlib import Path

import requests
from dotenv import load_dotenv
import os

SCRIPT_DIR = Path(__file__).resolve().parent
load_dotenv(SCRIPT_DIR / ".env")

API_BASE_URL = os.getenv("CATALOG_API_BASE_URL", "http://localhost:8004").rstrip("/")
IMAGES_DIR = SCRIPT_DIR / os.getenv("IMAGES_DIR", "images")
REQUEST_TIMEOUT_SECONDS = 15


def find_product_by_slug(slug: str) -> dict | None:
    response = requests.get(
        f"{API_BASE_URL}/api/v1/catalog/products/slug/{slug}",
        timeout=REQUEST_TIMEOUT_SECONDS,
    )
    if response.status_code == 404:
        return None
    response.raise_for_status()
    return response.json()


def upload_image(product_id: str, image_path: Path) -> None:
    with image_path.open("rb") as file_handle:
        files = {"files": (image_path.name, file_handle, "image/webp")}
        response = requests.post(
            f"{API_BASE_URL}/api/v1/catalog/products/{product_id}/images",
            files=files,
            timeout=REQUEST_TIMEOUT_SECONDS,
        )
    response.raise_for_status()


def main() -> int:
    if not IMAGES_DIR.is_dir():
        print(f"ERRO: pasta de imagens nao encontrada: {IMAGES_DIR}")
        return 1

    image_paths = sorted(IMAGES_DIR.glob("*.webp"))
    if not image_paths:
        print(f"ERRO: nenhum .webp encontrado em {IMAGES_DIR}")
        return 1

    uploaded, skipped, not_found, failed = [], [], [], []

    for image_path in image_paths:
        slug = image_path.stem
        try:
            product = find_product_by_slug(slug)
        except requests.RequestException as error:
            print(f"[ERRO] {slug}: falha ao consultar produto ({error})")
            failed.append(slug)
            continue

        if product is None:
            print(f"[NAO ENCONTRADO] {slug}: nenhum produto com esse slug")
            not_found.append(slug)
            continue

        if product.get("images"):
            print(f"[PULADO] {slug}: produto ja tem imagem cadastrada")
            skipped.append(slug)
            continue

        try:
            upload_image(product["id"], image_path)
        except requests.RequestException as error:
            print(f"[ERRO] {slug}: falha ao enviar imagem ({error})")
            failed.append(slug)
            continue

        print(f"[OK] {slug}: imagem enviada")
        uploaded.append(slug)

    print("\n===== Resumo =====")
    print(f"Enviadas:        {len(uploaded)}")
    print(f"Puladas (ja OK): {len(skipped)}")
    print(f"Nao encontradas: {len(not_found)}")
    print(f"Falhas:          {len(failed)}")

    return 1 if (not_found or failed) else 0


if __name__ == "__main__":
    sys.exit(main())
