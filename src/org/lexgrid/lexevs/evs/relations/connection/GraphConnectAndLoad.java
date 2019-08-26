package org.lexgrid.lexevs.evs.relations.connection;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.LexGrid.LexBIG.Exceptions.LBException;
import org.lexevs.dao.database.access.association.model.Node;
import org.lexgrid.lexevs.evs.relations.connection.LexEVSGraphConnectImpl.AssociationRow;
import org.lexgrid.lexevs.evs.relations.model.LexVertex;
import org.lexgrid.lexevs.evs.relations.model.NodeEdge;

import com.arangodb.ArangoCursor;
import com.arangodb.ArangoDB;
import com.arangodb.ArangoDBException;
import com.arangodb.ArangoDatabase;
import com.arangodb.ArangoGraph;
import com.arangodb.ArangoVertexCollection;
import com.arangodb.entity.EdgeDefinition;
import com.arangodb.entity.EdgeEntity;
import com.arangodb.entity.GraphEntity;
import com.arangodb.entity.VertexEntity;
import com.arangodb.model.GraphCreateOptions;


public class GraphConnectAndLoad {
	
	private static HashMap<String, LexVertex> cache = new HashMap<String,LexVertex>();
	private static int vertexCount = 0;
	private static int edgeCount = 0;
	public static void main(String[] args) {
		GraphConnectAndLoad graphLoad = new GraphConnectAndLoad();
		graphLoad.initDB();

	}



		protected static final String TEST_DB = "lexgraf";
		protected static ArangoDB arangoDB;
		protected static ArangoDatabase db;
		protected static final String GRAPH_NAME = "Anatomic_Structure_Is_Physical_Part_Of";
		protected static final String EDGE_COLLECTION_NAME = "edges";
		protected static final String VERTEXT_COLLECTION_NAME = "vertlex";


		public void initDB() {
			if (arangoDB == null) {
				arangoDB = new ArangoDB.Builder().host("127.0.0.1", 8529).user("root").password("lexgrid").build();
				arangoDB.getAccessibleDatabasesFor("root").stream().forEach(System.out::println);
			}
			try {
				arangoDB.db(TEST_DB).drop();
			} catch (final ArangoDBException e) {
			}
			arangoDB.createDatabase(TEST_DB);
			GraphConnectAndLoad.db = arangoDB.db(TEST_DB);

			final Collection<EdgeDefinition> edgeDefinitions = new ArrayList<EdgeDefinition>();
			final EdgeDefinition edgeDefinition = new EdgeDefinition().collection(EDGE_COLLECTION_NAME)
					.from(VERTEXT_COLLECTION_NAME).to(VERTEXT_COLLECTION_NAME);
			edgeDefinitions.add(edgeDefinition);
			try {
				ArangoGraph graph = db.graph(GRAPH_NAME);
				graph.create(Arrays.asList(edgeDefinition), new GraphCreateOptions());
				db.getGraphs().stream().forEach(x -> System.out.println("Graph Name: " + x.getName()));
				LexEVSGraphConnectImpl lexConnect = new LexEVSGraphConnectImpl();
				long startLexResolve = System.currentTimeMillis();
				List<AssociationRow> rows = lexConnect.getAssociationRowsFromRoots(lexConnect.getRoots(), lexConnect.getPredicateUidForName("someName") );
				System.out.println("Time to resolve all roots for LexEVS: " + (System.currentTimeMillis() - startLexResolve));
				rows.stream().forEach(x -> createTriplefromAssocRow(x));
				System.out.println("Edge Count: " + edgeCount);
				System.out.println("Vertex Count: " + vertexCount);
				List<Node> rootNodes = lexConnect.getRoots();
				long startGraphdbRootResolve = System.currentTimeMillis();
				rootNodes.stream().forEach(root -> queryAllVerticesForRoot(root.getEntityCode()));
				System.out.println("Time to resolve all roots for Arrango graph: " + (System.currentTimeMillis() - startGraphdbRootResolve));
			} catch (final ArangoDBException ex) {
					ex.printStackTrace();

			} catch (LBException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			finally{
				shutdown();
			}
		}

		public void shutdown() {
	//		arangoDB.db(TEST_DB).drop();
			arangoDB.shutdown();
			arangoDB = null;
		}

		private static void createTriplefromAssocRow(AssociationRow row){
			LexVertex A = new LexVertex(row.getSourceEntityCode(), row.getSourceEntityNamespace());
			LexVertex B = new LexVertex(row.getTargetEntityCode(), row.getTargetEntityNamespace());
			ArangoVertexCollection collection = db.graph(GRAPH_NAME).vertexCollection(VERTEXT_COLLECTION_NAME);
			VertexEntity Aa = collection.getVertex(A.getCode(), VertexEntity.class);
			VertexEntity Bb = collection.getVertex(B.getCode(), VertexEntity.class) ;
			if(Aa == null){
				Aa = createVertex(A);
			}
			
			if(Bb == null){
				Bb = createVertex(B);
			}

			if(Aa == null || Bb == null){return;}
			storeEdge(new NodeEdge(Aa.getId(), Bb.getId(), false, true,
					row.getAssociationPredicateGuid()));
		}

		private static EdgeEntity storeEdge(final NodeEdge edge) throws ArangoDBException {
			edgeCount++;
			return db.graph(GRAPH_NAME).edgeCollection(EDGE_COLLECTION_NAME).insertEdge(edge);
		}

		private static VertexEntity createVertex(final LexVertex vertex) throws ArangoDBException {
//			if(cache.get(vertex.getCode()) == null){
				vertexCount++;
//				cache.put(vertex.getCode(), vertex);
			return db.graph(GRAPH_NAME).vertexCollection(VERTEXT_COLLECTION_NAME).insertVertex(vertex);
//			}
//			else{
//				return null;
//			}
		}
		
		public void queryAllVerticesForRoot(String root) throws ArangoDBException {
			String queryString = "FOR v IN 1..10000 OUTBOUND 'vertlex/" + root + "' GRAPH 'Anatomic_Structure_Is_Physical_Part_Of' RETURN v._key";
			ArangoCursor<String> cursor = db.query(queryString, null, null, String.class);
			Collection<String> result = cursor.asListRemaining();
//			System.out.println("Result size for root "+ root +": " + result.size());
//			result.stream().forEach(System.out::println);

//			queryString = "WITH vertlex FOR v IN 1..3 OUTBOUND 'vertlex/C12754' edges RETURN v._key";
//			cursor = db.query(queryString, null, null, String.class);
//			result = cursor.asListRemaining();
//			result.stream().forEach(System.out::println);
		}



}

