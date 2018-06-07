package main.java.simplewallet.processor;

import java.util.logging.Logger;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.logging.Logger;

import sawtooth.sdk.processor.TransactionProcessor;
import sawtooth.sdk.processor.State;
import sawtooth.sdk.processor.TransactionHandler;
import sawtooth.sdk.processor.exceptions.InternalError;
import sawtooth.sdk.processor.exceptions.InvalidTransactionException;
import sawtooth.sdk.protobuf.TpProcessRequest;
import sawtooth.sdk.processor.Utils;
import com.google.protobuf.ByteString;


public class SimpleWalletProcessor {
    private final static Logger logger = Logger.getLogger(SimpleWalletProcessor.class.getName());

    public static void main(String[] args) {
	//Check connection string to validator is passed in arguments.
	if (args.length != 1) {
	    logger.info("Missing argument!! Please pass validator connection string");
	}
	// Connect to validator with connection string (tcp://validator:4004)
	TransactionProcessor simpleWalletProcessor = new TransactionProcessor(args[0]);
	// Create simple wallet transaction handler and register with the validator
	simpleWalletProcessor.addHandler(new SimpleWalletHandler());
	Thread thread = new Thread(simpleWalletProcessor);
	//start the transaction processor
	thread.run();
    }

}

/* ******************************************************************************
 * SimpleWalletHandler
 *
 * It handles the processing of operation supported by simplewallet.
 * It sets the name space prefix, versions and transaction family name.
 * This is the place where you implement your transaction specific logic(Insie apply() method
 * 
 ***************************************************************************** */

class SimpleWalletHandler implements TransactionHandler {
    private final Logger logger = Logger.getLogger(SimpleWalletHandler.class.getName());
    private final static String version = "1.0";
    private final static String txnFamilyName = "simplewallet";

    private String simpleWalletNameSpace;

    SimpleWalletHandler() {
	try {
	    //Initialize the simple wallet name space using first 6 characters
	    simpleWalletNameSpace = Utils.hash512(txnFamilyName.getBytes("UTF-8")).substring(0, 6);
	} catch (java.io.UnsupportedEncodingException ex) {
	    System.out.println("Unsupported the encoding format ");
	    ex.printStackTrace();
	    System.exit(1);
	}
    }
    
    /*
     * apply()
     *
     * This method is invoked for each transaction the validator
     * gets from the client
     * 
     * @param: request - contains the transaction
     *                   
     * @param: stateInfo - contains the state context
     *
     * @returns: void
     *
     * @throws: InvalidTransactionException, InternalError
     *
     */

    @Override
    public void apply(TpProcessRequest request, State stateInfo) throws InvalidTransactionException, InternalError {
        // Extract the payload as utf8 str from the transaction, in request var
	String payload =  request.getPayload().toStringUtf8();

        // Split the csv utf-8 string
	ArrayList<String> payloadList = new ArrayList<>(Arrays.asList(payload.split(",")));
	if(payloadList.size() != 2 && !(payloadList.size() == 3 && "transfer".equals(payloadList.get(0)))) {
	    throw new InvalidTransactionException("Invalid no. of arguments: expected 2 or 3, got:" + payloadList.size());
	}
	// First argument from payload is operation name
	String operation = payloadList.get(0);
	// Get the amount
	Integer amount = Integer.valueOf(payloadList.get(1));
	// Get the user signing public key from header
	String userKey = request.getHeader().getSignerPublicKey();
	switch(operation) {
	case "deposit" :
	    makeDeposit(stateInfo, operation, amount, userKey);
	    break;
	case "withdraw":
	    makeWithdraw(stateInfo, operation, amount, userKey);
	    break;
	    /* Add here you custom operation to perform and make changes in client as well
	     * pass the corresponding operation name in the payload.
	     * */
	case "transfer":
		makeTransfer(stateInfo, operation, amount, payloadList.get(2), userKey);
		break;
	default:
	    String error = "Unsupported operation " + operation;
	    throw new InvalidTransactionException(error);
	}
    }

    @Override
    public Collection<String> getNameSpaces() {
	ArrayList<String> namespaces = new ArrayList<>();
	namespaces.add(simpleWalletNameSpace);
	return namespaces;
    }

    @Override
    public String getVersion() {
	return version;
    }

    @Override
    public String transactionFamilyName() {
	return txnFamilyName;
    }

   /*
    * makeDeposit()
    *
    * @param stateInfo - contains the state(the merkle tree), 
    *
    * @param amount - the amount to add to user's wallet
    *
    * @param userKey - the wallet user's public key
    *
    * @returns - void
    *
    * @throws - InvalidTransactionException, InternalError
    *
    */

    private void makeDeposit(State stateInfo, String operation, Integer amount, String userKey)
	    throws InvalidTransactionException, InternalError {
	// Get the wallet key derived from the wallet user's public key
	String walletKey = getWalletKey(userKey);
	logger.info("Got user key " + userKey + "wallet key " + walletKey);
	// Get balance from ledger state
	Map<String, ByteString> currentLedgerEntry = stateInfo.getState(Collections.singletonList(walletKey));
	String balance = currentLedgerEntry.get(walletKey).toStringUtf8();
	Integer newBalance = 0;
	// getState() will return empty map if wallet key doesn't exist in state
	if (balance.isEmpty()) {
	    logger.info("This is the first time we got a deposit for user.");
	    logger.info("Creating a new account for the user: " + userKey);
	    newBalance = amount;
	}
	else {
	    newBalance = Integer.valueOf(balance) + amount;
	}
	// Update balance in the ledger state
	Map.Entry<String, ByteString> entry = new AbstractMap.SimpleEntry<String, ByteString>(walletKey,
		ByteString.copyFromUtf8(newBalance.toString()));
	Collection<Map.Entry<String, ByteString>> newLedgerEntry = Collections.singletonList(entry);
	logger.info("Depositing amount: " + amount);
	stateInfo.setState(newLedgerEntry);
    }

