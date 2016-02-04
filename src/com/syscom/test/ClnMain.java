package com.syscom.test;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Scanner;

import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClnMain {
	
	private final static Logger log = LoggerFactory.getLogger("com.syscom.test");
	
	private String mServerIp = "127.0.0.1";
	private int mServerPort = 1234;
	private int mTimeout = 10000;
	
	private ClnCommunicationHandler mCommunicationHandler;
	private ISO8583MsgHandler mISO8583MsgHandler = ISO8583MsgHandler.getInstance();
	
	private void start() {
		loadLog4jConfig();
		
		showInitLog();
		
		initISO8583MsgHandler();
		
		connectToServer();
		
		getUserInput();
	}

	private void loadLog4jConfig() {
		String log4jConfig = "./config/log4j.properties";

		PropertyConfigurator.configure(log4jConfig);
		
		log.info("Load log4j config succeed, path: <{}>", log4jConfig);
	}

	private void showInitLog() {
		log.info("------- Try to init client process with process ID: <{}> -------", ManagementFactory.getRuntimeMXBean().getName().split("@")[0]);
	}

	private void initISO8583MsgHandler() {
		boolean initSucceed = mISO8583MsgHandler.init();
		if (initSucceed) {
			log.info("Initialize ISO8583MsgHandler done");
		}
		else {
			log.error("Initialize ISO8583MsgHandler failed");
			System.exit(1);
		}
	}

	private void connectToServer() {
		Socket socket = new Socket();
		try {
			socket.setTcpNoDelay(true);
			
			// NOTE: 如果設定這個 InputStream read 會發生 timeout
//			socket.setSoTimeout(mTimeout);
			
			InetSocketAddress isa = new InetSocketAddress(mServerIp, mServerPort);
			
			socket.connect(isa, mTimeout);
			
			log.info("Connect to server ip: <{}>, port: <{}> succeed", mServerIp, mServerPort);
			
			mCommunicationHandler = new ClnCommunicationHandler(socket);
			
			mCommunicationHandler.start();
			
			log.info("------- Prepare to communicate with server -------");
		} 
		catch (IOException e) {
			log.warn("Connect failed, ip: <{}>, port: <{}>, err-msg: {}", mServerIp, mServerPort, e.toString(), e);
			System.exit(1);
		}
	}

	private void getUserInput() {
		try (Scanner scanner = new Scanner(System.in)) {
			String userInput = "";
			
			loop:
			while (true) {
				log.info("Please enter the message type to sent ('byte' to exit):");
				userInput = scanner.nextLine();
				
				switch (userInput) {
					case "bye":
						break loop;
						
					default:
						byte[] bMsg = mISO8583MsgHandler.createMsg(userInput);
						if (bMsg == null) {
							log.warn("Please input a message type which <header> and <template> both defined in j8583-config.xml");
						}
						else {
							log.info("Create message type: {} from template suceed, content:{}", userInput, new String(bMsg));
							
							mCommunicationHandler.sendMsgToServer(bMsg);
						}
						break;
				}
			}
			log.info(">>>>>> Client terminate <<<<<");
			System.exit(0);
		}
	}

	public static void main(String[] args) {
		new ClnMain().start();
	}

}
