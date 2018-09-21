package ylyu1.wean;

import clegoues.genprog4java.fitness.Fitness;
import clegoues.genprog4java.fitness.FitnessValue;
import clegoues.genprog4java.fitness.JUnitTestRunner;
import clegoues.genprog4java.fitness.TestCase;
import clegoues.genprog4java.rep.CachingRepresentation;
import clegoues.genprog4java.rep.Representation;
import clegoues.genprog4java.main.Configuration;
import clegoues.genprog4java.mut.EditOperation;
import clegoues.genprog4java.Search.Population;
import clegoues.genprog4java.main.Main;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;

public class VariantCheckerMain
{
	public static int turn = 0;
	public static String debug = "NOTDEBUG"; 
	//public final static boolean cinnamon = true;
	//public static ArrayList<Boolean> goodVariant = new ArrayList<Boolean>();
	
	//these will be initialized by main()
	public static String positiveTestsDaikonSampleArgForm = null;
	public static String negativeTestsArgForm = null;
	
	public static void main(String [] args) throws Exception
	{
		Runtime rt = Runtime.getRuntime();
		Process pr = rt.exec("mkdir opopop");
		pr.waitFor();
	}
	
	private static void setupArgForms()
	{
		positiveTestsDaikonSampleArgForm = "";
		for (TestCase posTest : Fitness.positiveTestsDaikonSample)
		{
			positiveTestsDaikonSampleArgForm = positiveTestsDaikonSampleArgForm + posTest.getTestName() + MultiTestRunner.SEPARATOR;
			//the addition of an extra SEPARATOR at the end should not cause a problem for String.split()
		}
		
		negativeTestsArgForm = "";
		for (TestCase negTest : Fitness.negativeTests)
		{
			negativeTestsArgForm = negativeTestsArgForm + negTest.getTestName() + MultiTestRunner.SEPARATOR;
		}
	}
	
