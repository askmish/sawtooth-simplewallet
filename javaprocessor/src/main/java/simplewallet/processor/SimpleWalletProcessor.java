package main.java.simplewallet.processor;

import java.util.logging.Logger;
import java.util.Collection;

import sawtooth.sdk.processor.TransactionProcessor;
import sawtooth.sdk.processor.State;
import sawtooth.sdk.processor.TransactionHandler;
import sawtooth.sdk.processor.exceptions.InternalError;
import sawtooth.sdk.processor.exceptions.InvalidTransactionException;
import sawtooth.sdk.protobuf.TpProcessRequest;
import sawtooth.sdk.processor.Utils;
import java.util.logging.Logger;
import java.util.ArrayList;
import main.java.simplewallet.application.SimpleWalletRequestProcessor;


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
	   perform actural business logic to perform and logic to update the ledger state
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

