# Social Graph Engine - Submission Ready Guide

Use this checklist to finalize your team submission quickly and avoid grading deductions.

## 1) Fill report metadata

Open `docs/REPORT.md` and complete:

- Section **1. Team Info** (all member names + emails)
- Verify Section **3. Dataset Information** still reflects your final run
- Keep the final counts as:
  - `Users: 4039`
  - `FOLLOWS: 176468`

## 2) Add all 11 screenshots

Place screenshots in `docs/screenshots/` with these exact names:

- `uc01_register.png`
- `uc02_login.png`
- `uc03_view_profile.png`
- `uc04_edit_profile.png`
- `uc05_follow.png`
- `uc06_unfollow.png`
- `uc07_following_followers.png`
- `uc08_mutual.png`
- `uc09_recommendations.png`
- `uc10_search.png`
- `uc11_popular.png`

Tip: each screenshot should show the console heading for that UC (e.g. `[UC-5] Follow another user`) and the successful result text.

## 3) Export report.pdf

Export `docs/REPORT.md` to `report.pdf`.

Any method is fine (VS Code extension, pandoc, or copy to Google Docs and export), as long as:

- all screenshots render in order,
- cypher blocks are visible,
- formatting is readable.

## 4) Prepare source-code folder

Make sure your source folder includes:

- `pom.xml`
- `README.md`
- `.env.example`
- `src/`
- `docs/REPORT.md`
- `docs/screenshots/`
- `facebook/` (unless your instructor says they already have the dataset)

Do **not** include:

- `.env`
- `.m2/`
- `tools/`
- `target/`
- IDE/system files

## 5) Build final `projects.zip`

Required by rubric: one zip containing `report.pdf` and source code folder.

Recommended final layout:

```text
projects.zip
├── report.pdf
└── source-code/
    ├── pom.xml
    ├── README.md
    ├── .env.example
    ├── src/
    ├── docs/
    └── facebook/
```

## 6) Final pre-submit checks

- Application runs:
  - loader: `--load --reset`
  - app: login + use case menu
- Report includes all 11 UC screenshots
- Team member information is complete
- No secrets are present (`.env` excluded)
- Zip opens correctly and contains exactly expected files

## 7) Optional one-line self-test notes (for your team)

You can add a short note in your report conclusion:

- Neo4j Aura connection verified
- Dataset loaded successfully (`4039` users, `176468` follows)
- All 11 use cases demonstrated via Java console UI

