package org.lexgrid.lexevs.evs.relations.connection;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.LexGrid.LexBIG.Exceptions.LBException;
import org.LexGrid.LexBIG.Impl.LexBIGServiceImpl;
import org.LexGrid.LexBIG.LexBIGService.CodedNodeGraph;
import org.LexGrid.LexBIG.LexBIGService.LexBIGService;
import org.LexGrid.LexBIG.Utility.Constructors;
import org.lexevs.dao.database.access.association.model.Node;
import org.lexevs.dao.database.service.codednodegraph.CodedNodeGraphService;
import org.lexevs.locator.LexEvsServiceLocator;
import org.lexgrid.lexevs.evs.relations.connection.LexEVSGraphConnectImpl.AssociationRow;

import com.arangodb.ArangoDB;
import com.arangodb.ArangoDatabase;

public class LexEVSGraphConnectImpl {
	

	public class AssociationRow {
		private String associationPredicateGuid;
		private String sourceEntityCode;
		private String sourceEntityNamespace;
		private String targetEntityCode;
		private String targetEntityNamespace;
		
		public AssociationRow(String predicateGuid, 
				String sourceCode,
				String sourceNamespace,
				String targetCode,
				String targetNamespace){
			this.associationPredicateGuid = predicateGuid;
			this.sourceEntityCode = sourceCode;
			this.sourceEntityNamespace = sourceNamespace;
			this.targetEntityCode = targetCode;
			this.targetEntityNamespace = targetNamespace;
			
		}
		/**
		 * @return the associationPredicateGuid
		 */
		public String getAssociationPredicateGuid() {
			return associationPredicateGuid;
		}
		/**
		 * @param associationPredicateGuid the associationPredicateGuid to set
		 */
		public void setAssociationPredicateGuid(String associationPredicateGuid) {
			this.associationPredicateGuid = associationPredicateGuid;
		}
		/**
		 * @return the sourceEntityCode
		 */
		public String getSourceEntityCode() {
			return sourceEntityCode;
		}
		/**
		 * @param sourceEntityCode the sourceEntityCode to set
		 */
		public void setSourceEntityCode(String sourceEntityCode) {
			this.sourceEntityCode = sourceEntityCode;
		}
		/**
		 * @return the sourceEntityNamespace
		 */
		public String getSourceEntityNamespace() {
			return sourceEntityNamespace;
		}
		/**
		 * @param sourceEntityNamespace the sourceEntityNamespace to set
		 */
		public void setSourceEntityNamespace(String sourceEntityNamespace) {
			this.sourceEntityNamespace = sourceEntityNamespace;
		}
		/**
		 * @return the targetEntityCode
		 */
		public String getTargetEntityCode() {
			return targetEntityCode;
		}
		/**
		 * @param targetEntityCode the targetEntityCode to set
		 */
		public void setTargetEntityCode(String targetEntityCode) {
			this.targetEntityCode = targetEntityCode;
		}
		/**
		 * @return the targetEntityNamespace
		 */
		public String getTargetEntityNamespace() {
			return targetEntityNamespace;
		}
		/**
		 * @param targetEntityNamespace the targetEntityNamespace to set
		 */
		public void setTargetEntityNamespace(String targetEntityNamespace) {
			this.targetEntityNamespace = targetEntityNamespace;
		}
		
		public String toString(){
			return "PredicateGuid: " + associationPredicateGuid 
					+ " sourceCode: " + sourceEntityCode 
					+ " sourceNamespace: " + sourceEntityNamespace 
					+ " targetCode: " + targetEntityCode 
					+ " targetNamespace: " + targetEntityNamespace;
			
		}

	}

	public static void main(String[] args) {
		try {
			LexEVSGraphConnectImpl graph = new LexEVSGraphConnectImpl();
					List<AssociationRow> rows = graph.getAssociationRowsFromRoots(graph.getRoots(), graph.getPredicateUidForName("someName") );
//					rows.stream().forEachOrdered(System.out::println);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (LBException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	public void run(){
		ArangoDB arangoDB = new ArangoDB.Builder().host("127.0.0.1", 8529).user("root").password("lexgrid").build();
		arangoDB.getAccessibleDatabasesFor("root").stream().forEach(System.out::println);
		ArangoDatabase db = arangoDB.db("lexgraf");
		System.out.println(db.exists());
		arangoDB.getAccessibleDatabasesFor("root").stream().forEach(System.out::println);
		arangoDB.shutdown();
	}
	

	public List<AssociationRow> getAssociationRowsFromRoots(List<Node> roots, String predicateName) throws LBException, IOException {
		CodedNodeGraphService svc = LexEvsServiceLocator.getInstance().getDatabaseServiceManager().getCodedNodeGraphService();
		String predName = getPredicateUidForName(predicateName);
		List<AssociationRow> row = (List<AssociationRow>) roots
				.stream()
				.flatMap(x ->
		svc.getSourcesFromTarget("http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#", "18.05b", 
				x.getEntityCode(), 
				x.getEntityCodeNamespace(), 
				predName)
				.stream().filter(a -> !a.getEntityCode().startsWith("@"))
				.map(y -> new AssociationRow(predName, x.getEntityCode(), 
						x.getEntityCodeNamespace(), 
						y.getEntityCode(), 
						y.getEntityCodeNamespace())))
				.collect(Collectors.toList());
		
		return getRowFromTargets(row, row, predicateName, svc);
	}
	
	private List<AssociationRow> getRowFromTargets(List<AssociationRow> rows, List<AssociationRow> rowsToReturn, String predName, CodedNodeGraphService svc){
		List<AssociationRow> rowsToRecurse =	 rows
				.stream()
				.flatMap(x ->
		svc.getSourcesFromTarget("http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl#", "18.05b", 
				x.getTargetEntityCode(), 
				x.getTargetEntityNamespace(), 
				predName)
				.stream()
				.filter(a -> !a.getEntityCode().startsWith("@"))
				.map(y -> new AssociationRow(predName, x.getTargetEntityCode(), 
						x.getTargetEntityNamespace(), 
						y.getEntityCode(), 
						y.getEntityCodeNamespace())))
				.collect(Collectors.toList());
		if(rowsToRecurse != null && rowsToRecurse.size() > 0){
			rowsToReturn.addAll(rowsToRecurse);
			return getRowFromTargets(rowsToRecurse, rowsToReturn,  predName, svc);
		}else{
			return rowsToReturn;
		}
	}
	
	
	public List<Node> getRoots() throws IOException{
		 List<Node> nodes = new ArrayList<Node>();
	     String file ="resources/AnatPartRoots.csv";
	      
	     BufferedReader reader = new BufferedReader(new FileReader(file));
	     String line = reader.readLine();
	     while ((line = reader.readLine()) != null) {
	    	 String code = line.split(",")[0].replace("\"", "");
//	    	 System.out.println(code);
	    	 Node node = new Node();
	    	 node.setEntityCode(code);
	    	 node.setEntityCodeNamespace("ncit");
	    	 nodes.add(node);
	     }
	     reader.close();
	     return nodes;
	}
	
	public String getPredicateUidForName(String name){
		return "11482057";
	}

}
