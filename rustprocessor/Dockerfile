# Copyright 2018 Intel Corporation.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

FROM ubuntu:bionic

RUN apt-get update \
 && apt-get install gnupg -y

RUN \
 && apt-get install -y \
 curl \
 gcc \
 libssl-dev \
 libzmq3-dev \
 pkg-config \
 unzip

RUN \
 if [ ! -z $HTTP_PROXY ] && [ -z $http_proxy ]; then \
  http_proxy=$HTTP_PROXY; \
 fi; \
 if [ ! -z $HTTPS_PROXY ] && [ -z $https_proxy ]; then \
  https_proxy=$HTTPS_PROXY; \
 fi; \
 if [ ! -z $http_proxy ]; then \
  http_proxy_host=$(printf $http_proxy | sed 's|http.*://\(.*\):\(.*\)$|\1|');\
  http_proxy_port=$(printf $http_proxy | sed 's|http.*://\(.*\):\(.*\)$|\2|');\
  mkdir -p $HOME/.cargo \
  && echo "[http]" >> $HOME/.cargo/config \
  && echo 'proxy = "'$http_proxy_host:$http_proxy_port'"' >> $HOME/.cargo/config \
  && cat $HOME/.cargo/config; \
 fi;

RUN curl -OLsS https://github.com/google/protobuf/releases/download/v3.5.1/protoc-3.5.1-linux-x86_64.zip \
 && unzip protoc-3.5.1-linux-x86_64.zip -d protoc3 \
 && rm protoc-3.5.1-linux-x86_64.zip

RUN curl https://sh.rustup.rs -sSf > /usr/bin/rustup-init \
 && chmod +x /usr/bin/rustup-init \
 && rustup-init -y

ENV PATH=$PATH:/project/simplewallet/rustprocessor/bin:/protoc3/bin:/root/.cargo/bin \
    CARGO_INCREMENTAL=0

RUN rustup component add rustfmt-preview


WORKDIR /project/simplewallet/rustprocessor

###########################################################################
# Below lines are workaround to avoid rebuilding dependencies every time 
# during docker build

# create a new empty shell project
RUN USER=root cargo new --bin my-project
WORKDIR /project/simplewallet/rustprocessor/my-project

# copy over your manifests 
#(context passed from docker yaml file in parent directory)
COPY ./rustprocessor/Cargo.lock Cargo.lock
COPY ./rustprocessor/Cargo.toml Cargo.toml

# this build step will cache your dependencies
RUN cargo build --release
RUN rm src/*.rs

# copy your source tree
COPY ./rustprocessor/src ./src
###########################################################################

WORKDIR /project/simplewallet/rustprocessor

#CMD cd rustprocessor \
CMD echo "\033[0;32m--- Building simplewallet-rust-tp ---\n\033[0m" \
 && rm -rf ./bin/ \
 && mkdir -p ./bin/ \
 && cargo build --release \
 && cp ./target/release/simplewallet-rust-tp ./bin/simplewallet-rust-tp \
 && cargo run --release --bin simplewallet-rust-tp -- -v -C tcp://validator:4004 \
 && tail -f /dev/null
