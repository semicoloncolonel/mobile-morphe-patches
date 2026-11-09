"""Checks a new bug report against the app versions the patches support.

Runs from .github/workflows/issue_triage.yml when someone outside the project
opens or edits an issue made with the Bug report form. It reads the App, App
version, App version code, and Phone or Android TV build fields, and compares
them with the targets in the patches on main and dev. When the version, the
version code, or the build is not one the patches support, it labels the issue
"untargeted version" and comments with the supported versions. When an edit
fixes that, it removes the label. Reports for apps it does not know, such as
"Other", are left alone.

Run from the repository root with GITHUB_EVENT_PATH set. DRY_RUN=true prints
what it would do instead of doing it.
"""

import json
import os
import re
import subprocess
import sys

LABEL = "untargeted version"
LABEL_COLOR = "C5DEF5"
LABEL_DESCRIPTION = "The reported app version or build is not one the patches support"
REFS = ["origin/main", "origin/dev"]
SOURCE = "patches/src/main/kotlin"
DEV_TARGET_HELPER = "shared/misc/DevTarget.kt"
NO_RESPONSE = "_No response_"


# ── The form ────────────────────────────────────────────────────────────────

def parse_form(body):
    """{field label: value} from an issue form body, with None for empty fields."""
    fields = {}
    for match in re.finditer(r"^### (.+?)\s*\n(.*?)(?=^### |\Z)", body or "", re.M | re.S):
        value = match.group(2).strip()
        fields[match.group(1).strip()] = None if value in ("", NO_RESPONSE) else value
    return fields


# ── The targets ─────────────────────────────────────────────────────────────

def git(*args):
    return subprocess.run(["git", *args], capture_output=True, text=True, check=True).stdout


def call_text(text, start):
    """The text of the call whose opening parenthesis is at start, without the parentheses."""
    depth = 0
    for i in range(start, len(text)):
        if text[i] == "(":
            depth += 1
        elif text[i] == ")":
            depth -= 1
            if depth == 0:
                return text[start + 1:i]
    return text[start + 1:]


def split_args(args):
    """Top-level comma-separated arguments."""
    parts, depth, current = [], 0, ""
    for char in args:
        if char in "([":
            depth += 1
        elif char in ")]":
            depth -= 1
        if char == "," and depth == 0:
            parts.append(current.strip())
            current = ""
        else:
            current += char
    if current.strip():
        parts.append(current.strip())
    return parts


def resolve(value, consts):
    value = value.strip()
    string = re.fullmatch(r'"([^"]*)"', value)
    if string:
        return string.group(1)
    return consts.get(value, value)


def parse_target(args, consts):
    target = {"version": None, "codes": set(), "description": None, "experimental": False}
    positional = 0
    for arg in split_args(args):
        named = re.match(r"(\w+)\s*=\s*(.+)", arg, re.S)
        name, value = (named.group(1), named.group(2)) if named else (None, arg)
        if name is None:
            name = ["version", "versionCode"][positional] if positional < 2 else None
            positional += 1
        if name == "version":
            target["version"] = resolve(value, consts)
        elif name == "versionCode":
            target["codes"].update(int(n) for n in re.findall(r"\d+", resolve(value, consts)))
        elif name == "versionCodes":
            target["codes"].update(int(n) for n in re.findall(r"\bto\s+(\d+)", value))
        elif name == "description":
            target["description"] = resolve(value, consts)
        elif name == "isExperimental":
            target["experimental"] = value.strip() == "true"
    return target


def parse_source(text):
    """[(app name, [target, ...]), ...] for each Compatibility in a Kotlin file."""
    consts = {m.group(1): m.group(2) for m in re.finditer(r'\bva[lr]\s+(\w+)\s*(?::\s*\w+)?\s*=\s*"([^"]*)"', text)}
    consts.update({m.group(1): m.group(2) for m in re.finditer(r"\bva[lr]\s+(\w+)\s*(?::\s*\w+)?\s*=\s*(\d+)\b", text)})
    apps = []
    for match in re.finditer(r"\bCompatibility\(", text):
        block = call_text(text, match.end() - 1)
        name = re.search(r'\bname\s*=\s*"([^"]+)"', block)
        if not name:
            continue
        targets = []
        for call in re.finditer(r"\b(AppTarget|devTarget)\(", block):
            target = parse_target(call_text(block, call.end() - 1), consts)
            if call.group(1) == "devTarget":
                target["dev"] = True
            if target["version"]:
                targets.append(target)
        apps.append((name.group(1), targets))
    return apps


def excluded_globs(ref):
    text = git("show", f"{ref}:patches/build.gradle.kts")
    block = re.search(r"kotlin\.exclude\((.*?)\)", text, re.S)
    return re.findall(r'"\*\*/([^"*]+)/?\*\*"', block.group(1)) if block else []


