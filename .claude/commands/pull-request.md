# PR 스킬

현재 브랜치의 변경 사항을 분석하고 PR을 생성한다.

## 실행 순서

1. `git status`, `git log dev..HEAD`, `git diff dev...HEAD`를 실행해 변경 내용과 커밋 목록을 파악한다.
2. 현재 브랜치명에서 이슈번호를 추출한다. (예: `fix/#341-record-hobby-create` → `#341`)
3. base 브랜치를 결정한다:
   - `feat/*`, `fix/*` 등 작업 브랜치 → `dev`
   - `dev` → `release`, `release` → `main` (배포 PR인 경우에만)
   - 기본값은 `dev`이며, 그 외로 보낼 때는 사용자에게 먼저 확인한다.
4. `.github/PULL_REQUEST_TEMPLATE.md`를 읽어 PR 본문 템플릿을 확인한 뒤, 해당 양식(`📄 작업 내용 요약`, `📎 Issue 번호` 등)에 맞춰 초안을 채워 사용자에게 보여주고 확인받는다. Issue 번호 항목은 `closed #번호` 형식으로 채운다.
5. 확인 후 `gh pr create --base <base브랜치>`로 PR을 생성한다.
6. 생성된 PR URL을 출력한다.

## 규칙

- PR 제목은 커밋 컨벤션과 동일한 형식으로 작성한다: `[#이슈번호] prefix: 작업내용`
- 본문은 한국어로 작성한다.
- 브랜치명에서 이슈번호를 찾을 수 없으면 사용자에게 직접 물어본다.
- 원격 브랜치가 없으면 `git push -u origin <브랜치명>`으로 먼저 푸시한다.
- PR을 생성하기 전에 반드시 사용자에게 초안을 보여주고 확인받는다.
- `main`으로 직접 PR을 여는 것은 배포를 트리거하므로(`.github/workflows/deploy.yml`이 `main` push에서 블루-그린 배포 실행) 사용자가 명시적으로 요청한 경우에만 한다.

## 실행

위 순서대로 바로 실행한다.
