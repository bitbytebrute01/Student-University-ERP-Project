#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

# Compile and Run using Maven
mvn clean compile
mvn exec:java -Dexec.mainClass="com.university.main.UniversityERP"
