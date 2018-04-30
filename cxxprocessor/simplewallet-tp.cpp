/* Copyright 2018 Intel Corporation

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
------------------------------------------------------------------------------*/

#include <ctype.h>
#include <string.h>

#include <log4cxx/logger.h>
#include <log4cxx/basicconfigurator.h>
#include <log4cxx/level.h>

#include <sawtooth_sdk/sawtooth_sdk.h>
#include <sawtooth_sdk/exceptions.h>

#include <cryptopp/sha.h>
#include <cryptopp/filters.h>
#include <cryptopp/hex.h>

#include <iostream>
#include <string>
#include <sstream>

#include <utility>
#include <list>
#include <vector>

using namespace log4cxx;

static log4cxx::LoggerPtr logger(log4cxx::Logger::getLogger
    ("simplewallet"));

static const std::string SIMPLEWALLET_FAMILY = "simplewallet";

#define DEFAULT_VALIDATOR_URL "tcp://validator:4004"

// Helper function: To generate an SHA512 hash and return it as a hex
// encoded string.
static std::string sha512(const std::string& message) {
    std::string digest;
    CryptoPP::SHA512 hash;

    CryptoPP::StringSource hasher(message, true,
        new CryptoPP::HashFilter(hash,
          new CryptoPP::HexEncoder (
             new CryptoPP::StringSink(digest), false)));

    return digest;
}

// Helper function: Tokenize std::string based on a delimiter
std::vector<std::string> split(const std::string& str, char delimiter) {
    std::istringstream strStream(str);
    std::string token;
    std::vector<std::string> tokens;

    while (std::getline(strStream, token, delimiter)) {
        tokens.push_back(token);
    }
    return tokens;
}

// Helper function: To extract Action str and value integer from given string
// and beneficiary string if available
void strToActionValueAndBeneficiary(const std::string& str,
                                    std::string& action,
                                    uint32_t& value,
                                    std::string& beneficiary) {
     std::vector<std::string> vs = split(str, ',');

     if (vs.size() == 2) {
         action = vs[0];
         value = std::stoi(vs[1]);
     } else if (vs.size() == 3) {
         action = vs[0];
         value = std::stoi(vs[1]);
         beneficiary = vs[2];
     } else {
         std::string error = "invalid no. of arguments: expected 2 or 3, got:"
             + std::to_string(vs.size()) + "\n";
         throw sawtooth::InvalidTransaction(error);
     }
}

/*******************************************************************************
 * SimpleWalletApplicator
 *
 * Handles the processing of SimpleWallet transactions
 * This is the place where you implement your TF logic
 ******************************************************************************/
class SimpleWalletApplicator:  public sawtooth::TransactionApplicator {
 public:
    SimpleWalletApplicator(sawtooth::TransactionUPtr txn,
        sawtooth::GlobalStateUPtr state) :
        TransactionApplicator(std::move(txn), std::move(state)) { }

    void Apply() {
        std::cout << "SimpleWalletApplicator::Apply\n";
        // Extract user's wallet public key from TransactionHeader
        std::string customer_pubkey = this->txn->header()->GetValue(
            sawtooth::TransactionHeaderSignerPublicKey);

        // Extract the payload from Transaction as a string
        const std::string& raw_data = this->txn->payload();

        std::string action;
        uint32_t value;
        std::string beneficiary_pubkey;

        // Extract the action and value from the payload string
        strToActionValueAndBeneficiary(raw_data,
                                       action,
                                       value,
                                       beneficiary_pubkey);

        std::cout << "Got: " << action << " and " << value << "\n";

        // Choose what to do with value, based on action
        if (action == "deposit") {
            this->makeDeposit(customer_pubkey, value);
        } else if (action == "withdraw") {
            this->doWithdraw(customer_pubkey, value);
        } else if (action == "transfer") {
            std::cout << "Got beneficiary: " << beneficiary_pubkey << "\n";
            this->doTransfer(customer_pubkey, value, beneficiary_pubkey);
        }
        // Add your own action and a corresponding handler here
        // Also add the actions in the client app as well
        else {
            std::string error = "Invalid action: '" + action + "'";
            throw sawtooth::InvalidTransaction(error);
        }
    }

