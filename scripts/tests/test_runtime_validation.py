import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]
WORKFLOW = ROOT / ".github" / "workflows" / "neoforge-1.21.1-port.yml"


class RuntimeValidationWorkflowTest(unittest.TestCase):
    def test_ci_runs_datagen_and_rejects_generated_resource_drift(self):
        workflow = WORKFLOW.read_text(encoding="utf-8")
        self.assertIn("runData", workflow)
        self.assertIn("git diff --exit-code -- src/generated/resources", workflow)

    def test_ci_starts_a_real_gametest_server(self):
        workflow = WORKFLOW.read_text(encoding="utf-8")
        self.assertIn("runGameTestServer", workflow)


if __name__ == "__main__":
    unittest.main()