	public static void checkInvariant(Population<? extends EditOperation> pop)
	{
		Aggregator.clear();
		ArrayList<Representation<? extends EditOperation>> goodRepsForCheck = new ArrayList<Representation<? extends EditOperation>>();
		ArrayList<Representation<? extends EditOperation>> notRepsForCheck = new ArrayList<Representation<? extends EditOperation>>();

		for(Representation<? extends EditOperation> rep : pop)
		{
			if (rep.getAlreadyCompiled() == null || !rep.getAlreadyCompiled().getLeft())
			{
				notRepsForCheck.add(rep);
				continue; //if rep is not already compiled, don't touch it and go on to the next representation
			}
			rep.vf = rep.getVariantFolder();
			try
			{
				if(rep==null)
					throw new RuntimeException("why is rep null???");
				if(rep.vf==null)
				{
					System.err.println("Warning: rep.vf is null for some reason");
				}
				if(rep.vf==null || rep.vf.equals(""))	
					rep.vf=rep.getAlreadyCompiled().getValue();
				System.out.println("VF Value: "+rep.vf);
				
				//String libtrunc = Configuration.libs.substring(0, Configuration.libs.lastIndexOf(":")); //this line is causing problems: Configuration.libs.lastIndexOf(":") is returning -1, : isn't always in the string
				int lastIndexOfColonInLibs = Configuration.libs.lastIndexOf(":");
				//String libtrunc = lastIndexOfColonInLibs == -1 ? Configuration.libs : Configuration.libs.substring(0, lastIndexOfColonInLibs);
				//String libtrunc = Configuration.libs; //no truncation for now
				
				if (positiveTestsDaikonSampleArgForm == null || negativeTestsArgForm == null)
					setupArgForms();
				
				CommandLine command1 = CommandLine.parse("cp -r "+Configuration.classTestFolder+" .");
				CommandLine command2 = CommandLine.parse("sh checker.sh "
							+positiveTestsDaikonSampleArgForm+" "
							+".:tmp/d_"+rep.vf+"/:"+ Main.GP4J_HOME+"/target/classes/" + ":" + Configuration.classTestFolder + ":" + Main.JUNIT_AND_HAMCREST_PATH + " "
							+ rep.getVariantFolder()+"pos" + " " + 
							"NOTORIG" + " " 
							+ Main.GP4J_HOME + " " + Main.JAVA8_HOME + " " + Main.DAIKON_HOME);
				System.err.println(command2.toString());
				CommandLine command3 = CommandLine.parse("sh checker.sh "
						+negativeTestsArgForm+" "
						+".:tmp/d_"+rep.vf+"/:"+ Main.GP4J_HOME+"/target/classes/" + ":" + Configuration.classTestFolder + ":" + Main.JUNIT_AND_HAMCREST_PATH + " "
						+ rep.getVariantFolder()+"neg" + " " + 
						"NOTORIG" + " " 
						+ Main.GP4J_HOME + " " + Main.JAVA8_HOME + " " + Main.DAIKON_HOME);
				
				
				
				//System.out.println("command: " + command2.toString());
				ExecuteWatchdog watchdog = new ExecuteWatchdog(10*60000);
				//timeout after 10 minutes, shouldn't be needed as there's timeouts in the JUnit tests. This hard timeout should be avoided as it can create zombie JUnit processes
				//thus, this timeout should be short
				DefaultExecutor executor = new DefaultExecutor();
				String workingDirectory = System.getProperty("user.dir");
				executor.setWorkingDirectory(new File(workingDirectory));
				executor.setWatchdog(watchdog);

				ByteArrayOutputStream out = new ByteArrayOutputStream();
				executor.setExitValue(0);

				executor.setStreamHandler(new PumpStreamHandler(out));
				FitnessValue posFit = new FitnessValue();

				try {
					executor.execute(command1);
					executor.execute(command2);
					String[] args = {rep.getVariantFolder()+"pos"};
					Aggregator.main(args);
					
					executor.execute(command3);
					args[0] = rep.getVariantFolder()+"neg";
					Aggregator.main(args);
					//String[] modifyArgs = {"MultiTestRunner", rep.getVariantFolder()+"pos"};
					//Modify.main(modifyArgs);
					//executor.execute(CommandLine.parse("java -cp "+".:tmp/d_"+rep.vf+"/:"+ Main.GP4J_HOME+"/target/classes/" + ":" + Configuration.classTestFolder + ":" + Main.JUNIT_AND_HAMCREST_PATH+" ylyu1.wean.MultiTestRunner "+positiveTestsDaikonSampleArgForm));
					out.flush();
					String output = out.toString();
					System.out.println(output);
					out.reset();
					//goodVariant.add(true);
					rep.isGoodForCheck=true;
					goodRepsForCheck.add(rep);
				} catch (ExecuteException exception) {
					//posFit.setAllPassed(false);
					System.out.println(exception.toString());
					//out.flush();
					System.out.println(out.toString());
					throw exception;
					
				} catch (Exception e) {
				} finally {
					if (out != null)
						try {
							out.close();
						} catch (IOException e) {
						}
				}
			}
			catch(Exception e)
			{
				System.out.println("ERROR!!!!!! "+rep.vf);
				e.printStackTrace();
				//goodVariant.add(false);
				rep.isGoodForCheck=false;
				if(!rep.vf.equals(""))Fitness.invariantCache.put(rep.hashCode(), null);
			}
		}
		
		Aggregator.bs.resizeAll();
		Aggregator.bs.doubleUp();
		
		for(int i = 0; i < goodRepsForCheck.size(); i++)
		{
			Fitness.invariantCache.put(goodRepsForCheck.get(i).hashCode(), Aggregator.bs.grid.get(i));
		}
		
		for(Representation<? extends EditOperation> r : notRepsForCheck)
		{
			Aggregator.bs.grid.add(Fitness.invariantCache.get(r.hashCode()));
		}
		
		Aggregator.bs.resizeAll();
		
		double[] scores = Aggregator.bs.getScores();
		
		for(int i = 0; i < goodRepsForCheck.size(); i++)
		{
			if(Configuration.invariantCheckerMode==2)
			{
				goodRepsForCheck.get(i).setFitness(scores[i]/10*(11-turn)+goodRepsForCheck.get(i).getFitness()/10*(turn-1));
			}
			else 
			{
				goodRepsForCheck.get(i).setFitness(scores[i]);
			}
		}
		
		for(int i = 0; i < notRepsForCheck.size(); i++)
		{
			if(Configuration.invariantCheckerMode==2)
			{
				notRepsForCheck.get(i).setFitness(scores[goodRepsForCheck.size()+i]/10*(11-turn)+notRepsForCheck.get(i).getFitness()/10*(turn-1));
			}
			else 
			{
				notRepsForCheck.get(i).setFitness(scores[goodRepsForCheck.size()+i]);
			}
				
		}
		
		System.out.println(scores.toString());
		//try{System.out.println(Arrays.toString(analyzeResults(pop)));}catch(Exception e) {System.out.println(e.toString());}
		//return checked;
		
	}
	
	
	
