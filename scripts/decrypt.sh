#!/bin/sh
export GPG_TTY=$(tty)

ls ./src/main/resources

gpg --quiet --batch --yes  --passphrase="$APPLICATION_PROPERTIES_PASSPHRASE" --output ./src/main/resources/application.properties --decrypt ./src/main/resources/application.properties.gpg

gpg --quiet --batch --yes  --passphrase="$APPLICATION_PROPERTIES_PASSPHRASE" --output ./src/main/resources/service_account.p12 --decrypt ./src/main/resources/service_account.p12.gpg

ls ./src/main/resources