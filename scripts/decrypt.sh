#!/bin/sh
export GPG_TTY=$(tty)
ls ./src/main/resources

gpg --quiet --batch --yes --decrypt --passphrase="$APPLICATION_PROPERTIES_PASSPHRASE" \
--output ./src/main/resources/application.properties ./src/main/resources/application.properties.gpg

gpg --quiet --batch --yes --decrypt --passphrase="$APPLICATION_PROPERTIES_PASSPHRASE" \
--output ./src/main/resources/spreadsheetCredentials.json ./src/main/resources/spreadsheetCredentials.json.gpg

gpg --quiet --batch --yes --decrypt --passphrase="$APPLICATION_PROPERTIES_PASSPHRASE" \
--output ./src/main/resources/service_account.p12 ./src/main/resources/service_account.p12.gpg

ls ./src/main/resources