 private:
    // Make a 70-character(35-byte) address to store and retrieve the state
    std::string MakeAddress(const std::string& customer_pubkey) {
        return sha512(SIMPLEWALLET_FAMILY).substr(0, 6) +
            sha512(customer_pubkey).substr(0, 64);
    }

    // Handle the SimpleWallet Deposit action
    // overflow and underflow cases are ignored for this example
    void makeDeposit(const std::string& customer_pubkey,
                     const uint32_t& request_amount) {
        // Generate the unique state address based on user's wallet public key
        auto address = this->MakeAddress(customer_pubkey);
        LOG4CXX_DEBUG(logger, "SimpleWalletApplicator::makeDeposit Key: "
            << customer_pubkey
            << " Address: " << address);

        std::string stored_balance_str;

        uint32_t customer_available_balance = 0;

        // Get the value stored at the state address for this wallet user
        if (this->state->GetState(&stored_balance_str, address)) {
            std::cout << "Available balance: " << stored_balance_str << "\n";
            if (stored_balance_str.length() != 0) {
                customer_available_balance = std::stoi(stored_balance_str);
            }
        } else {
            // If the state address doesn't exist we create a new account
            std::cout << "\nThis is the first time we got a deposit."
                << "\nCreating a new account for user: "
                << customer_pubkey << std::endl;
        }

        // This is the TF business logic:
        // Increment stored value by deposit value, extracted from txn payload
        customer_available_balance += request_amount;
        LOG4CXX_DEBUG(logger, "Storing new available balance: "
                               << customer_available_balance << " units");

        stored_balance_str = std::to_string(customer_available_balance);

        // Store the updated value in the user's unique state address
        this->state->SetState(address, stored_balance_str);
    }

    // Handle SimpleWallet Withdraw action.
    void doWithdraw(const std::string& customer_pubkey,
                    const uint32_t& request_amount) {
        auto address = this->MakeAddress(customer_pubkey);

        LOG4CXX_DEBUG(logger, "SimpleWalletApplicator::doWithdraw Key: "
            << customer_pubkey
            << " Address: " << address);

        std::string stored_balance_str;

        // Retrieve the balance available for customer account
        uint32_t customer_available_balance = 0;
        if (this->state->GetState(&stored_balance_str, address)) {
            std::cout << "Available balance: " << stored_balance_str << "\n";
            customer_available_balance = std::stoi(stored_balance_str);
        } else {
            std::string error = "Action was 'withdraw', but address"
                " not found in state for Key: " + customer_pubkey;
            throw sawtooth::InvalidTransaction(error);
        }

        if (customer_available_balance > 0
            && customer_available_balance >= request_amount) {
            customer_available_balance -= request_amount;
        } else {
            std::string error = "You don't have sufficient balance"
                " to withdraw." + customer_pubkey;
            throw sawtooth::InvalidTransaction(error);
        }

        // encode the value map back to string for storage.
        LOG4CXX_DEBUG(logger, "Storing new available balance:"
                              << customer_available_balance << " units");
        stored_balance_str = std::to_string(customer_available_balance);
        this->state->SetState(address, stored_balance_str);
    }