   /*
    * makeWithdraw()
    *
    * @param stateInfo - contains the state(the merkle tree), 
    *
    * @param amount - the amount to deduct from user's wallet
    *
    * @param userKey - the wallet user's public key
    *
    * @returns - void
    *
    * @throws - InvalidTransactionException, InternalError
    *
    */
    private void makeWithdraw(State stateInfo, String operation, Integer amount, String userKey)
	    throws InvalidTransactionException, InternalError {
	// Get the wallet key derived from the wallet user's public key
	String walletKey = getWalletKey(userKey);
	logger.info("Got user key " + userKey + "wallet key "+ walletKey);
	// Get balance from ledger state
	Map<String, ByteString> currentLedgerEntry = stateInfo.getState(Collections.singletonList(walletKey));
	String balance = currentLedgerEntry.get(walletKey).toStringUtf8();
	// getState() will return empty map if wallet key doesn't exist in state
	if (balance.isEmpty()) {
	    String error = "Didn't find the wallet key associated with user key " + userKey;
	    throw new InvalidTransactionException(error);
	}
	Integer value = Integer.valueOf(balance);
	if (value < amount) {
	    String error = "Withdraw amount should be lesser than or equal to " + value;
	    throw new InvalidTransactionException(error);
	}
	// Update balance
	Integer updateBalance = value - amount;
	Map.Entry<String, ByteString> entry = new AbstractMap.SimpleEntry<String, ByteString>(walletKey,
		ByteString.copyFromUtf8(updateBalance.toString()));
	Collection<Map.Entry<String, ByteString>> newLedgerEntry = Collections.singletonList(entry);
	logger.info("Withdrawing amount: " + amount);
	stateInfo.setState(newLedgerEntry);
    }
    
    /*
    * maketransfer()
    *
    * @param stateInfo - contains the state(the merkle tree), 
    *
    * @param transferAmount - the amount to transfer from 1st user's wallet to second users's wallet
    *
    * @param userToKey - the public key of the user to whose wallet amount is to be transferred
    *
    * @param userFromKey - the public key of the user from whose wallet amount is to be transferred
    *
    * @returns - void
    *
    * @throws - InvalidTransactionException, InternalError
    *
    */
    private void makeTransfer(State stateInfo, String operation, Integer transferAmount, String userToKey, String userFromKey)
	    throws InvalidTransactionException, InternalError {
	
	String walletKeyFrom = getWalletKey(userFromKey);
	String walletKeyTo = getWalletKey(userToKey);
	
	//Get and validate balance from ledger state for debtor 
	Map<String, ByteString> currentLedgerEntry = stateInfo.getState(Arrays.asList(new String[]{walletKeyFrom,walletKeyTo}));
	String balance = currentLedgerEntry.get(walletKeyFrom).toStringUtf8();
	if (balance.isEmpty()) {
	    throw new InvalidTransactionException("Didn't find the wallet key associated with user key " + userFromKey);
	}
	Integer debtorBalance = Integer.valueOf(balance);
	if (debtorBalance < transferAmount) {
	    throw new InvalidTransactionException("Transfer amount should be lesser than or equal to " + debtorBalance);
	}
	
	//Get and validate balance from ledger state for creditor 
	balance = currentLedgerEntry.get(walletKeyTo).toStringUtf8();
	if (balance.isEmpty()) {
	    throw new InvalidTransactionException("Didn't find the wallet key associated with user key " + userToKey);
	}
	Integer creditorBalance = Integer.valueOf(balance);
	
	// Update balance of debtor
	Integer updateBalance = debtorBalance - transferAmount;
	Map.Entry<String, ByteString> entry = new AbstractMap.SimpleEntry<String, ByteString>(walletKeyFrom,
		ByteString.copyFromUtf8(updateBalance.toString()));
	Collection<Map.Entry<String, ByteString>> newLedgerEntry = Collections.singletonList(entry);
	logger.info("Debiting balance with " + transferAmount);
	stateInfo.setState(newLedgerEntry);
	
	//Crediting to creditor
	// Update balance of creditor
	updateBalance = creditorBalance + transferAmount; 
	entry = new AbstractMap.SimpleEntry<String, ByteString>(walletKeyTo,
		ByteString.copyFromUtf8(updateBalance.toString()));
	newLedgerEntry = Collections.singletonList(entry);
	logger.info("Crediting to balance with " + transferAmount);
	stateInfo.setState(newLedgerEntry);
	
    }

    private String getWalletKey(String userKey) {
	// Generate unique key(wallet key) from the wallet namespace
        // and user signer public key
	return Utils.hash512(txnFamilyName.getBytes()).substring(0, 6)
		+ Utils.hash512(userKey.getBytes()).substring(0, 64);
    }
}

