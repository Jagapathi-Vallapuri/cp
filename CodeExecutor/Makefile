CC = g++
CFLAGS = -Wall -std=c++17
LIBS = -lSimpleAmqpClient -lrabbitmq

all: worker

worker: main.o sandbox.o utils.o
	$(CC) $(CFLAGS) -o worker main.o sandbox.o utils.o $(LIBS)

main.o: main.cpp sandbox.h defs.h LanguageStrategy.hpp utils.h
	$(CC) $(CFLAGS) -c main.cpp

sandbox.o: sandbox.cpp sandbox.h defs.h LanguageStrategy.hpp utils.h
	$(CC) $(CFLAGS) -c sandbox.cpp

utils.o: utils.cpp utils.h
	$(CC) $(CFLAGS) -c utils.cpp

clean:
	rm -f *.o worker