#!/bin/sh
export GPG_TTY=$(tty)
ls -la
gpg --quiet --batch --yes --decrypt --passphrase="$CONFIG_INI_PASSPHRASE" \
--output ./src/main/java/resources/application.properties ./src/main/java/resources/application.properties.gpg
ls -la