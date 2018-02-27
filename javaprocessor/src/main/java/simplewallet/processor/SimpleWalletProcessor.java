package main.java.simplewallet.processor;

import java.util.logging.Logger;

import sawtooth.sdk.processor.TransactionProcessor;

public class SimpleWalletProcessor {
    private final static Logger logger = Logger.getLogger(SimpleWalletProcessor.class.getName());

    public static void main(String[] args) {
	if (args.length != 1) {
	    logger.info("Missing argument!! Please pass validator connection string");
	}

	TransactionProcessor simpleWalletProcessor = new TransactionProcessor(args[0]);
	simpleWalletProcessor.addHandler(new SimpleWalletHandler());
	Thread thread = new Thread(simpleWalletProcessor);
	thread.run();
    }

}
