#!/usr/bin/env bash
# setup_fcm_token_app.sh
#
# HourlyChime Android アプリのビルド環境をセットアップし、
# Firebase App Distribution へリリース APK をアップロードするスクリプト。
#
# 前提条件:
#   - Android Studio がインストール済み（ANDROID_HOME が設定されていること）
#   - Node.js >= 20 (Firebase CLI のため)
#   - firebase login が完了していること
#   - app/google-services.json が配置済みであること
#
# 使用方法:
#   cd /path/to/HourlyChime
#   bash scripts/setup_fcm_token_app.sh

set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
KEYSTORE_DIR="$PROJECT_DIR/app/keystore"
KEYSTORE_FILE="$KEYSTORE_DIR/release.jks"
KEY_ALIAS="hourlychime"
LOCAL_PROPS="$PROJECT_DIR/local.properties"
DEFAULT_JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"

echo "=== FCM Token Viewer セットアップ ==="
echo "プロジェクト: $PROJECT_DIR"

# ---- 0. JVM 実行設定（ネットワーク安定化） ----
if [ -d "$DEFAULT_JAVA_HOME" ] && [ -z "${JAVA_HOME:-}" ]; then
    export JAVA_HOME="$DEFAULT_JAVA_HOME"
fi
export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:--Djava.net.preferIPv4Stack=true -Dhttps.protocols=TLSv1.2,TLSv1.3 -Djdk.tls.client.protocols=TLSv1.2,TLSv1.3}"

if [ -n "${JAVA_HOME:-}" ]; then
    echo "[OK] JAVA_HOME=$JAVA_HOME"
fi
echo "[OK] JAVA_TOOL_OPTIONS=$JAVA_TOOL_OPTIONS"

# ---- 1. Firebase CLI の確認 ----
if ! command -v firebase &>/dev/null; then
    echo "[INFO] Firebase CLI が見つかりません。インストールします..."
    npm install -g firebase-tools
fi
echo "[OK] Firebase CLI: $(firebase --version)"

# ---- 2. ログイン状態の確認 ----
if ! firebase projects:list &>/dev/null 2>&1; then
    echo "[INFO] Firebase にログインしてください..."
    firebase login
fi

# ---- 3. キーストアの生成（未存在の場合のみ） ----
if [ ! -f "$KEYSTORE_FILE" ]; then
    echo "[INFO] キーストアを生成します..."
    mkdir -p "$KEYSTORE_DIR"

    read -rp "キーストアのパスワードを入力してください: " STORE_PASS
    read -rp "キーのパスワードを入力してください (Enterで同じパスワードを使用): " KEY_PASS
    KEY_PASS="${KEY_PASS:-$STORE_PASS}"

    keytool -genkeypair -v \
        -keystore "$KEYSTORE_FILE" \
        -alias "$KEY_ALIAS" \
        -keyalg RSA -keysize 2048 -validity 9125 \
        -storepass "$STORE_PASS" \
        -keypass "$KEY_PASS" \
        -dname "CN=HourlyChime, OU=Dev, O=Personal, L=Tokyo, ST=Tokyo, C=JP"

    echo "[OK] キーストア生成完了: $KEYSTORE_FILE"
else
    echo "[OK] キーストアは既に存在します: $KEYSTORE_FILE"
    read -rsp "キーストアのパスワードを入力してください: " STORE_PASS; echo
    KEY_PASS="$STORE_PASS"
fi

# ---- 4. local.properties への署名情報書き込み ----
# 既存エントリを上書きしないよう sdk.dir 行以外を確認してから追記
if ! grep -q "RELEASE_STORE_FILE" "$LOCAL_PROPS" 2>/dev/null; then
    cat >> "$LOCAL_PROPS" <<EOF
RELEASE_STORE_FILE=keystore/release.jks
RELEASE_STORE_PASSWORD=$STORE_PASS
RELEASE_KEY_ALIAS=$KEY_ALIAS
RELEASE_KEY_PASSWORD=$KEY_PASS
EOF
    echo "[OK] local.properties に署名情報を追記しました"
else
    # パスワードを更新
    sed -i '' \
        -e "s|^RELEASE_STORE_PASSWORD=.*|RELEASE_STORE_PASSWORD=$STORE_PASS|" \
        -e "s|^RELEASE_KEY_PASSWORD=.*|RELEASE_KEY_PASSWORD=$KEY_PASS|" \
        "$LOCAL_PROPS"
    echo "[OK] local.properties のパスワードを更新しました"
fi

# ---- 5. ビルド ----
# ---- 5a. Android SDK ライセンスと必要パッケージ確認 ----
SDK_DIR=""
if [ -n "${ANDROID_SDK_ROOT:-}" ]; then
    SDK_DIR="$ANDROID_SDK_ROOT"
elif [ -n "${ANDROID_HOME:-}" ]; then
    SDK_DIR="$ANDROID_HOME"
elif [ -f "$LOCAL_PROPS" ]; then
    SDK_DIR="$(grep '^sdk.dir=' "$LOCAL_PROPS" | head -1 | cut -d'=' -f2- | sed 's#\\:#:#g')"
fi

if [ -n "$SDK_DIR" ] && [ -x "$SDK_DIR/cmdline-tools/latest/bin/sdkmanager" ]; then
    echo "[INFO] SDK ライセンスを確認します..."
    yes | "$SDK_DIR/cmdline-tools/latest/bin/sdkmanager" --licenses >/dev/null || true
    echo "[INFO] 必要 SDK パッケージを確認します..."
    "$SDK_DIR/cmdline-tools/latest/bin/sdkmanager" --install \
        "platform-tools" \
        "platforms;android-36" \
        "build-tools;36.0.0" >/dev/null || true
else
    echo "[WARN] sdkmanager が見つからないため SDK 準備をスキップします（Android Studio 側で不足分を導入してください）。"
fi

# ---- 5b. ビルド ----
echo "[INFO] リリース APK をビルドします..."
cd "$PROJECT_DIR"
./gradlew assembleRelease

APK_PATH=$(find "$PROJECT_DIR/app/build/outputs/apk/release" -name "*.apk" | head -1)
if [ -z "$APK_PATH" ]; then
    echo "[ERROR] APK が見つかりません。ビルドログを確認してください。"
    exit 1
fi
echo "[OK] APK: $APK_PATH"

# ---- 6. Firebase App Distribution へアップロード ----
echo "[INFO] Firebase App Distribution へアップロードします..."
./gradlew appDistributionUploadRelease

echo ""
echo "=== 完了 ==="
echo "テスターに招待メールが送信されます。"
echo "アプリをインストール後、画面に表示された FCM token を"
echo "config/config.yaml の firebase.registration_tokens に設定してください。"
