package com.syscom.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClnCommunicationHandler extends Thread {

	private final static Logger log = LoggerFactory.getLogger("com.syscom.test");
	
	private final static String DISCONNECTED = "disconnected";
	
	private boolean mConnected = false;
	
	private InputStream mInputStream;
	private OutputStream mOutputStream;
	
	private RespForSvrQ mRespForSvrQ;
	private RespForSvrHandler mRespForSvrHandler;
	
	private ClnMessageHandler mMessageHandler;
	
	public ClnCommunicationHandler(Socket socket) throws IOException {
		setName(this.getClass().getSimpleName());
		
		mConnected = true;
		
		mInputStream = socket.getInputStream();
		mOutputStream = socket.getOutputStream();
		
		mRespForSvrQ = new RespForSvrQ();
		mRespForSvrHandler = new RespForSvrHandler();
		mRespForSvrHandler.start();
		
		mMessageHandler = ClnMessageHandler.getInstance();
	}
	
	public void run() {
		try {
			while (mConnected) {
				byte[] bMsgLen = new byte[2];
				
				boolean disconnectFromServer = receiveFromSocket(bMsgLen);
				if (disconnectFromServer) {
					break;
				}
				
				int msgLen = ConvertUtil.convert2BytesToInt(bMsgLen);
				log.info("Received server message, length: <{}>", msgLen);
				
				byte[] bSvrMsg = new byte[msgLen];
				disconnectFromServer = receiveFromSocket(bSvrMsg);
				
				if (disconnectFromServer) {
					break;
				}
				
				log.info("Received server message done, msg: <{}>", new String(bSvrMsg));
				
				// TODO 處理 server 過來的 ISO8583 訊息
//				mMessageHandler.processISO8583(bSvrMsg);
			}
			disconnectFromServer();
		}
		catch (IOException e) {
			log.error("IOException raised while communicate with server, terminate ClnCommunicationHandler thread, msg: <{}>", e.getMessage(), e);
		}
	}
	
	private boolean receiveFromSocket(byte[] msg) throws IOException {
		boolean disconnected = false;
		
		int totalMsgLen = msg.length;
		
		int toReadLen = totalMsgLen;
		
		int offset = 0;
		
		while (offset < totalMsgLen) {
			byte[] tmp = new byte[toReadLen];
			
			int readLen = mInputStream.read(tmp);

			disconnected = (readLen == -1);
			
			if (!disconnected) {
				System.arraycopy(tmp, 0, msg, offset, readLen);
				offset += readLen;
				toReadLen -= readLen;
			}
			else {
				break;
			}
		}
		return disconnected;
	}
	
	private void disconnectFromServer() {
		log.warn("Detected disconnect from server: "
				+ "1. Set connected flag to false, "
				+ "2. Terminate RespForSvrHandler thread");
		
		mConnected = false;
		sendRespToServer(DISCONNECTED.getBytes());
	}
	
	public void sendRespToServer(byte[] msg) {
		mRespForSvrQ.offer(msg);
	}
	
	private class RespForSvrHandler extends Thread {
		
		public RespForSvrHandler() {
			setName(this.getClass().getSimpleName());
		}
		
		public void run() {
			try {
				while (mConnected) {
					List<byte[]> respMsg = mRespForSvrQ.getResps();
					
					int msgCounts = respMsg.size();
					if (msgCounts == 1) {
						String msg = new String(respMsg.get(0));
						if (msg.equals(DISCONNECTED)) {
							log.warn("Detected disconnect from server, terminate RespForSvrHandler thread...");
							break;
						}
					}
					
					for (int i = 0; i < msgCounts; i++) {
						byte[] bRespMsgs = respMsg.get(i);
						
						byte[] msgLen = ConvertUtil.convertIntTo2Bytes(bRespMsgs.length);
						
						mOutputStream.write(msgLen);
						mOutputStream.write(bRespMsgs);
						mOutputStream.flush();
						
						log.info("Send response message to server done, msg: <{}>", new String(bRespMsgs));
					}
				}
				log.warn("Received disconnect from server signal, terminate RespForSvrHandler thread");
			}
			catch (InterruptedException e) {
				log.error("InterruptedException raised while getting response message from queue, RespForSvrHandler thread terminated, msg: <{}>", e.getMessage(), e);
			} 
			catch (IOException e) {
				log.error("IOException raised while sending response message to server, RespForSvrHandler thread terminated, msg: <{}>", e.getMessage(), e);
			}
		}
	}
}
