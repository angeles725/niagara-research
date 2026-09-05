"""One biting test for tools/palette-lexicon-agents.py.

It runs the extractor over a fixture module dir whose lexicon contains a
duplicated bare key and whose module.xml declares exactly one agent. The test
FAILS if the duplicate-key detection is removed (it asserts the duplicated key
is reported with a count of 2 — the B759 hazard), which is the point of the
tool. Kept to a single test method deliberately.
"""

import importlib.util
import os
import unittest

_HERE = os.path.dirname(os.path.abspath(__file__))
_SCRIPT = os.path.join(_HERE, "..", "palette-lexicon-agents.py")
_FIXTURES = os.path.join(_HERE, "fixtures")


def _load_module():
    spec = importlib.util.spec_from_file_location("pla_tool", _SCRIPT)
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


class TestPaletteLexiconAgents(unittest.TestCase):
    def test_duplicate_key_and_agent_are_detected(self):
        pla = _load_module()

        # --- the biting assertion: the duplicated bare key must be reported ---
        data = pla.collect_module(_FIXTURES, "demoMod")
        self.assertEqual(len(data), 1, "expected one artifact (demoMod-rt)")
        art = data[0]
        self.assertIn(
            "foo.bar", art["duplicate_keys"],
            "duplicate-key detection must report 'foo.bar' (B759 hazard); "
            "this fails if find_duplicate_keys is stubbed/removed")
        self.assertEqual(
            art["duplicate_keys"]["foo.bar"], 2,
            "'foo.bar' is defined twice in the fixture lexicon")
        # a key that appears once must NOT be reported as a duplicate
        self.assertNotIn("baz", art["duplicate_keys"])

        # --- agent + palette sanity from the same fixture ---
        self.assertEqual(len(art["agents"]), 1, "fixture declares exactly one agent")
        self.assertTrue(art["palette_entries"], "fixture palette has entries")


if __name__ == "__main__":
    unittest.main()
