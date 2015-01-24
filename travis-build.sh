#!/bin/bash -x
if [[ ($TRAVIS_BRANCH =~ ^.*\/.*$) || ($TRAVIS_PULL_REQUEST != false) ]]
  then
  	mvn --settings .settings.xml test -q -U -Dmaven.test.redirectTestOutputToFile=true
  else
    mvn --settings .settings.xml install -P docs -q -U -DskipTests=true -Dmaven.test.redirectTestOutputToFile=true
	./docs/src/main/asciidoc/ghpages.sh
	mvn --settings .settings.xml deploy -nsu -Dmaven.test.redirectTestOutputToFile=true
fi