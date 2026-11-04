# Pushing HeroWalls to GitHub

The connected GitHub App on this machine does not have the **Create repository** permission,
so the repo has to exist before it can be pushed. Once it exists, the push is one command.

## 1. Create the empty repo (20 seconds, one click)

Open <https://github.com/new> and fill in:

* **Repository name:** `HeroWalls`
* **Visibility:** Public
* **Do NOT** add a README, .gitignore or licence (the project already has them)

Click **Create repository**. Leave the page open — it shows the push commands.

## 2. Push this project

```bash
cd HeroWalls
git remote add origin https://github.com/technicalnihilist-lgtm/HeroWalls.git
git push -u origin main
```

The project is already a git repository with one commit, so no `git init` or `git add` is needed.

## 3. What happens automatically after the push

Two GitHub Actions workflows start on their own:

| Workflow | What it does |
|---|---|
| **Build APK** | Runs the licence unit tests, builds `app-debug.apk` on GitHub's runner, uploads it as an artifact **and attaches it to a `v1.0.0` release** on the repo |
| **Screenshots** | Boots an Android 33 emulator, installs the APK, walks the app (home → collections → detail → search → saved → offline), captures real screenshots into `docs/screenshots/` and commits them |

The APK then lives at `Releases → v1.0.0 → app-debug.apk` and the screenshots appear in the repo.

You can also re-run either workflow by hand from the **Actions** tab (`workflow_dispatch`).
