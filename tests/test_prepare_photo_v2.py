"""Synthetic-source tests for the photo preparation stage; do not test aesthetics."""
import json
import tempfile
import unittest
from pathlib import Path
from PIL import Image

from scripts.prepare_photo_v2 import prepare_photo


class PreparePhotoTest(unittest.TestCase):
    def setUp(self):
        self.working = tempfile.TemporaryDirectory()
        self.addCleanup(self.working.cleanup)
        self.root = Path(self.working.name)
        self.assets = self.root / "assets-v2"

    def photo(self, name, colour=(124, 83, 30), size=(225, 400), mode="RGB"):
        path = self.root / name
        Image.new(mode, size, colour).save(path)
        return path

    def test_original_is_kept_and_candidate_is_pending_not_approved(self):
        source = self.photo("tiger.png")
        record = prepare_photo("amur_tiger", source, self.assets)
        self.assertEqual(record["status"], "pending_visual_review")
        self.assertEqual(record["asset"], "amur_tiger.webp")
        self.assertTrue(record["upscaled"])
        self.assertTrue((self.assets / "originals" / record["original"]).read_bytes() == source.read_bytes())
        self.assertEqual(record["output_dimensions"], {"width": 1440, "height": 2560})
        with Image.open(self.assets / "candidates/amur_tiger.webp") as image:
            self.assertEqual(image.size, (1440, 2560))
            self.assertEqual(image.format, "WEBP")
        self.assertEqual(
            json.loads((self.assets / "candidates/metadata/amur_tiger.json").read_text())["status"],
            "pending_visual_review",
        )

    def test_rejected_candidate_can_be_replaced_without_losing_history(self):
        first = prepare_photo("amur_tiger", self.photo("first.png", (255, 20, 20)), self.assets)
        second_source = self.photo("second.png", (20, 70, 255))
        with self.assertRaises(FileExistsError):
            prepare_photo("amur_tiger", second_source, self.assets)
        second = prepare_photo("amur_tiger", second_source, self.assets, replace_candidate=True)
        self.assertNotEqual(first["sha256"], second["sha256"])
        self.assertNotEqual(first["original"], second["original"])
        self.assertTrue((self.assets / "originals" / first["original"]).exists())
        self.assertTrue((self.assets / "originals" / second["original"]).exists())
        history = self.assets / "candidates/history"
        self.assertTrue((history / f"amur_tiger-{first['sha256'][:12]}.webp").exists())
        self.assertTrue((history / f"amur_tiger-{first['sha256'][:12]}.json").exists())

    def test_personal_ari_is_never_regenerated(self):
        with self.assertRaises(ValueError):
            prepare_photo("ari", self.photo("ari.png"), self.assets)

    def test_wrong_image_proportions_are_rejected_without_creating_candidate(self):
        with self.assertRaises(ValueError):
            prepare_photo("amur_tiger", self.photo("square.png", size=(400, 400)), self.assets)
        self.assertFalse((self.assets / "candidates/amur_tiger.webp").exists())

    def test_transparency_is_rejected(self):
        with self.assertRaises(ValueError):
            prepare_photo("amur_tiger", self.photo("alpha.png", (10, 90, 20, 100), mode="RGBA"), self.assets)

    def test_unknown_id_is_rejected(self):
        with self.assertRaises(ValueError):
            prepare_photo("nonexistent", self.photo("wrong.png"), self.assets)


if __name__ == "__main__":
    unittest.main()
