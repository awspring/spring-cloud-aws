.PHONY: format build

format:
	mvnd spring-javaformat:apply

build:
	mvnd spring-javaformat:apply verify spring-javaformat:apply

clean:
	mvnd clean
