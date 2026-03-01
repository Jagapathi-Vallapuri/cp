FROM debian:bookworm-slim AS builder

ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    cmake \
    git \
    python3 \
    default-jdk-headless \
    librabbitmq-dev \
    libboost-all-dev \
    ca-certificates \
    wget \
    pkg-config \
    && rm -rf /var/lib/apt/lists/*

RUN mkdir -p /usr/include/nlohmann && \
    wget -q https://github.com/nlohmann/json/releases/download/v3.11.2/json.hpp -O /usr/include/nlohmann/json.hpp

WORKDIR /tmp
RUN git clone --depth 1 https://github.com/alanxz/SimpleAmqpClient.git && \
    cd SimpleAmqpClient && \
    mkdir build && cd build && \
    cmake .. -DCMAKE_INSTALL_PREFIX=/usr/local && \
    make -j"$(nproc)" && \
    make install && \
    cd /tmp && rm -rf SimpleAmqpClient

WORKDIR /app
COPY . .
RUN make

FROM debian:bookworm-slim

ENV DEBIAN_FRONTEND=noninteractive
ENV LD_LIBRARY_PATH=/usr/local/lib

RUN apt-get update && apt-get install -y --no-install-recommends \
    python3 \
    g++ \
    default-jdk-headless \
    diffutils \
    ca-certificates \
    librabbitmq4 \
    libboost-system1.74.0 \
    libboost-chrono1.74.0 \
    libboost-date-time1.74.0 \
    libboost-thread1.74.0 \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=builder /usr/local/lib/libSimpleAmqpClient.so* /usr/local/lib/
COPY --from=builder /app/worker /app/worker

RUN useradd -m judgeuser && chown -R judgeuser:judgeuser /app

USER judgeuser
CMD ["./worker"]