# Copilot コーディング指示

## RTK 連携

- シェルコマンドを実行する際は、可能な範囲で `rtk` プレフィックスを利用する。
  - 例: `rtk git status`, `rtk git log -10`, `rtk gradlew test`
- 生コマンドが必要な場合は `rtk proxy <command>` を利用する。
- 効果確認には `rtk gain` と `rtk gain --history` を利用する。

## 言語

- ユーザーへの応答は原則として日本語で行う。
