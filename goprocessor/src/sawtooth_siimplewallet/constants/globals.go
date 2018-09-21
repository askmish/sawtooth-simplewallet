package constants

const (
	// Related to transaction family
	TransactionFamilyName    string = "simplewallet"
	TransactionFamilyVersion string = "1.0"
	// Related to length of addresses
	TransactionFamilyNamespaceAddressLength int = 6
	TransactionUserAddressLength            int = 64
	// Constants used in CLI
	TransactionTransfer string = "transfer"
	TransactionDeposit  string = "deposit"
	TransactionWithdraw string = "withdraw"
	// Transaction indices
	TransactionOperationIndex int = 0
	TransactionAmountIndex    int = 1
	TransactionToIndex        int = 2
)
