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
# ------------------------------------------------------------------------------

import hashlib
import logging

from sawtooth_sdk.processor.handler import TransactionHandler
from sawtooth_sdk.processor.exceptions import InvalidTransaction
from sawtooth_sdk.processor.exceptions import InternalError
from sawtooth_sdk.processor.core import TransactionProcessor

LOGGER = logging.getLogger(__name__)

FAMILY_NAME = "simplewallet"

def _hash(data):
    return hashlib.sha512(data).hexdigest()

sw_namespace = _hash(FAMILY_NAME.encode('utf-8'))[0:6]

class SimpleWalletTransactionHandler(TransactionHandler):
    def __init__(self, namespace_prefix):
        self._namespace_prefix = namespace_prefix

    @property
    def family_name(self):
        return FAMILY_NAME

    @property
    def family_versions(self):
        return ['1.0']

    @property
    def namespaces(self):
        return [self._namespace_prefix]

    def apply(self, transaction, context):
        header = transaction.header
        payload_list = transaction.payload.decode().split(",")
        operation = payload_list[0]
        amount = payload_list[1]
        
        from_key = header.signer_public_key

        LOGGER.info("Operation = "+ operation)

        if operation == "deposit":
            self._make_deposit(context, amount, from_key)
        elif operation == "withdraw":
            self._make_withdraw(context, amount, from_key)
        elif operation == "transfer":
            if len(payload_list) == 3:
                to_key = payload_list[2]
            self._make_transfer(context, amount, to_key, from_key)
        else:
            LOGGER.info("Unhandled action. Operation should be deposit, withdraw or transfer")

    def _make_deposit(self, context, amount, from_key):
        wallet_key = self._get_wallet_key(from_key)
        LOGGER.info('Got the key {} and the wallet key {} '.format(from_key, wallet_key))
        current_entry = context.get_state([wallet_key])
        new_balance = 0

        if current_entry == []:
            LOGGER.info('No previous deposits, creating new deposit {} '.format(from_key))
            new_balance = int(amount)
        else:
            balance = int(current_entry[0].data)
            new_balance = int(amount) + int(balance)

        state_data = str(new_balance).encode()
        addresses = context.set_state({wallet_key: state_data})

        if len(addresses) < 1:
            raise InternalError("State Error")

    def _make_withdraw(self, context, amount, from_key):
        wallet_key = self._get_wallet_key(from_key)
        LOGGER.info('Got the key {} and the wallet key {} '.format(from_key, wallet_key))
        current_entry = context.get_state([wallet_key])
        new_balance = 0

        if current_entry == []:
            LOGGER.info('No user with the key {} '.format(from_key))
        else:
            balance = int(current_entry[0].data)
            if balance < int(amount):
                raise InvalidTransaction('Not enough money. Tha amount should be lesser or equal to {} '.format(value))
            else:
                new_balance = balance - int(amount)

        LOGGER.info('Withdrawing {} '.format(amount))
        state_data = str(new_balance).encode()
        addresses = context.set_state(
            {self._get_wallet_key(from_key): state_data})

        if len(addresses) < 1:
            raise InternalError("State Error")

    def _make_transfer(self, context, transfer_amount, to_key, from_key):
        transfer_amount = int(transfer_amount)
        if transfer_amount <= 0:
            raise InvalidTransaction("The amount cannot be <= 0")

        wallet_key = self._get_wallet_key(from_key)
        wallet_to_key = self._get_wallet_key(to_key)
        LOGGER.info('Got the from key {} and the from wallet key {} '.format(from_key, wallet_key))
        LOGGER.info('Got the to key {} and the to wallet key {} '.format(to_key, wallet_to_key))
        current_entry = context.get_state([wallet_key])
        current_entry_to = context.get_state([wallet_to_key])
        new_balance = 0

        if current_entry == []:
            LOGGER.info('No user (debtor) with the key {} '.format(from_key))
        if current_entry_to == []:
            LOGGER.info('No user (creditor) with the key {} '.format(to_key))

        balance = int(current_entry[0].data)
        balance_to = int(current_entry_to[0].data)
        if balance < transfer_amount:
            raise InvalidTransaction('Not enough money. The amount should be less or equal to {} '.format(balance))
        else:
            LOGGER.info("Debitting balance with " + transfer_amount)
            update_debtor_balance = balance - int(transfer_amount)
            state_data = str(update_debtor_balance).encode()
            context.set_state({wallet_key: state_data})
            update_beneficiary_balance = balance_to + int(transfer_amount)
            state_data = str(update_beneficiary_balance).encode()
            context.set_state({wallet_to_key: state_data})

    def _get_wallet_key(self, from_key):
        return _hash(FAMILY_NAME.encode('utf-8'))[0:6] + _hash(from_key.encode('utf-8'))[0:64]

def setup_loggers():
    logging.basicConfig()
    logging.getLogger().setLevel(logging.DEBUG)

def main():
    setup_loggers()
    try:
        processor = TransactionProcessor(url='tcp://validator:4004')

        handler = SimpleWalletTransactionHandler(sw_namespace)

        processor.add_handler(handler)

        processor.start()

    except KeyboardInterrupt:
        pass
    except SystemExit as err:
        raise err
    except BaseException as err:
        traceback.print_exc(file=sys.stderr)
        sys.exit(1)
