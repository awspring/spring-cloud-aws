.PHONY: format build

format:
	mvnd spring-javaformat:apply

build:
	mvnd spring-javaformat:apply verify

clean:
	mvnd clean
