#!/usr/bin/env bash
# Stops the Jenkins started by start-jenkins.sh.
set -euo pipefail

JENKINS_HOME="${JENKINS_HOME:-${FRIDGECHEF_CI_DIR:-$HOME/.fridgechef-ci}/jenkins-home}"
PID_FILE="$JENKINS_HOME/jenkins.pid"

if [[ -f "$PID_FILE" ]] && kill -0 "$(cat "$PID_FILE")" 2>/dev/null; then
    kill "$(cat "$PID_FILE")"
    echo "Jenkins stopped."
else
    echo "Jenkins is not running."
fi
rm -f "$PID_FILE"
