# HourlyChime

曜日・時刻・祝日スキップ設定で毎正時に通知する時報 Android アプリです。
FCM registration token の取得・表示機能も搭載しています（開発者向けサブ機能）。

## 動作概要

アプリを起動すると「時報設定」タブで時報の ON/OFF・曜日・時刻範囲・祝日スキップを設定できます。
設定を保存すると次の正時にシステム通知として時報が届きます。

「FCM Token」タブでは FCM registration token を取得・表示できます。
token は長押しでコピーできます。Logcat でも確認できます。

```bash
adb logcat | grep registration_token=
```

## 前提条件

| ツール | バージョン |
| ------ | ---------- |
| Android Studio | 最新版 |
| JDK | 21 |
| Node.js | >= 20（Firebase CLI 用） |
| Firebase CLI | 最新版（`npm install -g firebase-tools`） |

## セットアップ

### 1. google-services.json を配置する

Firebase コンソール（<https://console.firebase.google.com>）で以下の手順を実施し、
`google-services.json` をダウンロードして `app/` ディレクトリに配置します。

1. 「プロジェクトを追加」で Firebase プロジェクトを作成する
2. 「アプリを追加」→ Android を選び、パッケージ名 `com.example.hourlychime` を登録する
3. `google-services.json` をダウンロードして `app/` に配置する
4. Firebase コンソールの「プロジェクトの設定」で **Firebase Cloud Messaging API** が有効になっていることを確認する

### 2. セットアップスクリプトを実行する

署名キーストアの生成・`local.properties` への署名情報書き込み・Firebase CLI ログイン確認から  
App Distribution への配布まで、以下のスクリプトで一括実行できます。

```bash
bash scripts/setup_fcm_token_app.sh
```

各ステップの詳細は [scripts/README.md](scripts/README.md) を参照してください。

手動で進める場合も同ファイルに手順をまとめています。

### USB デバッグで直接インストール（最速）

```bash
./gradlew installDebug
```

### Firebase App Distribution でテスターへ配布

ワンコマンドで Firebase App Distribution へリリース APK をビルド・配布できます:

```bash
# セットアップスクリプトで一括処理（初回推奨）
bash scripts/setup_fcm_token_app.sh

# セットアップ済みなら以下のいずれかで実行可能
./gradlew assembleRelease appDistributionUploadRelease  # Gradle プラグイン経由
# または
firebase appdistribution:distribute app/build/outputs/apk/release/app-release.apk \
  --app 1:1052281628451:android:3d20d418e11e45e48f0e96 \
  --testers esashika.kento@icloud.com
```

