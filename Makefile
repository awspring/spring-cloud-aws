.PHONY: format build docs

format:
	mvnd spotless:apply

build:
	mvnd spotless:apply verify

clean:
	mvnd clean

docs:
	mvnd verify -Pdocs -DskipTests=true
