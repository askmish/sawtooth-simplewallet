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
	// step 1. connect to validator with connection string
	TransactionProcessor simpleWalletProcessor = new TransactionProcessor(args[0]);
	// Create and handler and register with TP
	simpleWalletProcessor.addHandler(new SimpleWalletHandler());
	Thread thread = new Thread(simpleWalletProcessor);
	//start the transaction processor
	thread.run();
    }

}

/* ******************************************************************************
 * SimpleWalletHandler
 *
 * This class will be registered as the transaction processor handler
 * with validator
 * It sets the namespaceprefix, versions and types of transactions
 * that can be handled by this TP
 ***************************************************************************** */

class SimpleWalletHandler implements TransactionHandler {

    private final Logger logger = Logger.getLogger(SimpleWalletHandler.class.getName());
    private final static String version = "1.0";
    private final static String txnFamilyName = "simplewallet";

    private String simpleWalletNameSpace;

    SimpleWalletHandler() {
        try {
		//Initialize the simple wallet namespace using first 6 characters
                simpleWalletNameSpace = Utils.hash512(this.transactionFamilyName().getBytes("UTF-8")).substring(0, 6);
        } catch (java.io.UnsupportedEncodingException ex)
        {
        }
    }

    @Override
    public void apply(TpProcessRequest request, State stateInfo) throws InvalidTransactionException, InternalError {
	/* This method is invoked by validater to 
	   perform actual business logic to perform and do ledger state manipulation
	*/
        SimpleWalletRequestProcessor processor = new SimpleWalletRequestProcessor();
        processor.execute(request, stateInfo);
    }

    @Override
    public Collection<String> getNameSpaces() {
        ArrayList<String> namespaces = new ArrayList<>();
        namespaces.add(this.simpleWalletNameSpace);
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

}

class SimpleWalletRequestProcessor {

    private final Logger logger = Logger.getLogger(SimpleWalletRequestProcessor.class.getName());
    public static final String SimpleWalletNameSpace = "simplewallet";
    private Integer amount;
    private String userKey;

    public SimpleWalletRequestProcessor() {
        amount = 0;
        userKey = null;
    }

    public boolean execute(TpProcessRequest transactionRequest, State stateInfo) throws InvalidTransactionException, InternalError {
        // Get request from payload data
        String payload =  transactionRequest.getPayload().toStringUtf8();
        ArrayList<String> payloadList = new ArrayList<>(Arrays.asList(payload.split(",")));
        if(payloadList.size() != 2) {
            throw new InvalidTransactionException("Invalid no. of arguments: expected 2, got:" + payloadList.size());
        }
        // We would get the operation name and amount from the payload
        String operation = payloadList.get(0);
        this.amount = Integer.valueOf(payloadList.get(1));
        // Getting the user signing public key from header
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
        //Get the wallet key from the signer public key
        String walletKey = this.getWalletKey(this.userKey);
        logger.info("Got user key" + this.userKey + "wallet key " + walletKey);
        //Get balance from ledger state
        String balance = stateInfo.getState(Collections.singletonList(walletKey)).get(walletKey).toStringUtf8();
        Integer newBalance = 0;
        if (balance.isEmpty()) {
            logger.info("This is the first time we got a deposit for user.");
            logger.info("Creating a new account for the user: " + this.userKey);
            newBalance = this.amount;
        }
        else {
            newBalance = Integer.valueOf(balance) + this.amount;
        }
        // Update balance in the ledger state
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
        //Get balance from ledger state
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
        //Generate unique key(wallet key) from the wallet namespace and user signer key
        return Utils.hash512(SimpleWalletNameSpace.getBytes()).substring(0, 6)
                + Utils.hash512(userKey.getBytes()).substring(0, 64);
    }

}

