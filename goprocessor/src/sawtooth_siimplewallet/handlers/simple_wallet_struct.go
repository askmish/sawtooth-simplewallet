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