def load_targets(refs=REFS):
    """{app name: {version: {"codes", "descriptions", "experimental", "refs"}}} across refs."""
    apps = {}
    for ref in refs:
        excluded = [part.strip("/") for part in excluded_globs(ref)]
        for path in git("ls-tree", "-r", "--name-only", ref, SOURCE).split():
            if not path.endswith(".kt") or path.endswith(DEV_TARGET_HELPER):
                continue
            if any(f"/{part}/" in f"/{path}" or path.endswith(f"/{part}") for part in excluded):
                continue
            text = git("show", f"{ref}:{path}")
            if "Compatibility(" not in text:
                continue
            for name, targets in parse_source(text):
                versions = apps.setdefault(name, {})
                for target in targets:
                    entry = versions.setdefault(target["version"], {
                        "codes": set(), "descriptions": set(), "experimental": True, "refs": set()})
                    entry["codes"] |= target["codes"]
                    if target["description"]:
                        entry["descriptions"].add(target["description"])
                    # Stable when any branch lists it as a plain, non-experimental target.
                    stable = ref.endswith("main") and not target["experimental"]
                    entry["experimental"] = entry["experimental"] and not stable
                    entry["refs"].add(ref.rsplit("/", 1)[-1])
    return apps


# ── The decision ────────────────────────────────────────────────────────────

def is_tv(description):
    return bool(description) and "tv" in description.lower()


def evaluate(fields, apps):
    """None when the report matches a supported target or cannot be checked, else what is wrong."""
    app, version = fields.get("App"), fields.get("App version")
    if not app or not version or app not in apps:
        return None
    versions = apps[app]
    match = next((v for v in versions if v.lower() == version.strip().lower()), None)
    if match is None:
        return {"kind": "version", "app": app, "version": version.strip(), "versions": versions}
    entry = versions[match]
    code = (fields.get("App version code") or "").strip()
    if code.isdigit() and entry["codes"] and int(code) not in entry["codes"]:
        return {"kind": "build", "app": app, "version": match, "versions": versions,
                "reason": f"its version code is `{code}`"}
    build = fields.get("Phone or Android TV build") or ""
    descriptions = entry["descriptions"]
    if descriptions and build.startswith("Android TV") and not any(is_tv(d) for d in descriptions):
        return {"kind": "build", "app": app, "version": match, "versions": versions,
                "reason": "it is the Android TV build"}
    if descriptions and build.startswith("Phone") and all(is_tv(d) for d in descriptions):
        return {"kind": "build", "app": app, "version": match, "versions": versions,
                "reason": "it is the phone build"}
    return None


# ── The comment ─────────────────────────────────────────────────────────────

def version_line(version, entry):
    details = []
    if entry["descriptions"]:
        details.append(", ".join(sorted(entry["descriptions"])))
    if entry["codes"]:
        codes = ", ".join(f"`{c}`" for c in sorted(entry["codes"]))
        details.append(f"version code {codes}")
    if entry["refs"] == {"dev"}:
        details.append("experimental, in the dev pre-releases only")
    elif entry["experimental"]:
        details.append("experimental")
    return f"- `{version}`" + (f": {'; '.join(details)}" if details else "")


def comment(problem):
    supported = "\n".join(version_line(v, e) for v, e in sorted(problem["versions"].items()))
    if problem["kind"] == "version":
        opening = (f"The patches do not support {problem['app']} `{problem['version']}`, so this report "
                   f"cannot be looked into yet. They support these versions of {problem['app']}:")
    else:
        opening = (f"The patches support {problem['app']} `{problem['version']}`, but not the file you "
                   f"patched, because {problem['reason']}. They support these versions and builds of "
                   f"{problem['app']}:")
    return (f"{opening}\n\n{supported}\n\n"
            f"Please patch one of these and check whether the problem still happens. If it does, edit this "
            f"issue so it lists the version and build you used, and the `{LABEL}` label will be removed. "
            f"An issue with this label closes after 7 days without an update.")


# ── Acting on the issue ─────────────────────────────────────────────────────

def gh(*args):
    if os.environ.get("DRY_RUN") == "true":
        print("would run: gh " + " ".join(args))
        return
    subprocess.run(["gh", *args], check=True)


def main():
    event = json.load(open(os.environ["GITHUB_EVENT_PATH"]))
    issue = event["issue"]
    number = str(issue["number"])
    labels = {label["name"] for label in issue.get("labels", [])}
    problem = evaluate(parse_form(issue.get("body")), load_targets())
    if problem:
        print(f"#{number}: {problem['kind']} problem for {problem['app']} {problem['version']}")
        if LABEL in labels:
            return
        gh("label", "create", LABEL, "--force", "--color", LABEL_COLOR, "--description", LABEL_DESCRIPTION)
        gh("issue", "edit", number, "--add-label", LABEL)
        if os.environ.get("DRY_RUN") == "true":
            print("would comment:\n" + comment(problem))
        else:
            subprocess.run(["gh", "issue", "comment", number, "--body-file", "-"], input=comment(problem),
                           text=True, check=True)
    elif LABEL in labels:
        print(f"#{number}: the report now matches a supported target")
        gh("issue", "edit", number, "--remove-label", LABEL)
    else:
        print(f"#{number}: nothing to do")


if __name__ == "__main__":
    sys.exit(main())
