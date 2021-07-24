package ch.ethz.systems;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;

import java.util.List;


public class Login {

	private static final ObjectMapper mapper = new ObjectMapper();

    private static MongoClient client;

    private static MongoClient createconn() {
        try {
			MongoCredential credential = MongoCredential.createCredential("root", "mydatabase", "root".toCharArray());
			MongoCredential credentialAdmin = MongoCredential.createCredential("root", "admin", "root".toCharArray());
            return new MongoClient(new ServerAddress("127.0.0.1", 27017), List.of(credential, credentialAdmin));
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
		var collection = database.getCollection("users");
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


