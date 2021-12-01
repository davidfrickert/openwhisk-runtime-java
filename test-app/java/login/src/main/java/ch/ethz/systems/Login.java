package ch.ethz.systems;

import com.google.gson.JsonObject;
import com.mongodb.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class Login {

	private static MongoClient createconn() {
		try {
			MongoCredential credential = MongoCredential.createCredential("root", "mydatabase", "root".toCharArray());
			MongoCredential credentialAdmin = MongoCredential.createCredential("root", "admin", "root".toCharArray());
			return new MongoClient(new ServerAddress("146.193.41.231", 27017), List.of(credential, credentialAdmin));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
    
	private static MongoClient getConn(ConcurrentHashMap<String, Object> cglobals) {
		MongoClient con = null;
		String key = String.format("mongo-%d",Thread.currentThread().getId());
    	if (!cglobals.containsKey(key)) {
    		con = createconn();
    		cglobals.put(key, con);
    	} else {
    		con = (MongoClient) cglobals.get(key);
    	}
    	return con;
	}
	
	public static boolean login(ConcurrentHashMap<String, Object> globals, String username, String password) {
		MongoClient mc = getConn(globals);
		var database = mc.getDatabase("mydatabase");
		var collection = database.getCollection("users");
		var query = new BasicDBObject("username", username);
		var findIterable = collection.find(query);

		return findIterable.cursor().next().getString("password").equals(password);
	}
    
    public static JsonObject main(JsonObject args, Map<String, Object> globals, int id) {
    	ConcurrentHashMap<String, Object> cglobals = (ConcurrentHashMap<String, Object>) globals;
    	JsonObject response = new JsonObject();
        long time = System.currentTimeMillis();

        if (login(cglobals, args.getAsJsonPrimitive("username").getAsString(), args.getAsJsonPrimitive("password").getAsString())) {
        	response.addProperty("succeeded", "true");
        } else {
        	response.addProperty("succeeded", "false");
        }
            	
    	response.addProperty("time", System.currentTimeMillis() - time);
    	return response;
    }
    
    public static void main(String[] args) throws Exception {
    	ConcurrentHashMap<String, Object> cglobals = new ConcurrentHashMap<String, Object>();
    	System.out.println(login(cglobals, "username1", "password1"));
    	System.out.println(login(cglobals, "username1", "password2"));
    }
}


