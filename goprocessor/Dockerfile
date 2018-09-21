FROM ubuntu:xenial

RUN echo "deb [arch=amd64] http://repo.sawtooth.me/ubuntu/ci xenial universe" >> /etc/apt/sources.list \
 && echo "deb http://archive.ubuntu.com/ubuntu bionic-backports universe" >> /etc/apt/sources.list \
 && (apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys 8AA7AF1F1091A5FD \
   || apt-key adv --keyserver hkp://p80.pool.sks-keyservers.net:80 --recv-keys 8AA7AF1F1091A5FD) \
 && apt-get update \
 && apt-get install -y \
    build-essential \
    git \
    golang-1.9-go \
    libssl-dev \
    libzmq5 \
    libzmq3-dev \
    openssl \
    python3 \
    python3-grpcio-tools=1.1.3-1 \
 && apt-get clean

RUN mkdir -p /project/simplewallet/goprocessor
ENV GOPATH=/go:/go/src/github.com/hyperledger/sawtooth-sdk-go:/project/simplewallet/goprocessor
ENV PATH=$PATH:/project/bin:/go/bin:/usr/lib/go-1.9/bin:/project/simplewallet/goprocessor

# TODO: Use dep to solve dependency resolution problem in go code
RUN go get -u \
    github.com/golang/protobuf/proto \
    github.com/golang/protobuf/protoc-gen-go \
    github.com/pebbe/zmq4 \
    github.com/satori/go.uuid \
    github.com/btcsuite/btcd/btcec \
    github.com/jessevdk/go-flags \
    github.com/golang/mock/gomock \
    github.com/golang/mock/mockgen \
    golang.org/x/crypto/ssh \
    github.com/hyperledger/sawtooth-sdk-go

WORKDIR /go/src/github.com/hyperledger/sawtooth-sdk-go
RUN go generate 

EXPOSE 4004/tcp

WORKDIR /project/simplewallet/goprocessor
COPY . ./
RUN bash -c "./build.sh"
RUN rm -rf src/ *.sh Dockerfile
CMD bash -C "simple_wallet"
