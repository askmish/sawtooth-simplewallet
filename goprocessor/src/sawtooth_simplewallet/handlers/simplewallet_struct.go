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

package handlers

import "github.com/hyperledger/sawtooth-sdk-go/processor"

// Define struct, this will implement TP
type SimpleWalletHandler struct {
	context   *processor.Context
	operation string
	amount    int
	userFrom  string
	userTo    string
}

// Getters
func (self SimpleWalletHandler) getOperation() string {
	return self.operation
}

func (self SimpleWalletHandler) getAmount() int {
	return self.amount
}

func (self SimpleWalletHandler) getUserFrom() string {
	return self.userFrom
}

func (self SimpleWalletHandler) getUserTo() string {
	return self.userTo
}

func (self SimpleWalletHandler) getContext() *processor.Context {
	return self.context
}
