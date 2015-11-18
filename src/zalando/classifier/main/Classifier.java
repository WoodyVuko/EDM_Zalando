package zalando.classifier.main;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import org.json.simple.JSONObject;

import zalando.classifier.pipes.BPPipe;
import zalando.classifier.pipes.BloggerPipe;
import zalando.classifier.pipes.WordpressPipe;
import zalando.classifier.sourcer.SourceInput;

public class Classifier implements Runnable{
	private MyBlockingQueue inputQueue;
	private Identificator identificator = new Identificator();
	//private SourceInput input = new SourceInput();
	public int counter = 0;
	public String name = "";
	public Classifier(String name, MyBlockingQueue inputQueue) {
		super();
		this.name = name;
		this.inputQueue = inputQueue;
	}

	public void init(){
		
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		System.out.println(this.name + " started...");
		init();
		try {
			process();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	public void process() throws InterruptedException{
		for(;;){
			JSONObject obj = inputQueue.take();
			System.err.println(this.name + ": Taking Item form Q for processing");
			
			String urlFromRaw = obj.get("url").toString();
			String htmlFromRaw = obj.get("html").toString();			
			String selector = identificator.evaluate(urlFromRaw, htmlFromRaw);
			String filename = ++counter + this.name;
			BPPipe bp_pipe = new BPPipe(urlFromRaw, htmlFromRaw, selector, filename);
//			switch (selector) {
//			case "wordpress":
//				WordpressPipe wp_pipe = new WordpressPipe(urlFromRaw, htmlFromRaw);
//				break;
//			case "blogger":
//				BloggerPipe bl_pipe = new BloggerPipe(urlFromRaw, htmlFromRaw);
//				break;
//
//			default:
//				BPPipe bp_pipe = new BPPipe(urlFromRaw, htmlFromRaw);
//				break;
//			}
		}
	}
}
