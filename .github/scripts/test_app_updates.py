"""Tests for app_updates.py: version order, and Android TV builds on APKPure.

Run from the repository root: python3 -m unittest discover -s .github/scripts -p 'test_*.py'
"""

import unittest
from pathlib import Path
from unittest import mock

import app_updates


class NewerTest(unittest.TestCase):
    def assertNewer(self, a, b):
        self.assertTrue(app_updates.newer(a, b), f"{a} should be newer than {b}")
        self.assertFalse(app_updates.newer(b, a), f"{b} should not be newer than {a}")

    def assertNotNewer(self, a, b):
        self.assertFalse(app_updates.newer(a, b), f"{a} should not be newer than {b}")

    def test_plain_versions_compare_by_number(self):
        self.assertNewer("22.0.0.321", "22.0.0.36")
        self.assertNewer("10.44.2", "10.9.0")
        self.assertNotNewer("1.0", "1.0")

    def test_hulu_build_part(self):
        # APKPure lists only the part after the "+".
        self.assertTrue(app_updates.newer("18552421-google", "6.34.0+18552420-google"))
        self.assertFalse(app_updates.newer("18552419-google", "6.34.0+18552420-google"))

    def test_disney_build_parts_order_by_date(self):
        # APKPure lists "rc3-2026.11.02"; the rc number starts over with each version.
        pinned = "26.20.0+rc3-2026.11.02"
        self.assertTrue(app_updates.newer("rc1-2026.11.20", pinned))
        self.assertFalse(app_updates.newer("rc6-2026.07.30", pinned))
        self.assertFalse(app_updates.newer("rc5-2026.10.05", pinned))
        self.assertFalse(app_updates.newer("rc3-2026.11.02", pinned))

    def test_disney_undated_old_releases_are_older(self):
        self.assertFalse(app_updates.newer("3.5.0-rc4", "26.20.0+rc2-2026.10.28"))
        self.assertFalse(app_updates.newer("3.2.3-rc4", "26.20.0+rc3-2026.11.02"))
        self.assertFalse(app_updates.newer("4.4.1-rc1-2025.04.10", "26.20.0+rc3-2026.11.02"))

    def test_disney_full_names(self):
        self.assertNewer("26.21.0+rc1-2026.11.20", "26.20.0+rc3-2026.11.02")
        self.assertNewer("26.20.0+rc3-2026.11.02", "26.20.0+rc2-2026.10.28")

    def test_hulu_tv_build_hash_is_not_a_version_number(self):
        # Issue 64: 6344DFC8P3.9.606 is older than the pinned C59F39CFP3.10.33.
        self.assertNewer("C59F39CFP3.10.33", "6344DFC8P3.9.606")
        self.assertNewer("0A1B2C3DP3.10.40", "C59F39CFP3.10.33")
        self.assertNotNewer("6344DFC8P3.10.33", "C59F39CFP3.10.33")

    def test_sorting_newest_first(self):
        listed = ["rc6-2026.07.30", "rc1-2026.11.20", "3.5.0-rc4", "rc3-2026.11.02", "rc5-2026.10.05"]
        ordered = sorted(listed, key=app_updates.version_order, reverse=True)
        self.assertEqual(ordered, ["rc1-2026.11.20", "rc3-2026.11.02", "rc5-2026.10.05", "rc6-2026.07.30", "3.5.0-rc4"])


class FakePlay:
    def __init__(self, served, download=True):
        self.served = served
        self.download = download
        self.discarded = []

    def version(self, package):
        return self.served

    def downloaded(self, package):
        return (Path("base.apk"), Path("app.apks")) if self.download else None

    def discard(self, package):
        self.discarded.append(package)


