package zalando.classifier;

import zalando.classifier.main.Classifier;
import zalando.classifier.main.MyBlockingQueue;
import zalando.classifier.sourcer.SourceInput;
import zalando.classifier.sourcer.SourceOutput;

public class Start {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		MyBlockingQueue inputQ = new MyBlockingQueue("BQInput", 100);
		Thread SourceInputThread = new Thread(new SourceInput("SourceT1", inputQ));
		
		Thread ClassifierThread1 = new Thread(new Classifier("ClassifierT1", inputQ));
		Thread ClassifierThread2 = new Thread(new Classifier("ClassifierT2", inputQ));
		Thread ClassifierThread3 = new Thread(new Classifier("ClassifierT3", inputQ));
		Thread ClassifierThread4 = new Thread(new Classifier("ClassifierT4", inputQ));
		Thread ClassifierThread5 = new Thread(new Classifier("ClassifierT5", inputQ));
		Thread ClassifierThread6 = new Thread(new Classifier("ClassifierT6", inputQ));
		Thread ClassifierThread7 = new Thread(new Classifier("ClassifierT7", inputQ));
		Thread ClassifierThread8 = new Thread(new Classifier("ClassifierT8", inputQ));
		Thread ClassifierThread9 = new Thread(new Classifier("ClassifierT9", inputQ));
		Thread ClassifierThread10 = new Thread(new Classifier("ClassifierT10", inputQ));
		
		
		ClassifierThread1.start();
		ClassifierThread2.start();
//		ClassifierThread3.start();
//		ClassifierThread4.start();
//		ClassifierThread5.start();
//		ClassifierThread6.start();
//		ClassifierThread7.start();
//		ClassifierThread8.start();
//		ClassifierThread9.start();
//		ClassifierThread10.start();
		SourceInputThread.start();
	}
	
}
