# Copyright 2017 Intel Corporation
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
# ------------------------------------------------------------------------------

from __future__ import print_function
import argparse
import getpass
import logging
import os
import sys
import traceback
import pkg_resources

from colorlog import ColoredFormatter

from wallet.simplewallet_client import SimpleWalletClient
from wallet.simplewallet_exceptions import SimpleWalletException

DISTRIBUTION_NAME = 'simplewallet'

DEFAULT_URL = 'http://simplewallet_sawtooth-rest-api_1:8008'

def create_console_handler(verbose_level):
    clog = logging.StreamHandler()
    formatter = ColoredFormatter(
        "%(log_color)s[%(asctime)s %(levelname)-8s%(module)s]%(reset)s "
        "%(white)s%(message)s",
        datefmt="%H:%M:%S",
        reset=True,
        log_colors={
            'DEBUG': 'cyan',
            'INFO': 'green',
            'WARNING': 'yellow',
            'ERROR': 'red',
            'CRITICAL': 'red',
        })

    clog.setFormatter(formatter)
    clog.setLevel(logging.DEBUG)
    return clog

def setup_loggers(verbose_level):
    logger = logging.getLogger()
    logger.setLevel(logging.DEBUG)
    logger.addHandler(create_console_handler(verbose_level))

def add_deposit_parser(subparsers, parent_parser):
    parser = subparsers.add_parser(
        'deposit',
        help='deposits a certain amount to an account',
        parents=[parent_parser])

    parser.add_argument(
        'value',
        type=int,
        help='the amount to deposit')

    parser.add_argument(
        'customerName',
        type=str,
        help='the name of customer to deposit to')

def add_withdraw_parser(subparsers, parent_parser):
    parser = subparsers.add_parser(
        'withdraw',
        help='withdraws a certain amount from your account',
        parents=[parent_parser])

    parser.add_argument(
        'value',
        type=int,
        help='the amount to withdraw')

    parser.add_argument(
        'customerName',
        type=str,
        help='the name of customer to withdraw from')

def add_balance_parser(subparsers, parent_parser):
    parser = subparsers.add_parser(
        'balance',
        help='shows balance in your account',
        parents=[parent_parser])

    parser.add_argument(
        'customerName',
        type=str,
        help='the name of customer to withdraw from')

def create_parent_parser(prog_name):
    parent_parser = argparse.ArgumentParser(prog=prog_name, add_help=False)

    try:
        version = pkg_resources.get_distribution(DISTRIBUTION_NAME).version
    except pkg_resources.DistributionNotFound:
        version = 'UNKNOWN'

    parent_parser.add_argument(
        '-V', '--version',
        action='version',
        version=(DISTRIBUTION_NAME + ' (Hyperledger Sawtooth) version {}')
        .format(version),
        help='display version information')

    return parent_parser


def create_parser(prog_name):
    parent_parser = create_parent_parser(prog_name)

    parser = argparse.ArgumentParser(
        description='Provides subcommands to manage your simple wallet',
        parents=[parent_parser])

    subparsers = parser.add_subparsers(title='subcommands', dest='command')

    subparsers.required = True

    add_deposit_parser(subparsers, parent_parser)
    add_withdraw_parser(subparsers, parent_parser)
    add_balance_parser(subparsers, parent_parser)

    return parser

def _get_keyfile(args):
    customerName = getpass.getuser() if args.customerName is None else args.customerName
    home = os.path.expanduser("~")
    key_dir = os.path.join(home, ".sawtooth", "keys")

    return '{}/{}.priv'.format(key_dir, customerName)

def do_deposit(args):

    keyfile = _get_keyfile(args)

    client = SimpleWalletClient(baseUrl=DEFAULT_URL, keyFile=keyfile)

    response = client.deposit(args.customerName, args.value)

    print("Response: {}".format(response))

def do_withdraw(args):

    keyfile = _get_keyfile(args)

    client = SimpleWalletClient(baseUrl=DEFAULT_URL, keyFile=keyfile)

    response = client.withdraw(args.customerName, args.value)

    print("Response: {}".format(response))
    
def do_balance(args):
    keyfile = _get_keyfile(args)

    client = SimpleWalletClient(baseUrl=DEFAULT_URL, keyFile=keyfile)

    data = client.balance(args.customerName)

    if data is not None:
        print("Net balance = {}".format(data.decode()))
    else:
        raise SimpleWalletException("Data not found: {}".format(customerName))

def main(prog_name=os.path.basename(sys.argv[0]), args=None):
    if args is None:
        args = sys.argv[1:]
    parser = create_parser(prog_name)
    args = parser.parse_args(args)

    verbose_level = 0

    setup_loggers(verbose_level=verbose_level)

    if args.command == 'deposit':
        do_deposit(args)
    elif args.command == 'withdraw':
        do_withdraw(args)
    elif args.command == 'balance':
        do_balance(args)
    else:
        raise SimpleWalletException("invalid command: {}".format(args.command))


def main_wrapper():
    try:
        main()
    except SimpleWalletException as err:
        print("Error: {}".format(err), file=sys.stderr)
        sys.exit(1)
    except KeyboardInterrupt:
        pass
    except SystemExit as err:
        raise err
    except BaseException as err:
        traceback.print_exc(file=sys.stderr)
        sys.exit(1)
