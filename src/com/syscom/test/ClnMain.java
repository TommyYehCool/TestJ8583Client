package com.syscom.test;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.net.Socket;

import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClnMain {
	
	private final static Logger log = LoggerFactory.getLogger("com.syscom.test");
	
	private String mServerIp = "127.0.0.1";
	private int mServerPort = 1234;
	private int mTimeout = 10000;
	
	private ClnCommunicationHandler communicationHandler;
	
	private void start() {
		loadLog4jConfig();
		
		showInitLog();
		
		initMessageHandler();
		
		startClient();
	}

	private void loadLog4jConfig() {
		String log4jConfig = "./config/log4j.properties";

		PropertyConfigurator.configure(log4jConfig);
		
		log.info("Load log4j config succeed, path: <{}>", log4jConfig);
	}

	private void showInitLog() {
		log.info("------- Try to init client process with process ID: <{}> -------", ManagementFactory.getRuntimeMXBean().getName().split("@")[0]);
	}

	private void initMessageHandler() {
		boolean initSucceed = ClnMessageHandler.getInstance().init();
		if (initSucceed) {
			log.info("Initialize MessageHandler done");
		}
		else {
			log.error("Initialize MessageHandler failed");
			System.exit(1);
		}
	}

	private void startClient() {
		Socket socket = new Socket();
		try {
			socket.setTcpNoDelay(true);
			
			// 如果設定這個 InputStream read 會發生 timeout
//			socket.setSoTimeout(mTimeout);
			
			InetSocketAddress isa = new InetSocketAddress(mServerIp, mServerPort);
			
			socket.connect(isa, mTimeout);
			
			log.info("Connect to server ip:<{}>, port:<{}> succeed", mServerIp, mServerPort);
			
			communicationHandler = new ClnCommunicationHandler(socket);
			
			communicationHandler.start();
		} 
		catch (IOException e) {
			log.warn("Connect failed, ip:<{}>, port:<{}>, err-msg: {}", mServerIp, mServerPort, e.toString(), e);
			System.exit(1);
		}
	}

	public static void main(String[] args) {
		new ClnMain().start();
	}

}
