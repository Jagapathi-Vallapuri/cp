CC = g++

CFLAGS = -Wall -std=c++17

LIBS = -lSimpleAmqpClient -lrabbitmq

all: worker

worker: main.o sandbox.o
	$(CC) $(CFLAGS) -o worker main.o sandbox.o $(LIBS)

main.o: main.cpp sandbox.h defs.h
	$(CC) $(CFLAGS) -c main.cpp

sandbox.o: sandbox.cpp sandbox.h defs.h
	$(CC) $(CFLAGS) -c sandbox.cpp

clean:
	rm -f *.o worker