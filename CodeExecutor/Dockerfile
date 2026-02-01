FROM ubuntu:22.04

ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update && apt-get install -y \
    build-essential \
    cmake \
    git \
    python3 \
    default-jdk \
    librabbitmq-dev \
    libboost-all-dev \
    wget \
    pkg-config \
    && rm -rf /var/lib/apt/lists/*

RUN mkdir -p /usr/include/nlohmann && \
    wget https://github.com/nlohmann/json/releases/download/v3.11.2/json.hpp -O /usr/include/nlohmann/json.hpp

WORKDIR /tmp
RUN git clone https://github.com/alanxz/SimpleAmqpClient.git && \
    cd SimpleAmqpClient && \
    mkdir build && cd build && \
    cmake .. -DCMAKE_INSTALL_PREFIX=/usr && \
    make && \
    make install && \
    cd /tmp && rm -rf SimpleAmqpClient

RUN useradd -m judgeuser

WORKDIR /app
COPY . .

RUN make
RUN chown -R judgeuser:judgeuser /app

USER judgeuser
CMD ["./worker"]