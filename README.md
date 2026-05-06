# MCPTokenViewer

Firebase Cloud Messaging (FCM) の registration token を Android 実機で取得するための最小構成アプリです。

## 動作概要

アプリを起動すると FCM registration token を取得し、画面中央に表示します。
token は長押しでコピーできます。Logcat でも確認できます。

```
adb logcat | grep registration_token=
```

## 前提条件

| ツール | バージョン |
|--------|-----------|
| Android Studio | 最新版 |
| JDK | 21 |
| Node.js | >= 20（Firebase CLI 用） |
| Firebase CLI | 最新版（`npm install -g firebase-tools`） |

## セットアップ

### 1. google-services.json を配置する

Firebase コンソール（<https://console.firebase.google.com>）で以下の手順を実施し、
`google-services.json` をダウンロードして `app/` ディレクトリに配置します。

1. 「プロジェクトを追加」で Firebase プロジェクトを作成する
2. 「アプリを追加」→ Android を選び、パッケージ名 `com.example.fcm_token_viewer` を登録する
3. `google-services.json` をダウンロードして `app/` に配置する
4. Firebase コンソールの「プロジェクトの設定」で **Firebase Cloud Messaging API** が有効になっていることを確認する

### 2. リリース署名キーストアを生成する（初回のみ）

```bash
mkdir -p app/keystore
keytool -genkeypair -v \
  -keystore app/keystore/release.jks \
  -alias mcptokenviewer \
  -keyalg RSA -keysize 2048 -validity 9125 \
  -storepass <パスワード> \
  -keypass <パスワード> \
  -dname "CN=MCPTokenViewer, OU=Dev, O=Personal, L=Tokyo, ST=Tokyo, C=JP"
```

### 3. local.properties に署名情報を追記する

`local.properties`（Git 管理外）に以下を追加します。

```properties
RELEASE_STORE_FILE=keystore/release.jks
RELEASE_STORE_PASSWORD=<パスワード>
RELEASE_KEY_ALIAS=mcptokenviewer
RELEASE_KEY_PASSWORD=<パスワード>
```

### 4. Firebase CLI でログインする

```bash
firebase login
```

## ビルドと配布

### セットアップスクリプト（推奨）

ステップ 2〜4 と配布までを一括で行うスクリプトを用意しています。

```bash
bash scripts/setup_fcm_token_app.sh
```

#### 実行内容

1. JBR（Android Studio 同梱 JDK）と TLS/IPv4 系の JVM オプションを設定
2. Firebase CLI のインストール確認・ログイン確認
3. 署名キーストアの生成（初回のみ）
4. `local.properties` への署名情報書き込み
5. `sdkmanager` が利用可能な場合は SDK ライセンス確認と必要パッケージ導入
6. リリース APK のビルド
7. Firebase App Distribution へのアップロード

### USB デバッグで直接インストール（最速）

```bash
./gradlew installDebug
```

### Firebase App Distribution でテスターへ配布

```bash
# リリース APK をビルドしてアップロード
./gradlew assembleRelease appDistributionUploadRelease
```

ネットワーク環境によっては TLS 周りの問題を避けるため、次の実行例を使うと安定します。

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
JAVA_TOOL_OPTIONS="-Djava.net.preferIPv4Stack=true -Dhttps.protocols=TLSv1.2,TLSv1.3 -Djdk.tls.client.protocols=TLSv1.2,TLSv1.3" \
./gradlew assembleRelease appDistributionUploadRelease
```

テスター（`esashika.kento@icloud.com`）に招待メールが届きます。
アプリをインストールして token を取得してください。

テスターや release notes を変更する場合は `app/build.gradle.kts` の
`firebaseAppDistribution` ブロックを編集してください。

```kotlin
firebaseAppDistribution {
    artifactType = "APK"
    testers = "example@example.com"
    releaseNotes = "FCM token viewer build"
}
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

```
MCPTokenViewer/
├── app/
│   ├── google-services.json          # Firebase 設定（要配置）
│   ├── keystore/
│   │   └── release.jks               # 署名キー（Git 管理外）
│   ├── build.gradle.kts              # Firebase / App Distribution 設定
│   └── src/main/java/.../
│       └── MainActivity.kt           # token 取得・表示
├── gradle/
│   └── libs.versions.toml            # 依存バージョン管理
├── scripts/
│   └── setup_fcm_token_app.sh        # ビルド〜App Distribution 配布の一括スクリプト
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

```bash
./gradlew assembleRelease
```

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
