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

run:
	java -cp src/main/java $(CLASS) $(ARGS)

.PHONY: all clean compile test package run