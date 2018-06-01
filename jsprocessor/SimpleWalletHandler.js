/**
 * Copyright 2018 Intel Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ------------------------------------------------------------------------------
 */

'use strict'
const { TransactionHandler } = require('sawtooth-sdk/processor/handler')
const {
  InvalidTransaction,
  InternalError
} = require('sawtooth-sdk/processor/exceptions')
const crypto = require('crypto')
const {TextEncoder, TextDecoder} = require('text-encoding/lib/encoding')

const _hash = (x) => crypto.createHash('sha512').update(x).digest('hex').toLowerCase().substring(0, 64)
var encoder = new TextEncoder('utf8')
var decoder = new TextDecoder('utf8')
const MIN_VALUE = 0
const SW_FAMILY = 'simplewallet'
const SW_NAMESPACE = _hash(SW_FAMILY).substring(0, 6)

//function to obtain the payload obtained from the client
const _decodeRequest = (payload) =>
  new Promise((resolve, reject) => {
    payload = payload.toString().split(',')
    if (payload.length === 2) {
      resolve({
        action: payload[0],
        amount: payload[1]
      })
    }
   else if(payload.length === 3){ 
	resolve({
	  action:payload[0],
	  amount:payload[1],
	  toKey:payload[2]
	})
    }
    else {
      let reason = new InvalidTransaction('Invalid payload serialization')
      reject(reason)
    }
})

//function to display the errors
const _toInternalError = (err) => {
  console.log(" in error message block")
  let message = err.message ? err.message : err
  throw new InternalError(message)
}

//function to set the entries in the block using the "SetState" function
const _setEntry = (context, address, stateValue) => {
  let dataBytes = encoder.encode(stateValue)
  let entries = {
    [address]: dataBytes 
  }
  return context.setState(entries)
}

//function to make a deposit transaction
const makeDeposit =(context, address, amount, user)  => (possibleAddressValues) => {
  let stateValueRep = possibleAddressValues[address]
  let newBalance = 0
  let balance
  if (stateValueRep == null || stateValueRep == ''){
    console.log("No previous deposits, creating new deposit")
    newBalance = amount
  }
  else{
    balance = decoder.decode(stateValueRep)
    newBalance = parseInt(balance) + amount
    console.log("Amount crediting:"+newBalance)
  }
  let strNewBalance = newBalance.toString()
  return _setEntry(context, address, strNewBalance)
}

//function to make a withdraw transaction
const makeWithdraw =(context, address, amount, user)  => (possibleAddressValues) => {
  let stateValueRep = possibleAddressValues[address]
  let newBalance = 0
  let balance
  if (stateValueRep == null || stateValueRep == ''){
    newBalance = amount
  }
  else{
    balance = decoder.decode(stateValueRep)
    balance = parseInt(balance)
    if (balance < amount){
      throw new InvalidTransaction(`Not enough money. The amount should be lesser or equal to ${balance}`)
    }
    newBalance = balance - amount
    console.log("Amount debiting:"+newBalance)
  }
  let strNewBalance = newBalance.toString()
  return _setEntry(context, address, strNewBalance)
}

//function to make a transfer transaction
const makeTransfer =(context, senderAddress, amount, recieverAddress)  => (possibleAddressValues) => {
  if(amount <= MIN_VALUE){
    throw new InvalidTransaction('Amount is invalid')
  }
  let senderBalance
  let currentEntry = possibleAddressValues[senderAddress]
  let currentEntryTo = possibleAddressValues[recieverAddress]
  let senderNewBalance = 0
  let recieverBalance
  let recieverNewBalance = 0
  if(currentEntry == null || currentEntry == '')
    console.log("No user (debitor)")
  if(currentEntryTo == null || currentEntryTo == '')
    console.log("No user (Creditor)")
  senderBalance = decoder.decode(currentEntry)
  senderBalance = parseInt(senderBalance)
  recieverBalance = decoder.decode(currentEntryTo)
  recieverBalance = parseInt(recieverBalance)
  if(isNaN(senderBalance))
    senderBalance = 0
  if(isNaN(recieverBalance))
    recieverBalance = 0
  if(senderBalance < amount){
    throw new InvalidTransaction("Not enough money to perform transfer operation")
  }
  else{
    console.log("Debiting amount from the sender:"+amount)
    senderNewBalance = senderBalance -  amount
    recieverNewBalance = recieverBalance + amount
    let stateData = senderNewBalance.toString()
    _setEntry(context, senderAddress, stateData)
    stateData = recieverNewBalance.toString()
    console.log("Sender balance:"+senderNewBalance+", Reciever balance:"+recieverNewBalance)
    return  _setEntry(context, recieverAddress, stateData)
  }
}

class SimpleWalletHandler extends TransactionHandler{
  constructor(){
    super(SW_FAMILY,['1.0'],[SW_NAMESPACE])
  }
  apply(transactionProcessRequest, context){
    return _decodeRequest(transactionProcessRequest.payload)
    .catch(_toInternalError)
    .then((update) => {
    let header = transactionProcessRequest.header
    let userPublicKey = header.signerPublicKey
    let action = update.action
    if (!update.action) {
      throw new InvalidTransaction('Action is required')
    }
    let amount = update.amount
    if (amount === null || amount === undefined) {
      throw new InvalidTransaction('Value is required')
    }
    amount = parseInt(amount)
    if (typeof amount !== "number" ||  amount <= MIN_VALUE) {
      throw new InvalidTransaction(`Value must be an integer ` + `no less than 1`)
    }
        // Select the action to e performed
    let actionFn
    if (update.action === 'deposit') { 
      actionFn = makeDeposit
    }
    else if (update.action === 'withdraw') {
      actionFn = makeWithdraw 
    } 
    else if (update.action === 'transfer') {
      actionFn = makeTransfer
    }
    else if(update.action ==='balance') {
      actionFn = showBalance
    }
    else {	
      throw new InvalidTransaction(`Action must be create or take not ${update.action}`)		
    }
    let senderAddress = SW_NAMESPACE + _hash(userPublicKey).slice(-64)
    // this is the key obtained for the beneficiary in the payload , used only during transfer function
    let beneficiaryKey = update.toKey
    let recieverAddress
    if(beneficiaryKey != undefined){
      recieverAddress = SW_NAMESPACE + _hash(update.toKey).slice(-64)
    }
    // Get the current state, for the key's address:
    let getPromise
    if (update.action == 'transfer')
      getPromise = context.getState([senderAddress, recieverAddress])
    else
      getPromise = context.getState([senderAddress])
    let actionPromise = getPromise.then(
      actionFn(context, senderAddress, amount, recieverAddress)
      )
    return actionPromise.then(addresses => {
      if (addresses.length === 0) {
        throw new InternalError('State Error!')
      }  
    })
  })
 }
}
module.exports = SimpleWalletHandler
