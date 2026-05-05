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
2. 「アプリを追加」→ Android を選び、パッケージ名 `com.example.mcptokenviewer` を登録する
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

### USB デバッグで直接インストール（最速）

```bash
./gradlew installDebug
```

### Firebase App Distribution でテスターへ配布

```bash
# リリース APK をビルドしてアップロード
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
├── local.properties                  # 署名情報（Git 管理外）
└── gradle.properties                 # JVM / Gradle 設定
```

## 注意事項

- `local.properties` と `app/keystore/` は `.gitignore` により Git 管理外です
- token はアプリ再インストールや端末移行で再生成されることがあります
- 古い token は FCM 送信失敗の原因になるため、変更時は FCM 送信クライアント側の設定も更新してください