	public static int[] analyzeResults(Population<? extends EditOperation> pop) throws Exception
	{
		
		ArrayList<byte[]> templist = new ArrayList<byte[]>();
		ArrayList<Representation<? extends EditOperation>> repstorer = new ArrayList<Representation<? extends EditOperation>>();
		int max = 0;
		byte[] b = null;
		System.out.println("Entering first loop");
		for(Representation<? extends EditOperation> rep : pop)
		{
			if((!rep.getVariantFolder().equals(""))&&rep.isGoodForCheck)
			{
				try{b = tnsFetcher(rep.vf+"pos");}catch(Exception e) {System.out.println("BAD: "+rep.vf);}
				templist.add(b);
				if(b.length>max)max=b.length;
				try{b = tnsFetcher(rep.vf+"neg");}catch(Exception e) {System.out.println("BAD: "+rep.vf);}
				templist.add(b);
				if(b.length>max)max=b.length;
				repstorer.add(rep);
			}
			else
			{
				rep.setFitness(0.0);
			}
		}
		ArrayList<byte[]> list = new ArrayList<byte[]>();
		System.out.println("Entering second loop: templist length: " + templist.size()+" max: "+max);
		for(int i = 0; i < templist.size(); i+=2)
		{
			b = newByteArray2(2*max);
			for(int j = 0; j < templist.get(i).length; j++)
			{
				b[2*j]=templist.get(i)[j];
			}
			for(int j = 0; j < templist.get(i+1).length; j++)
			{
				b[2*j+1]=templist.get(i+1)[j];
			}
			
			Fitness.invariantCache.put(repstorer.get(i/2).hashCode(),b);
		}
		repstorer = new ArrayList<Representation<? extends EditOperation>>();
		System.out.println("Entering third loop");
		for(Representation<? extends EditOperation> rep : pop)
		{
			if(Fitness.invariantCache.containsKey(rep.hashCode()))
			{
				b=Fitness.invariantCache.get(rep.hashCode());
				
				if(b!=null)
				{
					repstorer.add(rep);
					//System.out.println(Arrays.toString(b));
					list.add(b);
				}
			}
			else
			{
				System.out.println("Should not happen");
			}
		}
		int[] diffScores =  Fitness.getStringDiffScore(list);
		int max1 = 0;
		for(int a: diffScores)
		{
			if(a>max1)max1=a;
		}
		if(max1==0)return diffScores;
		for(int i = 0; i < repstorer.size();i++)
		{
			if(Configuration.invariantCheckerMode==2)
			{	
				repstorer.get(i).setFitness(((double)diffScores[i])/((double)max1)/10*(11-turn)+repstorer.get(i).getFitness()/10*(turn-1)); 
			}
			else
			{
				repstorer.get(i).setFitness(((double)diffScores[i])/((double)max1));
			}
		}
		return diffScores;
	}
	
	public static byte[] newByteArray2(int size)
	{
		byte[] b = new byte[size];
		for(int i = 0; i < size; i++)
		{
			b[i]=2;
		}
		return b;
	}
	
	public static byte[] tnsFetcher(String s) throws Exception
	{
		ObjectInputStream ois = new ObjectInputStream(new FileInputStream(s+".tns"));
		byte[] b = (byte[]) ois.readObject();
		ois.close();
		return b;
	}
	
