package main.java.simplewallet.application;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.logging.Logger;

import sawtooth.sdk.processor.State;
import sawtooth.sdk.processor.Utils;
import sawtooth.sdk.processor.exceptions.InvalidTransactionException;
import sawtooth.sdk.protobuf.TpProcessRequest;
import sawtooth.sdk.processor.exceptions.InternalError;
import com.google.protobuf.ByteString;


public class SimpleWalletRequestProcessor {

    private final Logger logger = Logger.getLogger(SimpleWalletRequestProcessor.class.getName());
    public static final String SimpleWalletNameSpace = "simplewallet";
    private Integer amount;
    private String userKey;

    public SimpleWalletRequestProcessor() {
	amount = 0;
	userKey = null;
    }

    public boolean execute(TpProcessRequest transactionRequest, State stateInfo) throws InvalidTransactionException, InternalError {
	//Get request payload data
	String payload =  transactionRequest.getPayload().toStringUtf8();
	ArrayList<String> payloadList = new ArrayList<>(Arrays.asList(payload.split(",")));
	if(payloadList.size() != 2) {
	    throw new InvalidTransactionException("Invalid no. of arguments: expected 2, got:" + payloadList.size());
	}
	String operation = payloadList.get(0);
	this.amount = Integer.valueOf(payloadList.get(1));
	this.userKey = transactionRequest.getHeader().getSignerPublicKey();
	boolean status = false;
	switch(operation) {
	case "deposit" :
	    status = this.makeDeposit(stateInfo);
	    break;
	case "withdraw":
	    status = this.makeWithdraw(stateInfo);
	    break;
	default:
	    status = false;
	    String error = "Unsupported operation " + operation;
	    throw new InvalidTransactionException(error);
	}
	return status;
    }

    private boolean makeDeposit(State stateInfo) throws InvalidTransactionException, InternalError {
	String walletKey = this.getWalletKey(this.userKey);
	logger.info("Got user key" + this.userKey + "wallet key " + walletKey);
	String balance = stateInfo.getState(Collections.singletonList(walletKey)).get(walletKey).toStringUtf8();
	Integer newBalance = 0;
	// Update balance
	if (this.amount <= 0) {
	    String error = "Deposit amount should be greater than 0";
	    throw new InvalidTransactionException(error);
	}
	else if (balance.isEmpty()) {
	    logger.info("This is the first time we got a deposit for user.");
	    logger.info("Creating a new account for the user: " + this.userKey);
	    newBalance = this.amount;
	}
	else {
	    newBalance = Integer.valueOf(balance) + this.amount;
	}
	Map.Entry<String, ByteString> entry = new AbstractMap.SimpleEntry<String, ByteString>(walletKey,
		ByteString.copyFromUtf8(newBalance.toString()));
	Collection<Map.Entry<String, ByteString>> ledgerEntry = Collections.singletonList(entry);
	logger.info("Crediting balance with " + this.amount);
	stateInfo.setState(ledgerEntry);
	return true;
    }

    private boolean makeWithdraw(State stateInfo) throws InvalidTransactionException, InternalError {
 	String walletKey = this.getWalletKey(this.userKey);
 	logger.info("Got user key " + this.userKey + "wallet key "+ walletKey);
 	String balance = stateInfo.getState(Collections.singletonList(walletKey)).get(walletKey).toStringUtf8();

 	if (balance.isEmpty()) {
 	    logger.info("Didn't find the wallet key associated with user key" + this.userKey);
 	    return false;
 	}
 	Integer value = Integer.valueOf(balance);
 	if (value < this.amount) {
 	    String error = "Withdraw amount should be lesser than or equal to " + value;
 	    throw new InvalidTransactionException(error);
 	}
 	// Update balance
 	Integer updateBalance = value - this.amount;
 	Map.Entry<String, ByteString> entry = new AbstractMap.SimpleEntry<String, ByteString>(walletKey, ByteString.copyFromUtf8(updateBalance.toString()));
 	Collection<Map.Entry<String, ByteString>> ledgerEntry = Collections.singletonList(entry);
	logger.info("Debitting balance with " + this.amount);
 	stateInfo.setState(ledgerEntry);
 	return true;
     }

    private String getWalletKey(String userKey) {
	return Utils.hash512(SimpleWalletNameSpace.getBytes()).substring(0, 6)
		+ Utils.hash512(userKey.getBytes()).substring(0, 64);
    }
    
}

