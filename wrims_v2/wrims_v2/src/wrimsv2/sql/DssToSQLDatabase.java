package wrimsv2.sql;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import vista.db.dss.DSSUtil;
import vista.set.DataReference;
import vista.set.DataSet;
import vista.set.DataSetAttr;
import vista.set.Group;
import vista.set.RegularTimeSeries;
import wrimsv2.components.ControlData;
import wrimsv2.components.FilePaths;
import wrimsv2.evaluator.CsvOperation;
import wrimsv2.evaluator.DataTimeSeries;
import wrimsv2.evaluator.DssDataSet;
import wrimsv2.evaluator.DssDataSetFixLength;
import wrimsv2.evaluator.DssOperation;
import wrimsv2.hdf5.DSSHDF5Converter;

public class DssToSQLDatabase {

	public static String dssInfoFilePath="";
	private ArrayList<String[]> dssFilePathArr=new ArrayList<String[]> ();
	
	public DssToSQLDatabase(String[] args) {
		new DataBaseProfile(args);
		if (getDssFilePaths()){
			for (int i=0; i<dssFilePathArr.size(); i++){
				String[] dssFilePaths = dssFilePathArr.get(i);
				setSQLAttr(dssFilePaths[0]);
				dssReader(dssFilePaths);
				convertData();
			}
		}
		System.exit(0);
	}
	
	public static void main(String[] args){
		new DssToSQLDatabase(args);
	}
	
	public void setSQLAttr(String dvFilePath){
		FilePaths.fullDvarDssPath=dvFilePath;
		int index1=dvFilePath.lastIndexOf(File.separator);
		int index2=dvFilePath.lastIndexOf(".");
		FilePaths.dvarDssDirectory=dvFilePath.substring(0,index1+1);
		FilePaths.sqlScenarioName=dvFilePath.substring(index1+1,index2);
	}
	
	public void convertData(){
		ControlData.isSimOutput=false;
		if (ControlData.databaseURL.startsWith("jdbc:sqlserver")){
			SQLServerRWriter sqlServerRWriter = new SQLServerRWriter();
			sqlServerRWriter.process();
		}else if (ControlData.databaseURL.startsWith("jdbc:mysql")){
			MySQLRWriter mySqlRWriter = new MySQLRWriter();
			mySqlRWriter.process();
		}else{
			CsvOperation co = new CsvOperation();
			int index = FilePaths.fullDvarDssPath.lastIndexOf(".");
			String csvPath = FilePaths.fullDvarDssPath.substring(0, index)+".csv";
			co.ouputCSV(csvPath, 0);
		}
	}
	
	public boolean getDssFilePaths(){
		dssFilePathArr=new ArrayList<String[]> ();
		File dssInfoFile = new File (dssInfoFilePath);
		try {
			FileInputStream fs = new FileInputStream(dssInfoFile.getAbsolutePath());
			BufferedReader br = new BufferedReader(new InputStreamReader(fs));
		    String line=br.readLine();
		    if (br == null) return false;
			while((line=br.readLine()) !=null){
		    	line=line.trim();
		    	if (line.equals("")) return true;
		    	String[] dssFilePaths = new String[3];
		    	String[] parts = line.split(",");
		    	if (parts.length>=5){
		    		dssFilePaths[0]=parts[0];
		    		dssFilePaths[1]=parts[1];
		    		dssFilePaths[2]=parts[2];
		    		ControlData.partA=parts[3];
		    		ControlData.svDvPartF=parts[4];
		    		dssFilePathArr.add(dssFilePaths);
		    	}	
		    }
		    br.close();
		    fs.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return true;
		
	}
	
	public void dssReader(String[] dssFilePaths){
		System.out.println("Reading DV, SV, and Init Dss Files...");
		DataTimeSeries.dvAliasTS = readDssDataFixLength(dssFilePaths[0]);
		System.out.println("Reading DV Dss File Done");
		DataTimeSeries.svTS = readDssData(dssFilePaths[1]);
		System.out.println("Reading SV Dss File Done");
		DataTimeSeries.dvAliasInit = readDssData(dssFilePaths[2]);
		System.out.println("Reading Init Dss File Done");
	}
	
	public HashMap<String, DssDataSet>  readDssData(String dssPath){
		HashMap<String, DssDataSet> ddsMap=new HashMap<String, DssDataSet>();
		File dssFile=new File (dssPath);
		if (!dssFile.exists()) return ddsMap;
		DSSUtil.generateCatalog(dssPath);
		Group group = DSSUtil.createGroup("local", dssPath);
		int size = group.getNumberOfDataReferences();
		for (int i=0; i<size; i++){
			DataReference ref = group.getDataReference(i);
			DataSet ds = ref.getData();
			RegularTimeSeries rts=(RegularTimeSeries)ds;
			DssDataSet dds= new DssDataSet();
			ArrayList<Double> dataArray= new ArrayList<Double>();
			Date startDate=rts.getStartTime().getDate();
			int year=startDate.getYear()+1900;
			int month=startDate.getMonth();;
			int day = startDate.getDate();
			for (double dataEntry :  rts.getYArray()){
				dataArray.add(dataEntry);
			}
			DataSetAttr attr = rts.getAttributes();
			dds.setUnits(attr.getYUnits());
			dds.setKind(attr.getTypeName());
	        dds.setData(dataArray);
	        String timeStep=rts.getTimeInterval().toString();
	        dds.setTimeStep(timeStep);
	        dds.setStartTime(startDate);
	        dds.setFromDssFile(true);
	        dds.generateStudyStartIndex();
	        String name=attr.getLocationName();
	        String entryNameTS=DssOperation.entryNameTS(name, timeStep);
	        ddsMap.put(entryNameTS, dds);
		}
		return ddsMap;
	}
	
	public HashMap<String, DssDataSetFixLength> readDssDataFixLength(String dssPath){
		HashMap<String, DssDataSetFixLength> ddsMap=new HashMap<String, DssDataSetFixLength>();
		File dssFile=new File (dssPath);
		if (!dssFile.exists()) return ddsMap;
		DSSUtil.generateCatalog(dssPath);
		Group group = DSSUtil.createGroup("local", dssPath);
		int size = group.getNumberOfDataReferences();
		for (int i=0; i<size; i++){
			DataReference ref = group.getDataReference(i);
			DataSet ds = ref.getData();
			RegularTimeSeries rts=(RegularTimeSeries)ds;
			DssDataSetFixLength dds= new DssDataSetFixLength();
			double[] yArray = rts.getYArray();
			Date startDate=rts.getStartTime().getDate();
			int year=startDate.getYear()+1900;
			int month=startDate.getMonth();;
			int day = startDate.getDate();
			int j=0;
			double[] data = yArray;
			DataSetAttr attr = rts.getAttributes();
			dds.setUnits(attr.getYUnits());
			dds.setKind(attr.getTypeName());
	        dds.setData(data);
	        String timeStep=rts.getTimeInterval().toString();
	        dds.setTimeStep(timeStep);
	        dds.setStartTime(startDate);
	        dds.setFromDssFile(true);
	        String name=attr.getLocationName();
	        String entryNameTS=DssOperation.entryNameTS(name, timeStep);
	        ddsMap.put(entryNameTS, dds);
		}
		return ddsMap;
	}
}
