package org.deri.wiki.dbstats.cli;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.cli.Main;
import org.semanticweb.yars.nx.parser.NxParser;
import org.semanticweb.yars.nx.reorder.ReorderIterator;
import org.semanticweb.yars.util.FilterSubsequentDupesIterator;
import org.semanticweb.yars.util.TicksIterator;


/**
 * Rank an extracted graph.
 * 
 * @author aidhog
 *
 */
public class DBpediaRelationStats {
	static transient Logger _log = Logger.getLogger(DBpediaRelationStats.class.getName());
	
	public static final int TICKS = 1000000;

	/**
	 * @param args
	 * @throws IOException 
	 * @throws org.semanticweb.yars.nx.parser.ParseException 
	 */
	public static void main(String[] args) throws IOException, org.semanticweb.yars.nx.parser.ParseException {
		Options options = new Options();
		
		Main.addTicksOption(options);
		Main.addHelpOption(options);
		
		Option outO = new Option("o", "output");
		outO.setRequired(true);
		outO.setArgs(1);
		options.addOption(outO);
		
		Option ogzO = new Option("ogz", "gzip output");
		ogzO.setArgs(0);
		options.addOption(ogzO);
		
		Option insO = new Option("is", "ps* input");
		insO.setRequired(true);
		insO.setArgs(1);
		options.addOption(insO);
		
		Option insgzO = new Option("isgz", "gzipped ps* input");
		insgzO.setArgs(0);
		options.addOption(insgzO);
		
		Option inoO = new Option("io", "po* input");
		inoO.setRequired(true);
		inoO.setArgs(1);
		options.addOption(inoO);
		
		Option inogzO = new Option("iogz", "gzipped po* input");
		inogzO.setArgs(0);
		options.addOption(inogzO);
		
		CommandLineParser parser = new BasicParser();
		CommandLine cmd = null;
		try {
			cmd = parser.parse(options, args);
		} catch (ParseException e) {
			System.err.println("***ERROR: " + e.getClass() + ": " + e.getMessage());
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("parameters:", options );
			return;
		}

		if (cmd.hasOption("h")) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp("parameters:", options );
			return;
		}

		// # load primary input and output
		_log.info("Opening primary input and output...");
		
		InputStream iss = new FileInputStream(cmd.getOptionValue("is"));
		if(cmd.hasOption("isgz"))
			iss = new GZIPInputStream(iss);
		NxParser nxps = new NxParser(iss);
		
		InputStream ios = new FileInputStream(cmd.getOptionValue("io"));
		if(cmd.hasOption("iogz")){
			ios = new GZIPInputStream(ios);
		}
		NxParser nxpo = new NxParser(ios);
		
		String out = cmd.getOptionValue("o");
		OutputStream os = new FileOutputStream(out);
		if(cmd.hasOption("ogz")){
			os = new GZIPOutputStream(os);
		}
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os));

		_log.info("Reading ps* file");
		Map<Node,Integer[]> psstats = predStats(new FilterSubsequentDupesIterator(new TicksIterator(new ReorderIterator(nxps,new int[]{1,0,2}),TICKS)));
		_log.info("Reading po* file");
		Map<Node,Integer[]> postats = predStats(new FilterSubsequentDupesIterator(new TicksIterator(new ReorderIterator(nxpo,new int[]{1,2,0}),TICKS)));
		
		if(postats.size()!=psstats.size()){
			_log.warning("Different number of relations: [ps*:" + psstats.size() +"] [po*:"+ postats.size()+"]");
		}
		
		HashSet<Node> preds = new HashSet<Node>();
		preds.addAll(postats.keySet());
		preds.addAll(psstats.keySet());
		
		for(Node n:preds){
			Integer[] merge = new Integer[3];
			Integer[] po = postats.get(n);
			Integer[] ps = psstats.get(n);
			
			if(ps==null){
				_log.warning("Missing ps stats for "+n);
				merge[0] = 0;
				merge[1] = 0;
			} else{
				merge[0] = ps[0];
				merge[1] = ps[1];
			}
			
			if(po==null){
				_log.warning("Missing po stats for "+n);
				merge[0] = 0;
				merge[2] = 0;
			} else{
				if(merge[0]==0)
					merge[0] = po[0];
				else if(merge[0]!=po[0])
					_log.warning("For "+n+" ps* reports triples "+ps[0]+", po* reports triples "+po[0]);
				merge[2] = po[1];
			}
			
			bw.write(n.toN3()+"\t"+merge[0]+"\t"+merge[1]+"\t"+merge[2]+"\n");
		}
		
		bw.close();
		ios.close();
		iss.close();
	}
	
	
	private static final Map<Node,Integer[]> predStats(Iterator<Node[]> in){
		Map<Node,Integer[]> stats = new HashMap<Node,Integer[]>();
		Node[] last = null;
		Node[] next = null;
		
		int trips = 0, terms = 0;
		boolean done = !in.hasNext();
		while(!done){
			if(in.hasNext())
				 next = in.next();
			else done = true;
			
			if(last!=null){
				if(done || (!next[0].equals(last[0]))){
					terms++;
					if(stats.containsKey(last[0])){
						_log.warning("Seen "+last[0]+" before!");
					}
					Integer[] is = new Integer[2];
					is[0] = trips;
					is[1] = terms;
					
					stats.put(last[0],is);
					
					trips = 0;
					terms = 0;
				} else if(!next[1].equals(last[1])){
					terms ++;
				}
			}
			
			if(!done){
				trips++;
				last = next;
			}
		}
		
		return stats;
	}
}
