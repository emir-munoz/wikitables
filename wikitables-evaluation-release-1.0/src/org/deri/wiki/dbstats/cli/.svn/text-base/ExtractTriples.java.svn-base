package org.deri.wiki.dbstats.cli;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
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
import org.semanticweb.yars.nx.parser.Callback;
import org.semanticweb.yars.nx.parser.NxParser;
import org.semanticweb.yars.stats.Count;
import org.semanticweb.yars.util.CallbackNxBufferedWriter;


/**
 * Rank an extracted graph.
 * 
 * @author aidhog
 *
 */
public class ExtractTriples {
	static transient Logger _log = Logger.getLogger(ExtractTriples.class.getName());
	
	public static final int TICKS = 1000000;
	
	public static final int TRIPLE_AFTER_HASH = 2; 

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
		
		Option insO = new Option("i", "input triples and features");
		insO.setRequired(true);
		insO.setArgs(1);
		options.addOption(insO);
		
		Option insgzO = new Option("igz", "input is gzipped");
		insgzO.setArgs(0);
		options.addOption(insgzO);
		
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
		
		InputStream is = new FileInputStream(cmd.getOptionValue("i"));
		if(cmd.hasOption("igz"))
			is = new GZIPInputStream(is);
		BufferedReader br = new BufferedReader(new InputStreamReader(is));
		
		Callback cb = null;
		String out = cmd.getOptionValue("o");
		BufferedWriter bw = null;
		
		if(out!=null){
			OutputStream os = new FileOutputStream(out);
			if(cmd.hasOption("ogz")){
				os = new GZIPOutputStream(os);
			}
			bw = new BufferedWriter(new OutputStreamWriter(os));
			cb = new CallbackNxBufferedWriter(bw);
		}
		
		String line = null;
		int done = 0;
		while((line = br.readLine())!=null){
			line = line.trim();
			if(!line.isEmpty()){
				done++;
				if(done%1000000==0){
					_log.info("Read "+done+" lines.");
				}
				
				String[] split = line.split(" ");
				String triple = "";
				
				int hash = -1;
				for(String s:split){
					if(hash!=-1){
						hash++;
						if(hash>=TRIPLE_AFTER_HASH && hash<=TRIPLE_AFTER_HASH+3){
							triple+= s+" ";
						}
					} else if(s.equals("#")){
						hash = 0;
					}
				}
				
				try{
					Node[] nodes = NxParser.parseNodes(triple);
					if(nodes!=null && nodes.length==3){
						if(cb!=null){
							cb.processStatement(nodes);
						}
					} else{
						_log.severe("Cannot parse triple "+triple);
					}
				} catch(Exception e){
					_log.severe("Cannot parse triple "+triple);
				}
			}
		}
		
		is.close();
		if(bw!=null){
			bw.close();
		}
	}
}
