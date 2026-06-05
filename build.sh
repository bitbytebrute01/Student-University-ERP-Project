#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
export HOME=/Users/adityajoshi/Desktop/Student-University-ERP-Project
mvn -s settings.xml clean compile
