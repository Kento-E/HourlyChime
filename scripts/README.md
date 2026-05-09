# scripts/setup_fcm_token_app.sh

ビルド環境のセットアップから Firebase App Distribution への配布まで、  
下記の手順を一括で行うスクリプトです。

```bash
cd /path/to/HourlyChime
bash scripts/setup_fcm_token_app.sh
```

## 前提条件

| ツール | バージョン |
| ------ | ---------- |
| Android Studio | 最新版（JBR を同梱） |
| Node.js | >= 20（Firebase CLI 用） |
| `app/google-services.json` | 配置済みであること（→ プロジェクト README 参照） |

## スクリプトが行うこと

### ステップ 1 — Firebase CLI の確認

インストール済みかどうかを確認し、なければ npm で導入します。

### ステップ 2 — Firebase ログイン状態の確認

`firebase login` が実行済みかどうかを確認し、未実行ならブラウザでログインさせます。

### ステップ 3 — リリース署名キーストアの生成（初回のみ）

`app/keystore/release.jks` が存在しない場合に `keytool` で生成します。  
既に存在する場合は生成をスキップし、パスワードのみ入力を求めます。

手動で生成する場合は以下を実行します。

```bash
mkdir -p app/keystore
keytool -genkeypair -v \
  -keystore app/keystore/release.jks \
  -alias hourlychime \
  -keyalg RSA -keysize 2048 -validity 9125 \
  -storepass <パスワード> \
  -keypass <パスワード> \
  -dname "CN=HourlyChime, OU=Dev, O=Personal, L=Tokyo, ST=Tokyo, C=JP"
```

### ステップ 4 — local.properties への署名情報書き込み

`RELEASE_STORE_FILE` が未記載であれば以下の4行を `local.properties` に追記します。  
既に記載済みの場合はパスワード行のみ上書きします。

```properties
RELEASE_STORE_FILE=keystore/release.jks
RELEASE_STORE_PASSWORD=<パスワード>
RELEASE_KEY_ALIAS=hourlychime
RELEASE_KEY_PASSWORD=<パスワード>
```

> `local.properties` は `.gitignore` により Git 管理外です。

### ステップ 5 — Android SDK ライセンスと必要パッケージ確認

`sdkmanager` が利用可能な場合、SDK ライセンス受諾と必要パッケージ（platform-tools, platforms;android-36, build-tools;36.0.0）の導入を行います。

### ステップ 6 — リリース APK ビルド

```bash
./gradlew assembleRelease
```

### ステップ 7 — Firebase App Distribution へアップロード

```bash
./gradlew appDistributionUploadRelease
```

この Gradle タスクは内部で Firebase CLI を呼び出します。  
そのため事前に `firebase login` が完了している必要があります。

## JVM ネットワーク設定について

スクリプトは Android Studio 同梱の JBR を自動で使用し、  
TLS/IPv4 周りの安定化オプションを設定します。

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
JAVA_TOOL_OPTIONS="-Djava.net.preferIPv4Stack=true -Dhttps.protocols=TLSv1.2,TLSv1.3 ..."
```

手動でビルド・配布する場合はこれらを明示的に指定してください（→ プロジェクト README 参照）。
