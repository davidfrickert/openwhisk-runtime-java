package ch.ethz.systems;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;


public class Login {

	private static final ObjectMapper mapper = new ObjectMapper();

    private static final String db = "mongodb://r630-01:27017";
    private static MongoClient client;

    private static MongoClient createconn() {
        try {
            return new MongoClient(new MongoClientURI(db));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
	private static synchronized MongoClient connectMongoDB() {
		if (client == null) {
			client = createconn();
		}
    	return client;
	}
	
	public static boolean login(String username, String password) {
		var mc = connectMongoDB();
		var database = mc.getDatabase("mydatabase");
		var collection = database.getCollection("customers");
		var query = new BasicDBObject("username", username);
		var findIterable = collection.find(query);

		return findIterable.cursor().next().getString("password").equals(password);
	}

	public static ObjectNode main(JsonNode args) {
    	ObjectNode response = mapper.createObjectNode();
        long time = System.currentTimeMillis();

        if (login(args.get("username").asText(), args.get("password").asText())) {
        	response.put("succeeded", "true");
        } else {
        	response.put("succeeded", "false");
        }
            	
    	response.put("time", System.currentTimeMillis() - time);
    	return response;
    }
}


