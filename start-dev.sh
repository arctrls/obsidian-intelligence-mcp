#!/bin/bash
VAULT_PATH="${VAULT_PATH:-$HOME/obsidian}"
PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$PROJECT_DIR"
VAULT_PATH="$VAULT_PATH" exec ./gradlew bootRun --args='--spring.profiles.active=dev'
