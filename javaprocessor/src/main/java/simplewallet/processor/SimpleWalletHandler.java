package main.java.simplewallet.processor;

import java.util.Collection;

import sawtooth.sdk.processor.State;
import sawtooth.sdk.processor.TransactionHandler;
import sawtooth.sdk.processor.TransactionProcessor;
import sawtooth.sdk.processor.exceptions.InternalError;
import sawtooth.sdk.processor.exceptions.InvalidTransactionException;
import sawtooth.sdk.protobuf.TpProcessRequest;
import sawtooth.sdk.processor.Utils;
import java.util.logging.Logger;
import java.util.ArrayList;
import main.java.simplewallet.application.SimpleWalletRequestProcessor;

public class SimpleWalletHandler implements TransactionHandler {

    private final Logger logger = Logger.getLogger(SimpleWalletHandler.class.getName());
    private final static String version = "1.0";
    private final static String txnFamilyName = "simplewallet";

    private String simpleWalletNameSpace;

    SimpleWalletHandler() {
	try {
		simpleWalletNameSpace = Utils.hash512(this.transactionFamilyName().getBytes("UTF-8")).substring(0, 6);
	} catch (java.io.UnsupportedEncodingException ex)
	{
	} 
    }

    @Override
    public void apply(TpProcessRequest request, State stateInfo) throws InvalidTransactionException, InternalError {
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
