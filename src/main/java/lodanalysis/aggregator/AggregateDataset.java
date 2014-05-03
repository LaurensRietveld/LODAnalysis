package lodanalysis.aggregator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import lodanalysis.Entry;
import lodanalysis.Settings;
import lodanalysis.utils.Counter;
import lodanalysis.utils.NodeContainer;

import org.apache.commons.io.FileUtils;

public class AggregateDataset implements Runnable  {
	private File datasetDir;
	private InputStream gzipStream;
	private InputStream fileStream;
	private Reader decoder;
	private BufferedReader reader;
	Map<Set<String>, Counter> tripleNsCounts = new HashMap<Set<String>, Counter>();
	Map<String, Counter> dataTypeCounts = new HashMap<String, Counter>();
	Map<String, Counter> langTagCounts = new HashMap<String, Counter>();
	Map<String, Counter> langTagWithoutRegCounts = new HashMap<String, Counter>();
	Map<String, Counter> totalNsCounts = new HashMap<String, Counter>();
	Map<String, Counter> nsCountsUniq = new HashMap<String, Counter>();
	Map<String, Counter> uniqUriCounts = new HashMap<String, Counter>();
	Map<String, Counter> uniqBnodeCounts = new HashMap<String, Counter>();
	Map<String, Counter> schemaCounts = new HashMap<String, Counter>();
	private Entry entry;
	public static void aggregate(Entry entry, File datasetDir) throws IOException {
		AggregateDataset aggr = new AggregateDataset(entry, datasetDir);

		aggr.run();
	}
	public AggregateDataset(Entry entry, File datasetDir) throws IOException {
		this.entry = entry;
		this.datasetDir = datasetDir;
	}

	private void processDataset() throws IOException {
		try {
			File inputFile = new File(datasetDir, Settings.FILE_NAME_INPUT_GZ);
			if (!inputFile.exists()) inputFile = new File(datasetDir, Settings.FILE_NAME_INPUT);
			if (inputFile.exists()) {
				BufferedReader br = getNtripleInputStream(inputFile);
				String line = null;
				while((line = br.readLine())!= null) {
					processLine(line);
				}

				postProcessAnalysis();
				store();
				close();
			} else {
				log("no input file found in dataset " + datasetDir.getName());
			}

		} catch (Throwable e) {
			//cancel on ALL exception. I want to know whats going on!
			System.out.println("Exception analyzing " + datasetDir.getName());
			e.printStackTrace();
		}
	}
	
	private void log(String msg) throws IOException {
		if (entry.isVerbose()) System.out.println(msg);
		Aggregator.writeToLogFile(msg);
		
	}

	private void store() throws IOException {
		writeCountersToFile(new File(datasetDir, Settings.FILE_NAME_NS_COUNTS), totalNsCounts);
		writeCountersToFile(new File(datasetDir, Settings.FILE_NAME_NS_UNIQ_COUNTS), nsCountsUniq);
		writeCountersToFile(new File(datasetDir, Settings.FILE_NAME_LANG_TAG_COUNTS), langTagCounts);
		writeCountersToFile(new File(datasetDir, Settings.FILE_NAME_LANG_TAG_NOREG_COUNTS), langTagWithoutRegCounts);
		writeCountersToFile(new File(datasetDir, Settings.FILE_NAME_DATATYPE_COUNTS), dataTypeCounts);
		writeCountersToFile(new File(datasetDir, Settings.FILE_NAME_UNIQ_URIS_COUNTS), uniqUriCounts);
		writeCountersToFile(new File(datasetDir, Settings.FILE_NAME_UNIQ_BNODES_COUNTS), uniqBnodeCounts);
		writeCountersToFile(new File(datasetDir, Settings.FILE_NAME_SCHEMA_URI_COUNTS), schemaCounts);

		//this one is a bit different (key is a set of strings)
		FileWriter namespaceTripleCountsOutput = new FileWriter(new File(datasetDir, "namespaceTripleCounts"));
		for (Set<String> tripleNs: tripleNsCounts.keySet()) {
			namespaceTripleCountsOutput.write(tripleNs.toString() + "\t" + tripleNsCounts.get(tripleNs) + System.getProperty("line.separator"));
		}
		namespaceTripleCountsOutput.close();
	}

	private void postProcessAnalysis() {
		//we've got all the unique uris. Use these to count how diverse each namespace is used (i.e., the 'namespaceUniqCounts'
		for (String uri: uniqUriCounts.keySet()) {
			String ns = NodeContainer.getNs(uri);
			if (!nsCountsUniq.containsKey(ns)) {
				nsCountsUniq.put(ns, new Counter(1));
			} else {
				nsCountsUniq.get(ns).increase();
			}
		}
	}
	
	/**
	 * get nodes. if it is a uri, remove the < and >. For literals, keep quotes. This makes the number of substring operation later on low, and we can still distinguish between URIs and literals
	 * @param line
	 * @return
	 */
	public static String[] getNodes(String line){
		StringBuilder sub = null;
		StringBuilder pred = null;
		StringBuilder obj = null;
		int nodePos = 0; //0: sub, 1: pred, 2: obj
		int lineLength = line.length() - 2;//stop before the end, i.e., we don't want the ' .' part in the object
		for (int i = 0; i < lineLength; i++) {
			char curChar = line.charAt(i);
			
			if (nodePos < 2) {
				//in pred or obj
				if (curChar == ' ') {
					nodePos++;
				} else {
					if (sub == null) {
						//skip this first char (we know it is a <)
						sub = new StringBuilder();
						continue;
					}
					if (nodePos == 1 && pred == null) {
						//skip this first char (we know it is a <)
						pred = new StringBuilder();
						
						//and btw, remove final '>' from sub
						sub.deleteCharAt(sub.length()-1);
						continue;
					}
					if (nodePos == 0) {
						sub.append(curChar);
					} else {
						pred.append(curChar);
					}
					
				}
			} else {
				if (obj == null) {
					obj = new StringBuilder();
					
					//and btw, remove final '>' from sub
					pred.deleteCharAt(pred.length()-1);
					
					if (curChar == '<') {
						//ah, object is uri. do not read this char, and make sure we don't read the final '>'
						lineLength--;	
						continue;
					}
				}
				obj.append(curChar);
			}
			
	    }
		return new String[]{sub.toString().intern(), pred.toString().intern(), obj.toString().intern()};
	}

