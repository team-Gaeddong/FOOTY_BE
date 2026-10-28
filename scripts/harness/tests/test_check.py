"""Exercise observable gate behavior in isolated repositories; no Java needed."""
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest
import unicodedata

SOURCE = Path(__file__).resolve().parents[1]


class GateTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory(prefix="footy-gate-test-")
        self.addCleanup(self.tmp.cleanup)
        self.repo = Path(self.tmp.name) / "repo"
        self.repo.mkdir()
        self.env = os.environ.copy()
        for key in list(self.env):
            if key.startswith("GIT_"):
                self.env.pop(key)
        self.env.update(GIT_CONFIG_NOSYSTEM="1", GIT_CONFIG_GLOBAL=os.devnull)
        self.env["TMPDIR"] = self.tmp.name
        dest = self.repo / "scripts/harness"
        dest.mkdir(parents=True)
        for name in ("check.sh", "check_commits.py"):
            shutil.copy2(SOURCE / name, dest / name)
        self.stub_build(0)
        self.git("init", "-b", "main")
        self.git("config", "user.name", "Harness test")
        self.git("config", "user.email", "test@example.invalid")
        self.git("config", "commit.gpgsign", "false")
        self.git("add", ".")
        self.commit("initial baseline")
        self.base = self.git("rev-parse", "HEAD")
        self.git("update-ref", "refs/remotes/origin/main", "HEAD")
        self.git("switch", "-c", "feature/12_harness")

    def git(self, *args):
        return subprocess.check_output(
            ["git", *args], cwd=self.repo, env=self.env, stderr=subprocess.PIPE,
            text=True,
        ).strip()

    def commit(self, message="🧪 :: 검증 추가"):
        self.git("commit", "--allow-empty", "-m", message)

    def stub_build(self, code):
        f = self.repo / "gradlew"
        f.write_text(f'#!/bin/sh\nprintf "%s\\n" "$@" > build-invocation\nexit {code}\n')
        f.chmod(0o755)

    def gate(self, expected, *args):
        result = subprocess.run(
            ["bash", "scripts/harness/check.sh", *args], cwd=self.repo,
            env=self.env, text=True, capture_output=True,
        )
        self.assertEqual(result.returncode, expected, result.stdout + result.stderr)
        return result

    def test_valid_change_builds_and_cleans_success_log(self):
        self.commit()
        self.gate(0)
        self.assertEqual((self.repo / "build-invocation").read_text().splitlines(),
                         ["clean", "build", "--console=plain"])
        self.assertEqual(list(Path(self.tmp.name).glob("footy-harness-build.*")), [])

    def test_missing_base_cannot_pass_bad_commit(self):
        self.commit("bad commit")
        self.git("update-ref", "-d", "refs/remotes/origin/main")
        self.gate(1)

    def test_non_commit_base_fails(self):
        self.commit()
        blob = self.git("rev-parse", "HEAD:scripts/harness/check.sh")
        self.gate(1, "--base", blob)

    def test_main_direct_commit_fails(self):
        self.git("switch", "main")
        self.commit()
        self.gate(1)

    def test_develop_direct_commit_fails(self):
        self.git("switch", "-c", "develop")
        self.commit()
        self.gate(1)

    def test_invalid_branch_fails(self):
        self.git("branch", "-m", "feature/no-issue")
        self.commit()
        self.gate(1)

    def test_hyphenated_issue_branch_passes(self):
        self.git("branch", "-m", "feature/30-재경기-생성-api-구현")
        self.commit()
        self.gate(0)

    def test_invalid_commit_fails(self):
        self.commit("feat :: 검증 추가")
        self.gate(1)

    def test_subject_over_50_fails(self):
        self.commit("🧪 :: " + "가" * 51)
        self.gate(1)

    def test_decomposed_korean_50_char_subject_passes(self):
        self.commit("🧪 :: " + unicodedata.normalize("NFD", "가" * 50))
        self.gate(0)

    def test_english_only_fails(self):
        self.commit("🧪 :: Add tests")
        self.gate(1)

    def test_merge_type_requires_issue(self):
        self.commit("🔀 :: 병합 추가")
        self.gate(1)

    def test_optional_ordinary_issue_and_scope_forms_remain_accepted(self):
        for message in ["🐛 :: 버그 수정", "🐛 :: (#12)[서비스] 버그 수정",
                        "📝 :: [문서] 설명 추가", "🔀 :: (#12) 변경 병합"]:
            self.commit(message)
        self.gate(0)

    def test_empty_change_range_is_partial(self):
        self.gate(2)

    def test_skip_build_is_partial_and_does_not_build(self):
        self.commit()
        self.gate(2, "--skip-build")
        self.assertFalse((self.repo / "build-invocation").exists())

    def test_skip_does_not_mask_failure(self):
        self.commit("bad")
        self.gate(1, "--skip-build")

    def test_build_failure_preserves_log(self):
        self.commit()
        self.stub_build(1)
        self.gate(1)
        self.assertEqual(len(list(Path(self.tmp.name).glob("footy-harness-build.*"))), 1)

    def test_detached_requires_explicit_branch(self):
        self.commit()
        self.git("checkout", "--detach")
        self.gate(1)
        self.gate(0, "--branch", "feature/12_harness", "--head", "HEAD",
                  "--base", self.base)

    def test_attached_branch_cannot_be_disguised(self):
        self.git("switch", "main")
        self.commit()
        self.gate(1, "--branch", "feature/12_harness")

    def test_head_must_match_built_checkout(self):
        self.commit()
        self.gate(1, "--head", self.base)

    def test_baseline_explicitly_builds_main_without_commit_policy(self):
        self.git("switch", "main")
        self.gate(0, "--mode", "baseline")
        self.assertTrue((self.repo / "build-invocation").exists())
        self.gate(2, "--mode", "baseline", "--skip-build")

    def test_invalid_arguments_fail_before_build(self):
        for args in [("--unknown",), ("--base",), ("--base", "HEAD", "HEAD"),
                     ("--mode", "baseline", "--base", "HEAD"),
                     ("--mode", "bogus"), ("--head", "--all")]:
            with self.subTest(args=args):
                self.gate(1, *args)
        self.assertFalse((self.repo / "build-invocation").exists())

    def test_shallow_history_fails(self):
        self.commit()
        clone = Path(self.tmp.name) / "shallow"
        subprocess.check_call(["git", "clone", "--depth=1", self.repo.as_uri(), str(clone)],
                              env=self.env, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        self.repo = clone
        self.gate(1, "--base", "HEAD")

    def test_advanced_base_uses_shared_ancestor(self):
        self.commit()
        self.git("switch", "main")
        self.commit("legacy base commit outside feature scope")
        self.git("update-ref", "refs/remotes/origin/main", "HEAD")
        self.git("switch", "feature/12_harness")
        self.gate(0)

    def test_unrelated_history_fails(self):
        self.commit()
        self.git("switch", "--orphan", "unrelated")
        self.commit("other root")
        self.git("update-ref", "refs/remotes/origin/main", "HEAD")
        self.git("switch", "feature/12_harness")
        self.gate(1)

    def test_git_log_failure_cannot_pass(self):
        self.commit()
        executable = shutil.which("git")
        bindir = Path(self.tmp.name) / "bin"
        bindir.mkdir()
        shim = bindir / "git"
        shim.write_text(f'#!/bin/sh\nif [ "$1" = log ]; then exit 42; fi\nexec "{executable}" "$@"\n')
        shim.chmod(0o755)
        self.env["PATH"] = str(bindir) + os.pathsep + self.env["PATH"]
        self.gate(1)


if __name__ == "__main__":
    unittest.main()
