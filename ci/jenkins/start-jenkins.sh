#!/usr/bin/env bash
# Starts a local Jenkins for FridgeChef in the background.
# Everything (plugins, credentials, tools, jobs) comes from this directory, so no setup wizard is needed.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"

JENKINS_VERSION="${JENKINS_VERSION:-2.568.3}"
PLUGIN_MANAGER_VERSION="${PLUGIN_MANAGER_VERSION:-2.15.0}"
DIST_DIR="${JENKINS_DIST_DIR:-$HOME/tools/jenkins}"
SECRETS_DIR="${FRIDGECHEF_CI_DIR:-$HOME/.fridgechef-ci}"

export JENKINS_HOME="${JENKINS_HOME:-$SECRETS_DIR/jenkins-home}"
export JENKINS_PORT="${JENKINS_PORT:-8090}"
export JDK21_HOME="${JDK21_HOME:-/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home}"
export SONAR_HOST_URL="${SONAR_HOST_URL:-http://localhost:9000}"
export FRIDGECHEF_REPO_URL="${FRIDGECHEF_REPO_URL:-$REPO_DIR}"
export CASC_JENKINS_CONFIG="$SCRIPT_DIR/casc.yaml"

JAVA="$JDK21_HOME/bin/java"
WAR="$DIST_DIR/jenkins-$JENKINS_VERSION.war"
PLUGIN_MANAGER="$DIST_DIR/jenkins-plugin-manager-$PLUGIN_MANAGER_VERSION.jar"
PID_FILE="$JENKINS_HOME/jenkins.pid"
LOG_FILE="$JENKINS_HOME/jenkins.log"

if [[ -f "$PID_FILE" ]] && kill -0 "$(cat "$PID_FILE")" 2>/dev/null; then
    echo "Jenkins is already running (pid $(cat "$PID_FILE")) at http://localhost:$JENKINS_PORT/"
    exit 0
fi

mkdir -p "$DIST_DIR" "$JENKINS_HOME/plugins"
chmod 700 "$SECRETS_DIR"

if [[ ! -f "$WAR" ]]; then
    echo "Downloading Jenkins $JENKINS_VERSION..."
    curl -fsSL -o "$WAR" "https://get.jenkins.io/war-stable/$JENKINS_VERSION/jenkins.war"
fi
if [[ ! -f "$PLUGIN_MANAGER" ]]; then
    echo "Downloading jenkins-plugin-manager $PLUGIN_MANAGER_VERSION..."
    curl -fsSL -o "$PLUGIN_MANAGER" \
        "https://github.com/jenkinsci/plugin-installation-manager-tool/releases/download/$PLUGIN_MANAGER_VERSION/jenkins-plugin-manager-$PLUGIN_MANAGER_VERSION.jar"
fi

echo "Installing plugins..."
"$JAVA" -jar "$PLUGIN_MANAGER" \
    --war "$WAR" \
    --plugin-download-directory "$JENKINS_HOME/plugins" \
    --plugin-file "$SCRIPT_DIR/plugins.txt"

# Secrets live outside the repository
if [[ ! -f "$SECRETS_DIR/jenkins-admin-password" ]]; then
    (umask 077 && openssl rand -base64 18 > "$SECRETS_DIR/jenkins-admin-password")
fi
if [[ ! -f "$SECRETS_DIR/sonar-token" ]]; then
    echo "Missing $SECRETS_DIR/sonar-token: generate a token in SonarQube (My Account > Security) and save it there." >&2
    exit 1
fi
JENKINS_ADMIN_PASSWORD="$(cat "$SECRETS_DIR/jenkins-admin-password")"
SONAR_TOKEN="$(cat "$SECRETS_DIR/sonar-token")"
export JENKINS_ADMIN_PASSWORD SONAR_TOKEN

echo "Starting Jenkins on port $JENKINS_PORT..."
nohup "$JAVA" \
    -Djenkins.install.runSetupWizard=false \
    -Dhudson.plugins.git.GitSCM.ALLOW_LOCAL_CHECKOUT=true \
    -jar "$WAR" --httpPort="$JENKINS_PORT" \
    > "$LOG_FILE" 2>&1 &
echo $! > "$PID_FILE"

echo "Jenkins is starting: http://localhost:$JENKINS_PORT/ (user: admin, password: $SECRETS_DIR/jenkins-admin-password)"
echo "Log: $LOG_FILE"
