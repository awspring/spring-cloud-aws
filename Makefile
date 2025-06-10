.PHONY: format build docs

format:
	mvnd spotless:apply

build:
	mvnd verify spotless:apply

clean:
	mvnd clean

docs:
	mvnd verify -Pdocs-classic -DskipTests=true

docs-full:
	mvnd verify javadoc:aggregate -Pdocs-classic -DskipTests=true