	private void processLine(String line) {
		
		String[] nodes = getNodes(line);
		if (nodes.length == 3) {
			NodeContainer sub = new NodeContainer(nodes[0], NodeContainer.Position.SUB);
			NodeContainer pred = new NodeContainer(nodes[1], NodeContainer.Position.PRED);
			NodeContainer obj = new NodeContainer(nodes[2], NodeContainer.Position.OBJ);

			/**
			 * Collecting and counting schema URIs
			 */
			if (sub.isSchema)
				upCounter(schemaCounts, sub.stringRepresentation);
			if (pred.isSchema)
				upCounter(schemaCounts, pred.stringRepresentation);
			if (obj.isSchema)
				upCounter(schemaCounts, obj.stringRepresentation);

			/**
			 * store ns triples
			 */
			Set<String> tripleNs = new HashSet<String>();
			if (sub.ns != null) tripleNs.add(sub.ns);
			if (pred.ns != null) tripleNs.add(pred.ns);
			if (obj.ns != null) tripleNs.add(obj.ns);
			if (!tripleNsCounts.containsKey(tripleNs)) {
				tripleNsCounts.put(tripleNs, new Counter(1));
			} else {
				tripleNsCounts.get(tripleNs).increase();
			}

			/**
			 * store ns counters
			 */
			if (sub.isUri) upCounter(totalNsCounts, sub.ns);
			if (pred.isUri) upCounter(totalNsCounts, pred.ns);
			if (obj.isUri) upCounter(totalNsCounts, obj.ns);

			/**
			 * store uniq uris
			 */
			if (sub.isUri) upCounter(uniqUriCounts, sub.stringRepresentation);
			if (pred.isUri) upCounter(uniqUriCounts, pred.stringRepresentation);
			if (obj.isUri) upCounter(uniqUriCounts, obj.stringRepresentation);

			/**
			 * store uniq bnodes
			 */
			if (sub.isBnode) upCounter(uniqBnodeCounts, sub.stringRepresentation);
			if (pred.isBnode) upCounter(uniqBnodeCounts, pred.stringRepresentation);
			if (obj.isBnode) upCounter(uniqBnodeCounts, obj.stringRepresentation);


			if (obj.isLiteral) {
				if (obj.datatype != null) {
					upCounter(dataTypeCounts, obj.datatype);
				}
				if (obj.langTag != null) {
					upCounter(langTagCounts, obj.langTag);
				}
				if (obj.langTagWithoutReg!= null) {
					upCounter(langTagWithoutRegCounts, obj.langTagWithoutReg);
				}
			}
		} else {
			System.out.println("Could not get triple from line. " + nodes.toString());
		}

	}

	/**
	 * just a simple helper method, to update the maps with a string as key, and counter as val
	 */
	private void upCounter(Map<String, Counter> map, String key) {
		if (key == null) key = "null";
		Counter counter = map.get(key);
		if (counter == null) {
			counter = new Counter(1);
			map.put(key.intern(), counter);
		}
		counter.increase();
	}
	/**
	 * just a simple helper method, to store the maps with a string as key, and counter as val
	 * @throws IOException 
	 */
	private void writeCountersToFile(File targetFile, Map<String, Counter> map) throws IOException {
		FileWriter fw = new FileWriter(targetFile);
		for (String key: map.keySet()) {
			fw.write(key + "\t" + map.get(key) + System.getProperty("line.separator"));
		}
		fw.close();
	}

	private BufferedReader getNtripleInputStream(File file) throws IOException {
		reader = null;
		if (file.getName().endsWith(".gz")) {
			fileStream = new FileInputStream(file);
			gzipStream = new GZIPInputStream(fileStream, 200536);//maximize buffer: http://java-performance.com/
			decoder = new InputStreamReader(gzipStream, "UTF-8");
			reader = new BufferedReader(decoder);
		} else {
			reader = new BufferedReader(new FileReader(file));
		}

		return reader;
	}

	private void close() throws IOException {
		if (gzipStream != null) gzipStream.close();
		if (fileStream != null) fileStream.close();
		if (decoder != null) decoder.close();
		if (reader != null) reader.close();

	}
	
	private void storeDelta() throws IOException {
		File deltaFile = new File(datasetDir, Aggregator.DELTA_FILENAME);
		FileUtils.write(deltaFile, Integer.toString(Aggregator.DELTA_ID));
	}
	
	/**
	 * del delta. This way, when we re-run (forced) a dataset analysis, but we stop in the middle, we know we have to re-run this dataset again later on
	 * @throws IOException
	 */
	private void delDelta() throws IOException {
		File deltaFile = new File(datasetDir, Aggregator.DELTA_FILENAME);
		if (deltaFile.exists()) deltaFile.delete();
	}
	
	@Override
	public void run() {
		try {
			
			log("aggregating " + datasetDir.getName());
			delDelta();
			processDataset();
			storeDelta();
			Aggregator.PROCESSED_COUNT++;
			Aggregator.printProgress(datasetDir);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
