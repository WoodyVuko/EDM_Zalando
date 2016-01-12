package zalando.classifier.main;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import org.json.simple.JSONObject;

import zalando.classifier.pipes.BPPipe;
import zalando.classifier.pipes.BloggerPipe;
import zalando.classifier.pipes.ManualWordpressPipe;
import zalando.classifier.pipes.RssPipe;
import zalando.classifier.pipes.TumblePipe;

public class Classifier implements Runnable{
	private MyBlockingQueue inputQueue;
	private MyBlockingQueue outputQueue;
	private Identificator identificator = new Identificator();
	//private SourceInput input = new SourceInput();
	public int counter = 0;
	public String name = "";
	public Classifier(String name, MyBlockingQueue inputQueue, MyBlockingQueue outputQueue) {
		super();
		this.name = name;
		this.inputQueue = inputQueue;
		this.outputQueue = outputQueue;
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
	public ArrayList<String> manualWPPipeCounter = new ArrayList<String>();
	public ArrayList<String> bloggerPipeCounter = new ArrayList<String>();
	public ArrayList<String> tumblrPipeCounter = new ArrayList<String>();
	public ArrayList<String> defaultPipeCounter = new ArrayList<String>();
	
	public void process() throws InterruptedException{
		for(;;){
			JSONObject obj = inputQueue.poll(5, TimeUnit.SECONDS);
			if (obj == null) 
			{
				synchronized (this.inputQueue) {
					this.inputQueue.notify();
				}
				break;
			}
			System.err.println(this.name + ": Taking Item form Q for processing");
			
			String urlFromRaw = obj.get("url").toString();
			String htmlFromRaw = obj.get("html").toString();			
			String selector = identificator.evaluate(urlFromRaw, htmlFromRaw);
			String filename = ++counter + this.name;
			
			switch (selector) {
//			case "wordpress":
//				new WordpressPipe(urlFromRaw, htmlFromRaw);
//				break;
//			case "blogger":
//				new BloggerPipe(urlFromRaw, htmlFromRaw);
//				break;
			case "manual_wordpress":
			{
				ManualWordpressPipe mwp = new ManualWordpressPipe(urlFromRaw, htmlFromRaw);
				JSONObject result = mwp.process();
				if(result != null)
				{
					result.put("selector", selector);
					outputQueue.put(result);
				}
				new BPPipe(urlFromRaw, htmlFromRaw, selector, filename+"_compare_mwp");
				manualWPPipeCounter.add(urlFromRaw);
				break;
			}
			case "blogger":
			{
				BloggerPipe blogger_pipe = new BloggerPipe(urlFromRaw, htmlFromRaw, selector, filename);
				JSONObject result = blogger_pipe.process();
				result.put("selector", selector);
				if(result != null){
					outputQueue.put(result);
					bloggerPipeCounter.add(urlFromRaw);
				}
				break;
			}
			case "tumblr":
			{
				TumblePipe blogger_pipe = new TumblePipe(urlFromRaw, htmlFromRaw, selector, filename);
				JSONObject result = blogger_pipe.process();
				result.put("selector", selector);
				if(result != null){
					outputQueue.put(result);
					tumblrPipeCounter.add(urlFromRaw);
				}
				break;
			}
			case "rssBlogger":
			case "rss":
			{
				boolean isBlogger = !selector.equalsIgnoreCase("rss");
				RssPipe rp = new RssPipe(urlFromRaw, isBlogger);
				JSONObject result = rp.process();
				
				if (result != null) {
					result.put("selector", selector);
					outputQueue.put(result);
				}
				break;
			}
			default:
			{
				//anstatt in der pipe jedes JSONObj zu schreiben, geben wir es in den Classifier
				//zurueck, damit der das schreibt, weil er asyncron alle processed Objs kriegen soll
				//er schreibt es in die OutputQueue. 
				BPPipe bp_pipe = new BPPipe(urlFromRaw, htmlFromRaw, selector, filename);
				JSONObject result = bp_pipe.process();
				result.put("selector", selector);
				if(result != null){
					outputQueue.put(result);
					defaultPipeCounter.add(urlFromRaw);
				}
				break;
			}
			} 
			
		}
		System.out.println("Default: " +defaultPipeCounter.size());
		System.out.println("Wordpress: " +manualWPPipeCounter.size());
		System.out.println("Blogger: " +bloggerPipeCounter.size());
		System.out.println("Tumblr: " +tumblrPipeCounter.size());
	}
}
