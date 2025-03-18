MVN = mvn
ARGS =
CLASS =

all: clean compile test package

clean:
	$(MVN) clean

compile:
	$(MVN) compile

test:
	$(MVN) test

package:
	$(MVN) package

transpile:
	java -jar target/oblivious-java-parser-1.0-SNAPSHOT.jar $(ARGS)

# Compile space-separated list of class names in CLASS env var from src/main/java/com/example
javac:
	javac $(foreach class, $(CLASS), src/main/java/com/example/$(class).java)

run:
	java -cp src/main/java $(CLASS) $(ARGS)

TIMESTAMP := $(shell date +%Y%m%d_%H%M%S)

run-timed:
	time make run CLASS="$(CLASS)" ARGS="$(ARGS)" > logs/$(CLASS)_run_$(TIMESTAMP).log 2>&1 &

.PHONY: all clean compile test package transpile javac run