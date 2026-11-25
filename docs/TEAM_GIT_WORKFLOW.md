# Team GitHub workflow and release stages

This ZIP has **no Git remote**; it is safe to copy into a new repository. GitHub repo creation, invitations and PR merges are user-authorized manual tasks.

```bat
git init
git add .
git commit -m "Initial PolyglotMesh full-stack source"
git branch -M main
git remote add origin YOUR_NEW_GITHUB_URL
git push -u origin main

git branch sampreeth
git branch shagun
git branch aastha
git branch fazal
git push origin sampreeth shagun aastha fazal
```

In GitHub, invite teammates at **Settings > Collaborators** or your organization/team access screen. Create a Projects task board and import `TASK_BOARD.csv`. Protect `main` with PR reviews and CI checks.

Milestones to tag **only after successful verification**, never in advance:

```bat
git switch main
git tag -a v0.5 -m "Mid-review verified"
git push origin v0.5

git tag -a v0.7 -m "Native feasibility and metrics verified"
git push origin v0.7

git tag -a v1.0 -m "Final review verified"
git push origin v1.0
```

Use feature branches and PRs; never force-push or restore intentionally deleted work from another project. PR reviewers must actually test changes before merging. This guide is not evidence that merges or tags occurred.
