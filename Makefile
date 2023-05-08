.PHONY: format build docs

format:
	mvnd spotless:apply

build:
	mvnd verify spotless:apply

clean:
	mvnd clean

docs:
	mvnd verify -Pdocs -DskipTests=true