	//Deprecated
	public static void checkInvariantOrig()
	{
		try
			{
				if (positiveTestsDaikonSampleArgForm == null || negativeTestsArgForm == null)
					setupArgForms();
			
				CommandLine command1 = CommandLine.parse("cp -r "+Configuration.classTestFolder+" .");
				CommandLine command2 = CommandLine.parse("sh checker.sh "+positiveTestsDaikonSampleArgForm+" "+Configuration.libs+":.:"+Main.GP4J_HOME+"/target/classes/ origPos ORIGPOS"
							+ " " + Main.GP4J_HOME + " " + Main.JAVA8_HOME + " " + Main.DAIKON_HOME);
				CommandLine command3 = CommandLine.parse("sh checker.sh "+negativeTestsArgForm+" "+Configuration.libs+":.:"+Main.GP4J_HOME+"/target/classes/ origNeg ORIGNEG"
						 	+ " " + Main.GP4J_HOME + " " + Main.JAVA8_HOME + " " + Main.DAIKON_HOME);
				
				System.out.println("command: " + command2.toString());
				
				ExecuteWatchdog watchdog = new ExecuteWatchdog(1000000);
				DefaultExecutor executor = new DefaultExecutor();
				String workingDirectory = System.getProperty("user.dir");
				executor.setWorkingDirectory(new File(workingDirectory));
				executor.setWatchdog(watchdog);

				ByteArrayOutputStream out = new ByteArrayOutputStream();
				executor.setExitValue(0);

				executor.setStreamHandler(new PumpStreamHandler(out));
				FitnessValue posFit = new FitnessValue();

				try {
					executor.execute(command1);
					executor.execute(command2);
					executor.execute(command3);
					out.flush();
					String output = out.toString();
					System.out.println(output);
					out.reset();

				} catch (ExecuteException exception) {
					//posFit.setAllPassed(false);
					out.flush();
					System.out.println(out.toString());
					throw exception;
					
				} catch (Exception e) {
				} finally {
					if (out != null)
						try {
							out.close();
						} catch (IOException e) {
						}
				}
			}
			catch(Exception e)
			{
				
			}
		
		
	}
	
	public static void runDaikon()
	{
		if (positiveTestsDaikonSampleArgForm == null || negativeTestsArgForm == null)
			setupArgForms();
		CommandLine command0 = CommandLine.parse("cp "+Main.GP4J_HOME+"/checker.sh .");
		CommandLine command1 = CommandLine.parse("cp "+Main.GP4J_HOME+"/runDaikon.sh .");
		CommandLine command2 = CommandLine.parse("sh runDaikon.sh "+positiveTestsDaikonSampleArgForm+" "+Configuration.pathToNoTimeoutTests+":"+Configuration.classSourceFolder+":"+Configuration.testClassPath+":"+Main.GP4J_HOME+"/target/classes/"
					+ " " + Main.GP4J_HOME + " " + Main.JAVA8_HOME + " " + Main.DAIKON_HOME);
		
		//System.out.println("command: " + command2.toString());
		ExecuteWatchdog watchdog = new ExecuteWatchdog(Math.max(Fitness.positiveTestsDaikonSample.size()*5*60000, 60*60000)); //set a timeout of 5 minutes per test case, or 60 minutes, whichever is longer
		DefaultExecutor executor = new DefaultExecutor();
		String workingDirectory = System.getProperty("user.dir");
		executor.setWorkingDirectory(new File(workingDirectory));
		executor.setWatchdog(watchdog);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		executor.setExitValue(0);

		executor.setStreamHandler(new PumpStreamHandler(out));
		FitnessValue posFit = new FitnessValue();

		try {
			executor.execute(command0);
			executor.execute(command1);
			executor.execute(command2);
			out.flush();
			String output = out.toString();
			System.out.println(output);
			out.reset();
			String[] weanParserConfig = {"MultiTestRunner", "NOTDEBUG"};
			WeanParse.main(weanParserConfig);

		} catch (ExecuteException exception) {
			//posFit.setAllPassed(false);
			System.out.println(exception.toString());
			String output = out.toString();
			System.out.println(output);
			DataProcessor.storeError("rundaikon");
			Runtime.getRuntime().exit(1);
		} catch (Exception e) {
		} finally {
			if (out != null)
				try {
					out.flush();
					out.close();
				} catch (IOException e) {
				}
		}
		
	}

}