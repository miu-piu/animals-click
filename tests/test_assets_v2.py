"""Contract tests for 2.0 catalog, image briefs and packaging safeguards."""
import tempfile
import unittest
from pathlib import Path

from scripts.package_content_v2 import validate_cards, validate_image, validate_approvals
from scripts.make_generation_briefs_v2 import briefs


class ContentContractTest(unittest.TestCase):
    def test_generation_plan_has_one_entry_per_card(self):
        cards = briefs()
        self.assertEqual(len(cards), 142)
        self.assertEqual(len({c["id"] for c in cards}), 142)
        self.assertEqual(sum(c["generation"] == "GENERATE_SEPARATELY" for c in cards), 141)

    def test_ari_must_not_be_generated(self):
        card = next(c for c in briefs() if c["id"] == "ari")
        self.assertEqual(card["generation"], "PRESERVE_REAL_PHOTO")
        self.assertNotIn("prompt", card)

    def test_far_eastern_species_and_ray_have_special_briefs(self):
        cards = {c["id"]: c for c in briefs()}
        for name in ("amur_tiger", "amur_leopard", "snow_leopard", "ray"):
            self.assertIn("COMPOSITION AND POSE", cards[name]["prompt"])
        self.assertIn("not jaguar", cards["amur_leopard"]["prompt"].lower())
        self.assertEqual(cards["ray"]["label"], "скат")

    def test_duplicate_ids_and_assets_are_rejected(self):
        valid = [
            {"id": "a", "label": "кот", "asset": "a.webp"},
            {"id": "b", "label": "пёс", "asset": "b.webp"},
        ]
        validate_cards(valid)
        with self.assertRaises(ValueError):
            validate_cards([valid[0], dict(valid[0])])
        with self.assertRaises(ValueError):
            validate_cards([valid[0], {"id": "b", "label": "пёс", "asset": "a.webp"}])

    def test_directory_traversal_and_wrong_extension_are_rejected(self):
        with self.assertRaises(ValueError):
            validate_cards([{"id": "bad", "label": "кот", "asset": "../cat.webp"}])
        with self.assertRaises(ValueError):
            validate_cards([{"id": "bad", "label": "кот", "asset": "cat.png"}])

    def test_release_requires_individual_approval_and_locked_source(self):
        digest = "a" * 64
        valid = {"cat.webp": {
            "status": "approved", "sha256": digest,
            "original_sha256": "b" * 64,
            "review_notes": "Full visible paws; calm domestic interior; checked anatomy."
        }}
        self.assertEqual(validate_approvals(valid, {"cat.webp"}), {"cat.webp": digest})
        invalid = {"cat.webp": {**valid["cat.webp"], "status": "pending"}}
        with self.assertRaises(ValueError):
            validate_approvals(invalid, {"cat.webp"})
        invalid = {"cat.webp": {**valid["cat.webp"], "review_notes": ""}}
        with self.assertRaises(ValueError):
            validate_approvals(invalid, {"cat.webp"})
        with self.assertRaises(ValueError):
            validate_approvals(valid, {"cat.webp", "dog.webp"})

    def test_non_webp_binary_is_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "test.webp"
            path.write_bytes(b"not a real image")
            with self.assertRaises(ValueError):
                validate_image(path, strict_dimensions=False)


if __name__ == "__main__":
    unittest.main()
