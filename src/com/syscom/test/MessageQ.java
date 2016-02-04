package com.syscom.test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class MessageQ {
private BlockingQueue<byte[]> queue;
	
	// TODO 改為設定檔
	private int maximumTakenFromQ; 
	
	public MessageQ() {
		queue = new LinkedBlockingQueue<byte[]>();
	}
	
	public void offer(byte[] resp) {
		queue.offer(resp);
	}
	
	public List<byte[]> getResps() throws InterruptedException {
		List<byte[]> allResps = new ArrayList<byte[]>(100);
		
		int alreadyTaked = 0;
		
		byte[] resp = queue.take();
		allResps.add(resp);
		
		alreadyTaked++;
		
		byte[] remainResp = null;
		while ((remainResp = queue.poll()) != null) {
			allResps.add(remainResp);
			alreadyTaked++;
			if (alreadyTaked == maximumTakenFromQ) {
				break;
			}
		}
		
		return allResps;
	}
}
