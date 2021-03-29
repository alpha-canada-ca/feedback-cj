#!/bin/sh
export GPG_TTY=$(tty)
ls ./src/main/resources
gpg --quiet --batch --yes --decrypt --passphrase="$APPLICATION_PROPERTIES_PASSPHRASE" \
--output ./src/main/resources/application.properties ./src/main/resources/application.properties.gpg
ls ./src/main/resources

mkdir config
ls -la
gpg --quiet --batch --yes --decrypt --passphrase="$APPLICATION_PROPERTIES_PASSPHRASE" \
--output ./config/config.ini config.ini.gpg
ls -la ./config