    // Handle SimpleWallet Transfer action.
    void doTransfer(const std::string& customer_pubkey,
                    const uint32_t& request_amount,
                    const std::string& beneficiary_pubkey) {
        // Get the global state address for each user's account
        // based on respective pubkeys
        auto customer_state_address = this->MakeAddress(customer_pubkey);
        auto beneficiary_state_address = this->MakeAddress(beneficiary_pubkey);

        LOG4CXX_DEBUG(logger, "SimpleWalletApplicator::doTransfer Key: "
            << customer_pubkey
            << " Address: " << customer_state_address);

        LOG4CXX_DEBUG(logger, "SimpleWalletApplicator::doTransfer Beneficiary "
            << "Key: "
            << beneficiary_pubkey
            << " Address: " << beneficiary_state_address);

        std::string stored_balance_str;

        // Retrieve the balance available for customer account
        uint32_t customer_available_balance = 0;
        if (this->state->GetState(&stored_balance_str,
                                  customer_state_address)) {
             customer_available_balance = std::stoi(stored_balance_str);
        } else {
            // The customer account hasn't been created yet
            std::string error = "Action was 'transfer', but address"
                " not found in state for customer Key: " + customer_pubkey;
            throw sawtooth::InvalidTransaction(error);
        }

        // Retrieve the balance available for beneficiary account
        uint32_t beneficiary_available_balance = 0;
        if (this->state->GetState(&stored_balance_str,
                                  beneficiary_state_address)) {
             beneficiary_available_balance = std::stoi(stored_balance_str);
        } else {
            // The beneficiary account hasn't been created yet
            std::string error = "Action was 'transfer', but address"
                " not found in state for beneficiary Key: "
                + beneficiary_pubkey;
            throw sawtooth::InvalidTransaction(error);
        }

        // Verify if customer has sufficient balance
        // to transfer requested amount
        if (customer_available_balance < request_amount) {
            std::string error = "Insufficient balance."
                                " Can't transfer from customer: "
                                + customer_pubkey;
            throw sawtooth::InvalidTransaction(error);
        }

        // Update the balance for customer and store in global state
        customer_available_balance -= request_amount;
        LOG4CXX_DEBUG(logger, "Finalizing " << customer_available_balance
                               << " units for customer: " << customer_pubkey);
        stored_balance_str = std::to_string(customer_available_balance);
        this->state->SetState(customer_state_address, stored_balance_str);

        // Update the balance for beneficiary and store in global state
        beneficiary_available_balance += request_amount;
        LOG4CXX_DEBUG(logger, "Finalizing " << beneficiary_available_balance
                               << " units for beneficiary: "
                               << beneficiary_pubkey);
        stored_balance_str = std::to_string(beneficiary_available_balance);
        this->state->SetState(beneficiary_state_address, stored_balance_str);
    }
};

/*******************************************************************************
 * SimpleWalletHandler
 *
 * This class will be registered as the transaction processor handler
 * with validator
 * It sets the namespaceprefix, versions, TF and types of transactions
 * that can be handled by this TP - via the apply method
 ******************************************************************************/
class SimpleWalletHandler: public sawtooth::TransactionHandler {
 public:
    // Generating a namespace prefix in the default constructor
    SimpleWalletHandler() {
        this->namespacePrefix = sha512(SIMPLEWALLET_FAMILY).substr(0, 6);
        LOG4CXX_DEBUG(logger, "namespace:" << this->namespacePrefix);
    }

    std::string transaction_family_name() const {
        return std::string(SIMPLEWALLET_FAMILY);
    }

    std::list<std::string> versions() const {
        return { "1.0" };
    }

    std::list<std::string> namespaces() const {
        return { namespacePrefix };
    }

    sawtooth::TransactionApplicatorUPtr GetApplicator(
            sawtooth::TransactionUPtr txn,
            sawtooth::GlobalStateUPtr state) {
        return sawtooth::TransactionApplicatorUPtr(
            new SimpleWalletApplicator(std::move(txn), std::move(state)));
    }

 private:
    std::string namespacePrefix;
};

int main(int argc, char** argv) {
    try {
        const std::string connectToValidatorUrl = DEFAULT_VALIDATOR_URL;

        // Set up a simple configuration that logs on the console.
        BasicConfigurator::configure();

        // Set logging verbosity to max
        logger->setLevel(Level::getAll());

        // Create a transaction processor

        // 1. connect to validator at connectToValidatorUrl
        sawtooth::TransactionProcessorUPtr processor(
            sawtooth::TransactionProcessor::Create(connectToValidatorUrl));

        // 2. create a transaction handler for our SimpleWallet TF
        sawtooth::TransactionHandlerUPtr transaction_handler(
            new SimpleWalletHandler());

        // 3. register the transaction handler with validator
        processor->RegisterHandler(
            std::move(transaction_handler));

        // 4. run the transaction processor
        processor->Run();

        return 0;
    } catch(std::exception& e) {
        std::cerr << "Unexpected exception exiting: " << std::endl;
        std::cerr << e.what() << std::endl;
    } catch(...) {
        std::cerr << "Exiting due to unknown exception." << std::endl;
    }

    return -1;
}
