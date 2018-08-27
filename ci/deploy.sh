#!/usr/bin/env bash
if [ "${TRAVIS_PULL_REQUEST}" == "false" ]; then
        openssl aes-256-cbc -K ${encrypted_4d1347a249a5_key} -iv ${encrypted_4d1347a249a5_iv} -in ci/cikey.asc.enc -out ci/cikey.asc -d;
        gpg --fast-import ci/cikey.asc;
        mvn clean deploy -P !develop,sign,build-extras --settings ci/mvn-settings.xml;
    else
        echo "skip deploy";
fi