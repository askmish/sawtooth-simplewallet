# sawtooth-simplewallet
A simple sawtooth "simplewallet" transaction family example (processor + client)

# simplewallet

This is a minimal example of a sawtooth 1.0 application. This example demonstrates, a common usecase, where a customer deposits/withdraws money from an account, based on private key.

A customer can:
1. deposit money into his/her bank account.
2. withdraw money from his/her bank account.
3. check the balance in the account.

The customer is identified by a customer name and a corresponding public key. The value of the wallet, i.e. the balance, is stored at an address derived from hash of customer's public key and the transaction family namespace. To keep code simple, this example has been written for just one customer, one account.

# Components 
The application is built in two parts:
1. The client application written in python, written in two parts: _client.py file representing the backend stuff and the _cli.py representing the frontend stuff. The example is built by using the setup.py file located in one directory level up.

2. The Transaction Processor is written in C++11 using c++-sawtooth-sdk. It comes with its CMake files for build.

# Usage

This example uses docker-compose and docker containers. If you do not have these installed please follow the instructions here: https://docs.docker.com/install/

Start the docker containers in docker-compose.yaml file, located in sawtooth-simplewallet directory:
```bash
cd sawtooth-simplewallet
docker-compose up --build
```

At this point all the containers should have been built and running.

To launch the client, you could do this:
```bash
docker exec -it simplewallet-client_1 bash
```

You can locate the right docker client container name using `docker ps`.

Sample command usage:
sawtooth keygen jack <--- This creates the public/private keys, a pre-requisite for all commands following
./simplewallet deposit 100 jack <--- This adds the 100 amount to Jack's state address
./simplewallet withdraw 50 jack <--- Withdraws 50 from Jack's state address
./simplewallet balance jack <--- Displays the balance left in Jack's account

# License
This example and Hyperledger Sawtooth software are licensed under the [Apache License Version 2.0](LICENSE) software license.
