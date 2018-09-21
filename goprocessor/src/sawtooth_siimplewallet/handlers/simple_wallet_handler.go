package handlers

import (
	"errors"
	"github.com/hyperledger/sawtooth-sdk-go/logging"
	"github.com/hyperledger/sawtooth-sdk-go/processor"
	"github.com/hyperledger/sawtooth-sdk-go/protobuf/processor_pb2"
	"sawtooth_siimplewallet/constants"
	"sawtooth_siimplewallet/utils"
	"strconv"
	"strings"
)

var logger = logging.Get()

func (self *SimpleWalletHandler) FamilyName() string {
	return constants.TransactionFamilyName
}

func (self *SimpleWalletHandler) FamilyVersions() []string {
	return []string{constants.TransactionFamilyVersion}
}

func (self *SimpleWalletHandler) Namespaces() []string {
	return []string{self.getNamespaceAddress()}
}

func (self *SimpleWalletHandler) Apply(request *processor_pb2.TpProcessRequest, context *processor.Context) error {
	payload := string(request.GetPayload())
	payloadList := strings.Split(payload, ",")
	if len(payloadList) != 2 && !(len(payloadList) == 3 && constants.TransactionTransfer == payloadList[constants.TransactionOperationIndex]) {
		return errors.New("Invalid no. of arguments: expected 2 or 3, got: " + string(len(payloadList)))
	}

	self.context = context
	// get operation
	self.operation = payloadList[constants.TransactionOperationIndex]
	// get amount
	var err error
	self.amount, err = strconv.Atoi(payloadList[constants.TransactionAmountIndex])
	if err != nil {
		return err
	}

	// get public key from header
	self.userFrom = request.GetHeader().GetSignerPublicKey()
	switch self.operation {
	case constants.TransactionDeposit:
		return self.deposit()
	case constants.TransactionWithdraw:
		return self.withdraw()
	case constants.TransactionTransfer:
		self.userTo = payloadList[constants.TransactionToIndex]
		return self.transfer()
	default:
		return errors.New("Unsupported operation " + self.getOperation())
	}
	return nil
}

func (self SimpleWalletHandler) transfer() error {

	walletKeyFrom := self.getNamespaceAddress() + utils.Hexdigest(self.getUserFrom())[:constants.TransactionUserAddressLength]
	walletKeyTo := self.getNamespaceAddress() + utils.Hexdigest(self.getUserTo())[:constants.TransactionUserAddressLength]

	//Get and validate balance from ledger state for debtor
	currentLedgerEntry, err := self.getContext().GetState([]string{walletKeyFrom, walletKeyTo})
	if err != nil {
		return err
	}
	balance := string(currentLedgerEntry[walletKeyFrom])
	if balance == "" {
		return errors.New("Didn't find the wallet key associated with user key " + self.getUserFrom())
	}
	debtorBalance, err := strconv.Atoi(balance)
	if err != nil {
		return err
	}
	if debtorBalance < self.getAmount() {
		return errors.New("Transfer amount should be lesser than or equal to " + strconv.Itoa(debtorBalance))
	}

	//Get and validate balance from ledger state for creditor
	balance = string(currentLedgerEntry[walletKeyTo])
	if balance == "" {
		return errors.New("Didn't find the wallet key associated with user key " + self.getUserTo())
	}
	creditorBalance, err := strconv.Atoi(balance)
	if err != nil {
		return err
	}

	// Update balance of debtor
	updateBalance := debtorBalance - self.getAmount()
	entry := make(map[string][]byte)
	entry[walletKeyFrom] = []byte(strconv.Itoa(updateBalance))
	logger.Info("Debiting balance with " + strconv.Itoa(self.getAmount()))
	self.getContext().SetState(entry)

	//Crediting to creditor
	// Update balance of creditor
	updateBalance = creditorBalance + self.getAmount()
	entry = make(map[string][]byte)
	entry[walletKeyTo] = []byte(strconv.Itoa(updateBalance))
	logger.Info("Crediting to balance with " + strconv.Itoa(self.getAmount()))
	self.getContext().SetState(entry)
	return nil
}

func (self SimpleWalletHandler) withdraw() error {
	// Get the wallet key derived from the wallet user's public key
	walletKey := self.getNamespaceAddress() + utils.Hexdigest(self.getUserFrom())[:constants.TransactionUserAddressLength]
	logger.Info("Got user key " + self.getUserFrom() + "wallet key " + walletKey)
	// Get balance from ledger state
	currentLedgerEntry, err := self.getContext().GetState([]string{walletKey})
	if err != nil {
		return err
	}
	balance := string(currentLedgerEntry[walletKey])
	// getState() will return empty map if wallet key doesn't exist in state
	if balance == "" {
		return errors.New("Didn't find the wallet key associated with user key " + self.getUserFrom())
	}
	value, err := strconv.Atoi(balance)
	if err != nil {
		return err
	}
	if value < self.getAmount() {
		return errors.New("Withdraw amount should be lesser than or equal to " + strconv.Itoa(value))
	}
	// Update balance
	updateBalance := value - self.getAmount()
	entry := make(map[string][]byte)
	entry[walletKey] = []byte(strconv.Itoa(updateBalance))
	logger.Info("Withdrawing amount: " + strconv.Itoa(self.getAmount()))
	self.getContext().SetState(entry)
	return nil
}

func (self SimpleWalletHandler) deposit() error {
	// Get the wallet key derived from the wallet user's public key
	walletKey := self.getNamespaceAddress() + utils.Hexdigest(self.getUserFrom())[:constants.TransactionUserAddressLength]
	logger.Info("Got user key " + self.getUserFrom() + "wallet key " + walletKey)
	// Get balance from ledger state
	currentLedgerEntry, err := self.getContext().GetState([]string{walletKey})
	if err != nil {
		return err
	}
	balance := string(currentLedgerEntry[walletKey])
	newBalance := 0
	// getState() will return empty map if wallet key doesn't exist in state
	if balance == "" {
		logger.Info("This is the first time we got a deposit for user.")
		logger.Info("Creating a new account for the user: " + self.getUserFrom())
		newBalance = self.getAmount()
	} else {
		newBalance += self.getAmount()
		var err error
		newBalance, err = strconv.Atoi(balance)
		if err != nil {
			return err
		}
	}
	// Update balance in the ledger state
	entry := make(map[string][]byte)
	entry[walletKey] = []byte(strconv.Itoa(newBalance))
	logger.Info("Depositing amount: " + strconv.Itoa(self.getAmount()))
	self.getContext().SetState(entry)
	return nil
}

func (self SimpleWalletHandler) getNamespaceAddress() string {
	return utils.Hexdigest(constants.TransactionFamilyName)[:constants.TransactionFamilyNamespaceAddressLength]
}