ネットワーク環境によっては TLS 周りの問題を避けるため、次の実行例を使うと安定します。

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
JAVA_TOOL_OPTIONS="-Djava.net.preferIPv4Stack=true -Dhttps.protocols=TLSv1.2,TLSv1.3 -Djdk.tls.client.protocols=TLSv1.2,TLSv1.3" \
./gradlew assembleRelease appDistributionUploadRelease
```

テスター（`esashika.kento@icloud.com`）に招待メールが届きます。
アプリをインストールして token を取得してください。

### GitHub Actions で自動配布

PR のマージ（`pull_request: closed` / Auto Merge完了）または手動実行（`workflow_dispatch`）で、
ユニットテスト実行後に Firebase App Distribution へ自動配布します。

ワークフロー定義: `.github/workflows/android-distribute.yml`

#### GitHub Variables（非秘密）

Repository Variables として設定:
- `RELEASE_KEY_ALIAS`: 署名キーのエイリアス（例: `hourlychime`）

#### GitHub Secrets（秘密）

Repository Secrets として設定:
- `GOOGLE_SERVICES_JSON`: `app/google-services.json` の内容（生JSONまたはBase64）
- `RELEASE_KEYSTORE_BASE64`: `app/keystore/release.jks` の Base64
- `RELEASE_STORE_PASSWORD`: 署名キーストアのパスワード
- `RELEASE_KEY_PASSWORD`: 署名キーのパスワード
- `FIREBASE_TOKEN`: `firebase login:ci` で発行したトークン

`RELEASE_KEYSTORE_BASE64` は以下で作成できます。

```bash
base64 -i app/keystore/release.jks | pbcopy
```

 GitHub CLI で設定する場合:

```bash
gh variable set RELEASE_KEY_ALIAS --body "hourlychime"
gh secret set GOOGLE_SERVICES_JSON < app/google-services.json
gh secret set RELEASE_KEYSTORE_BASE64 < app/keystore/release.jks.b64
gh secret set RELEASE_STORE_PASSWORD
gh secret set RELEASE_KEY_PASSWORD
gh secret set FIREBASE_TOKEN
```

#### 認証情報の設定

- **初回のみ**: `firebase login` で Firebase CLI にログインしてください
- 詳細は [scripts/README.md](scripts/README.md) を参照してください

テスターや release notes を変更する場合は `app/build.gradle.kts` の
アップロードタスク定義を編集してください。

```kotlin
val firebaseAppId = "1:1052281628451:android:xxxxxxxxxxxxxxxx"
val firebaseTesters = "example@example.com"
val firebaseReleaseNotes = "Updated features"
```

## token の取得

1. アプリを起動する
2. 画面中央に表示された token をコピーする（長押し）
3. 取得した token を FCM 送信クライアントの設定に貼り付ける

Logcat で確認する場合:

```bash
adb logcat | grep registration_token=
```

> token はアプリ再インストールや端末移行で再生成されます。変更時は送信クライアント側の設定も更新してください。

## プロジェクト構成

```text
HourlyChime/
├── app/
│   ├── google-services.json          # Firebase 設定（要配置）
│   ├── keystore/
│   │   └── release.jks               # 署名キー（Git 管理外）
│   ├── build.gradle.kts              # Firebase / App Distribution 設定
│   └── src/main/java/com/example/hourlychime/
│       ├── MainActivity.kt           # タブUI・FCM token取得
│       ├── TimeSignalScreen.kt       # 時報設定UI
│       ├── TimeSignalScheduler.kt    # AlarmManager スケジューリング
│       ├── TimeSignalReceiver.kt     # アラーム受信・通知発行
│       ├── TimeSignalSettings.kt     # 設定データ・SharedPreferences
│       ├── HolidayRepository.kt      # 祝日データ取得・キャッシュ
│       ├── NotificationHelper.kt     # 通知チャンネル・通知発行
│       └── BootReceiver.kt           # 端末再起動後の復元
├── gradle/
│   └── libs.versions.toml            # 依存バージョン管理
├── scripts/
│   ├── setup_fcm_token_app.sh        # ビルド〜App Distribution 配布の一括スクリプト
│   └── README.md                     # スクリプト詳細
├── local.properties                  # 署名情報（Git 管理外）
└── gradle.properties                 # JVM / Gradle 設定
```

## 注意事項

- `local.properties` と `app/keystore/` は `.gitignore` により Git 管理外です
- token はアプリ再インストールや端末移行で再生成されることがあります
- 古い token は FCM 送信失敗の原因になるため、変更時は FCM 送信クライアント側の設定も更新してください

## トラブルシュート

### 1. `Plugin com.android.application was not found` が出る

- `./gradlew ... | tail -20` のようなパイプ付きコマンドは、Gradle の失敗が見えにくくなることがあります。
- まずはパイプなしで実行して正しい終了コードを確認してください。
- FW / egress 制限で `dl.google.com` に到達できない環境では、Google Maven のミラーURLを指定してください。

```bash
./gradlew assembleRelease
```

```bash
# 例: 事前に用意した Maven ミラーを使う
export GOOGLE_MAVEN_MIRROR_URLS="https://nexus.example.com/repository/google-mirror"

# direct の Google Maven を使えない環境では true にする
export DISABLE_DIRECT_GOOGLE_MAVEN=true

./gradlew --no-daemon help
```

- `GOOGLE_MAVEN_MIRROR_URLS` はカンマ区切りで複数指定できます。
- `DISABLE_DIRECT_GOOGLE_MAVEN=true` を指定する場合は `GOOGLE_MAVEN_MIRROR_URLS` も必ず指定してください。
- `disableDirectGoogleMaven=true`（Gradle property）または `DISABLE_DIRECT_GOOGLE_MAVEN=true`（環境変数）かつ、`googleMavenMirrorUrls` / `GOOGLE_MAVEN_MIRROR_URLS` 未指定の場合は `settings.gradle.kts` で fail-fast し、ミラー未指定の旨を明示して停止します。

### 2. `AppDistribution` プラグイン適用時に `AppExtension does not exist` が出る

- Firebase App Distribution プラグインのバージョンを最新系に更新します。
- このプロジェクトでは `gradle/libs.versions.toml` の `firebaseAppDistribution` を `5.2.1` にしています。

### 3. `SDK license not accepted` / `build-tools;36.0.0` がない

Android SDK Command-line Tools を最新化し、ライセンス受諾と必要パッケージ導入を行ってください。

```bash
SDK="$HOME/Library/Android/sdk"
curl -fsL "https://dl.google.com/android/repository/commandlinetools-mac-14742923_latest.zip" -o /tmp/commandlinetools-mac-latest.zip
rm -rf /tmp/cmdline-tools-extract "$SDK/cmdline-tools/latest"
unzip -q /tmp/commandlinetools-mac-latest.zip -d /tmp/cmdline-tools-extract
mv /tmp/cmdline-tools-extract/cmdline-tools "$SDK/cmdline-tools/latest"

yes | "$SDK/cmdline-tools/latest/bin/sdkmanager" --licenses
"$SDK/cmdline-tools/latest/bin/sdkmanager" --install "platform-tools" "platforms;android-36" "build-tools;36.0.0"
```
