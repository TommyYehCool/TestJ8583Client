package com.syscom.test;

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.solab.iso8583.IsoMessage;
import com.solab.iso8583.MessageFactory;
import com.solab.iso8583.parse.ConfigParser;

public class ClnMessageHandler {
	
	private final static Logger log = LoggerFactory.getLogger("com.syscom.test");
	
	private final String mJ8583CfgPath = "./config/j8583-config.xml";
	
	private static ClnMessageHandler instance = new ClnMessageHandler();
	
	private MessageFactory<IsoMessage> mf = new MessageFactory<IsoMessage>();
	
	public static ClnMessageHandler getInstance() {
		return instance;
	}
	
	public boolean init() {
		boolean succeed = true;
		try {
			ConfigParser.configureFromUrl(mf, new File(mJ8583CfgPath).toURI().toURL());
			
			log.info("Load J8583 config succeed, path: <{}>", mJ8583CfgPath);
		} 
		catch (IOException e) {
			log.error("IOException raised while loading J8583 config, msg: <{}>", e.toString(), e);
			
			succeed = false;
		}
		return succeed;
	}
}