class TvBuildTest(unittest.TestCase):
    """APKPure lists only the Android TV build of Tubi 10.44.5000."""

    APPS = {"com.tubitv": {"versions": ["10.44.2"], "patches": ["Skip ads"]}}

    def run_check(self, play, inspected=None):
        tv = {"tv_build": True, "cert": "matches", "version_name": "10.44.5000"}
        with mock.patch.object(app_updates, "apkpure_versions", return_value=["10.44.5000", "10.44.2"]), \
                mock.patch.object(app_updates, "check_version", return_value=tv), \
                mock.patch.object(app_updates, "inspect_apk", return_value=inspected or {}) as inspect:
            results = app_updates.check(self.APPS, {"com.tubitv": "Tubi"}, {}, "cli", "mpp", "work", "aapt2",
                                        "apksigner", None, Path("keep"), play)
        return results[0], inspect

    def test_phone_build_unchanged_is_up_to_date(self):
        play = FakePlay("10.44.2")
        record, inspect = self.run_check(play)
        self.assertEqual(record["status"], "up to date")
        self.assertEqual(record["tv_builds_skipped"], ["10.44.5000"])
        inspect.assert_not_called()
        self.assertEqual(play.discarded, ["com.tubitv"])

    def test_newer_phone_build_is_checked_from_play(self):
        phone = {"tv_build": False, "cert": "matches", "version_name": "10.45.0",
                 "patches": {"applied": ["Skip ads"], "failed": []}}
        record, inspect = self.run_check(FakePlay("10.45.0"), phone)
        self.assertEqual(record["status"], "update")
        self.assertEqual(record["latest"], "10.45.0")
        self.assertEqual(record["source"], "Google Play")
        self.assertFalse(record["rollout"])
        self.assertEqual(inspect.call_args.args[1:3], (Path("base.apk"), Path("app.apks")))
        self.assertEqual(app_updates.review_reasons(record), [])

    def test_unknown_play_version_is_reported_for_review(self):
        record, _ = self.run_check(FakePlay(None))
        self.assertEqual(record["status"], "update")
        self.assertTrue(record["tv_build"])

    def test_tv_only_issue_is_closed(self):
        record = {"package": "com.tubitv", "name": "Tubi", "pinned": "10.44.2", "status": "up to date",
                  "play": "10.44.2", "tv_builds_skipped": ["10.44.5000"]}
        calls = []

        def gh(*args, stdin=None):
            calls.append(args)
            return '[{"number": 32, "title": "App update: Tubi 10.44.5000"}]' if args[:2] == ("issue", "list") else ""

        with mock.patch.object(app_updates, "gh", side_effect=gh):
            app_updates.sync_issues([record], None, {"com.tubitv": "10.44.2"})
        closes = [c for c in calls if c[:2] == ("issue", "close")]
        self.assertEqual(len(closes), 1)
        self.assertEqual(closes[0][2], "32")
        self.assertIn("still serves Tubi `10.44.2` to phones", closes[0][-1])


class CleanupTest(unittest.TestCase):
    def test_keeps_the_download_folder(self):
        """Run 42789639970: patching Play's com.abc.abcnews.apk must not touch
        APKPure's com.abc.abcnews-11.12.0 download folder."""
        import tempfile
        with tempfile.TemporaryDirectory() as workdir:
            work = Path(workdir)
            (work / "com.abc.abcnews-11.12.0").mkdir()
            (work / "com.abc.abcnews-result.json").write_text("{}")
            (work / "com.abc.abcnews-patched.apk").write_text("")
            (work / "com.abc.abcnews-patched-temporary-files").mkdir()
            app_updates.remove_patch_outputs(workdir, "com.abc.abcnews")
            self.assertEqual(sorted(p.name for p in work.iterdir()), ["com.abc.abcnews-11.12.0"])


class OlderApkTest(unittest.TestCase):
    """Issues 65 and 68: APKPure listed a version above the Netflix pin, but the
    APK it served was 9.92.0, below the 9.93.0 pin."""

    PINNED = "9.93.0 build 7 74878"
    APPS = {"com.netflix.mediaclient": {"versions": [PINNED], "patches": ["Disable autoplay"]}}

    def run_phone(self, apk_version, served):
        outcome = {"tv_build": False, "cert": "matches", "version_name": apk_version,
                   "patches": {"applied": ["Disable autoplay"], "failed": []}}
        with mock.patch.object(app_updates, "apkpure_versions", return_value=["9.94.0 build 1 75000"]), \
                mock.patch.object(app_updates, "check_version", return_value=outcome), \
                mock.patch.object(app_updates, "inspect_apk", return_value={}) as inspect:
            results = app_updates.check(self.APPS, {"com.netflix.mediaclient": "Netflix"}, {}, "cli", "mpp",
                                        "work", "aapt2", "apksigner", None, Path("keep"), FakePlay(served))
        return results[0], inspect

    def test_older_apk_is_not_reported(self):
        record, inspect = self.run_phone("9.92.0 build 9 74772", "9.92.0 build 9 74772")
        self.assertEqual(record["status"], "up to date")
        self.assertEqual(record["not_newer"], ["9.94.0 build 1 75000 (APK is 9.92.0 build 9 74772)"])
        inspect.assert_not_called()

    def test_older_apk_with_unknown_play_version_is_up_to_date(self):
        record, _ = self.run_phone("9.92.0 build 9 74772", None)
        self.assertEqual((record["status"], record["latest"]), ("up to date", self.PINNED))

    def test_newer_apk_is_still_reported(self):
        record, _ = self.run_phone("9.94.0 build 1 75000", "9.94.0 build 1 75000")
        self.assertEqual((record["status"], record["latest"]), ("update", "9.94.0 build 1 75000"))

