MVN = mvn
ARGS =

all: clean compile test package

clean:
	$(MVN) clean

compile:
	$(MVN) compile

test:
	$(MVN) test

package:
	$(MVN) package

run:
	java -jar target/oblivious-java-parser-1.0-SNAPSHOT.jar $(ARGS)

.PHONY: all clean compile test package run