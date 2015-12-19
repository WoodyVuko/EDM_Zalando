package zalando.classifier.sourcer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import zalando.classifier.main.MyBlockingQueue;

public class SourceOutput implements Runnable {
	public String name = "";
	public MyBlockingQueue outputQueue;
	public String startPath = "files/tmp/output/";
	public String FolderToWrite = "neu";
	FileWriter fileWriter = null;
	BufferedWriter bufferedWriter = null;

	public SourceOutput(String name, MyBlockingQueue outputQueue) {
		super();
		// TODO Auto-generated constructor stub
		this.name = name;
		this.outputQueue = outputQueue;
	}

	@Override
	public void run() {
		int counter = 0;
		System.out.println(this.name + " started... waiting for some Input");
		try {
			fileWriter = new FileWriter("files/tmp/test/" + FolderToWrite + "/neu.json");
			fileWriter.write("[");
			//fileWriter.write(System.getProperty("line.separator"));

			while (!outputQueue.isEmpty()) {
				counter++;
				JSONObject obj = outputQueue.take();

				fileWriter.write(obj.toJSONString());
				fileWriter.write(System.getProperty("line.separator"));
				if(!outputQueue.isEmpty())fileWriter.write(",");
				fileWriter.write(System.getProperty("line.separator"));

			}
	    	//fileWriter.write(System.getProperty( "line.separator" ));
	    	fileWriter.write("]");
	    	fileWriter.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