class DevOnlyCloseTest(unittest.TestCase):
    """An app no main release has yet must not be closed as "released"."""

    def run_sync(self, stable):
        record = {"package": "com.netflix.mediaclient", "name": "Netflix", "pinned": "9.93.0 build 7 74878",
                  "status": "up to date"}
        calls = []

        def gh(*args, stdin=None):
            calls.append(args)
            if args[:2] == ("issue", "list"):
                return '[{"number": 68, "title": "App update: Netflix 9.92.0 build 9 74772"}]'
            return ""

        with mock.patch.object(app_updates, "gh", side_effect=gh):
            app_updates.sync_issues([record], None, stable)
        edits = [c for c in calls if c[:2] == ("issue", "edit")]
        closes = [c for c in calls if c[:2] == ("issue", "close")]
        return edits, closes

    def test_dev_only_app_closes_without_claiming_a_release(self):
        edits, closes = self.run_sync({"com.other.app": "1.0"})
        self.assertEqual(len(closes), 1)
        self.assertIn("which no main release includes yet", closes[0][-1])
        self.assertNotIn("main release targets", closes[0][-1])
        self.assertNotIn(app_updates.RELEASED_LABEL, edits[0])

    def test_released_app_still_says_main_release(self):
        edits, closes = self.run_sync({"com.netflix.mediaclient": "9.93.0 build 7 74878"})
        self.assertIn("The latest main release targets Netflix", closes[0][-1])
        self.assertIn(app_updates.RELEASED_LABEL, edits[0])


class CoveredVersionTest(unittest.TestCase):
    """An "update" the release already covers must not open an issue the next run closes."""

    def run_sync(self, latest, stable):
        record = {"package": "com.netflix.mediaclient", "name": "Netflix", "pinned": "9.93.0 build 7 74878",
                  "status": "update", "latest": latest, "targets": [], "patches": None}
        calls = []

        def gh(*args, stdin=None):
            calls.append(args)
            if args[:2] == ("issue", "list"):
                return "[]"
            return "https://github.com/o/r/issues/99" if args[:2] == ("issue", "create") else ""

        with mock.patch.object(app_updates, "gh", side_effect=gh):
            app_updates.sync_issues([record], None, stable)
        return [c for c in calls if c[:2] == ("issue", "create")]

    def test_older_than_main_release_opens_nothing(self):
        stable = {"com.netflix.mediaclient": "9.93.0 build 7 74878"}
        self.assertEqual(self.run_sync("9.92.0 build 9 74772", stable), [])

    def test_same_as_main_release_opens_nothing(self):
        stable = {"com.netflix.mediaclient": "9.93.0 build 7 74878"}
        self.assertEqual(self.run_sync("9.93.0 build 7 74878", stable), [])

    def test_newer_than_main_release_opens_an_issue(self):
        stable = {"com.netflix.mediaclient": "9.93.0 build 7 74878"}
        self.assertEqual(len(self.run_sync("9.94.0 build 2 75100", stable)), 1)


class IsTvBuildTest(unittest.TestCase):
    """A phone build that lists Android TV as optional is not a TV build."""

    def badging(self, text):
        result = mock.Mock(stdout=text)
        with mock.patch.object(app_updates, "run", return_value=result):
            return app_updates.is_tv_build(Path("app.apk"), "aapt2")

    def test_optional_leanback_feature_is_a_phone_build(self):
        self.assertFalse(self.badging(
            "uses-feature-not-required: name='android.software.leanback'\n"
            "launchable-activity: name='com.tubitv.MainActivity'\n"))

    def test_tv_launcher_is_a_tv_build(self):
        self.assertTrue(self.badging(
            "launchable-activity: name='a.Main'\nleanback-launchable-activity: name='a.TvMain'\n"))

    def test_tv_build_that_requires_leanback(self):
        # Starz 6.12.1 for Android TV (version code 9511).
        self.assertTrue(self.badging(
            "launchable-activity: name='com.starz.amznfiretv.SplashActivity'\n"
            "leanback-launchable-activity: name='com.starz.amznfiretv.SplashActivity'\n"
            "  uses-feature: name='android.software.leanback'\n"))

    def test_one_apk_for_phones_and_tvs_is_a_phone_build(self):
        # Substack 3.14.0 has a TV activity, but leanback is optional (issue 95).
        self.assertFalse(self.badging(
            "launchable-activity: name='com.substack.app.ui.AppActivity'\n"
            "leanback-launchable-activity: name='com.substack.tv.TvActivity'\n"
            "  uses-feature-not-required: name='android.software.leanback'\n"))


if __name__ == "__main__":
    unittest.main()
