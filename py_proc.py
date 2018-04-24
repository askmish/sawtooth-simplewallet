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


class SWTransactionHandler(TransactionHandler):
    def __init__(self, namespace_prefix):
        self._namespace_prefix = namespace_prefix

    @property
    def family_name(self):
        return 'simplewallet'

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
        
        to_key = payload_list[2]
        
        from_key = header.signer_public_key
        
        if operation == "deposit":
            _make_deposit(context, operation, amount, from_key)

        if operation == "withdraw":
            _make_withdraw(context, operation, amount, from_key)

        if operation == "transfer":
            _make_transfer(context, operation, amount, to_key, from_key)


def _make_deposit(self, context, operation, amount):
    pass


def _make_withdraw(self, context, operation, amount):
    pass


def _make_transfer(self, context, operation, amount, from_key, to_key):
    pass


def _get_key(self):
    return _hash(FAMILY_NAME.encode('utf-8'))[0:6] + _hash(self._publicKey.encode('utf-8'))[0:64]


def main():
    processor = TransactionProcessor(url='tcp://localhost:4004')

    handler = SWTransactionHandler(sw_namespace)

    processor.add_handler(handler)

    processor.start()


if __name__ == "__main__":
    main()
