package ch.ethz.systems;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.io.InputStream;
import java.security.MessageDigest;
import javax.xml.bind.DatatypeConverter;

import com.dfrickert.simpleminioclient.SimpleMinioClient;
import com.dfrickert.simpleminioclient.auth.Credentials;
import com.google.gson.JsonObject;

public class FileHashing {

	private static final int size = 2*1024*1024;
	private static final String storage = "http://146.193.41.231:8999";

    private static SimpleMinioClient createconn() {
        try {
            return new SimpleMinioClient(storage, new Credentials("minio", "minio123"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // TODO - is this Minioclient thread safe?
	private static String run(SimpleMinioClient minioClient, int seed, byte[] buffer) {
		try {
			InputStream stream = minioClient.get("files", String.format("file-%d.dat", seed)).getEntity().getContent();
            for (int bytesread = 0;
                 bytesread < size;
                 bytesread += stream.read(buffer, bytesread, size - bytesread));
            stream.close();
            return DatatypeConverter.printHexBinary(MessageDigest.getInstance("MD5").digest(buffer));
		} catch(Exception e) {
			e.printStackTrace();
		}
        return null;
	}

	private static SimpleMinioClient getConn(ConcurrentHashMap<String, Object> cglobals) {
		SimpleMinioClient con = null;
		String key = String.format("minio-%d",Thread.currentThread().getId());
    	if (!cglobals.containsKey(key)) {
    		con = createconn();
    		cglobals.put(key, con);
    	} else {
    		con = (SimpleMinioClient) cglobals.get(key);
    	}
    	return con;
	}

	private static byte[] getBuffer(ConcurrentHashMap<String, Object> cglobals) {
		byte[] buffer = null;
		String key = String.format("buffer-%d",Thread.currentThread().getId());
    	if (!cglobals.containsKey(key)) {
    		buffer = new byte[size];
    		cglobals.put(key, buffer);
    	} else {
    		buffer = (byte[]) cglobals.get(key);
    	}
    	return buffer;
	}

    public static JsonObject main(JsonObject args, Map<String, Object> globals, int id) {
    	ConcurrentHashMap<String, Object> cglobals = (ConcurrentHashMap<String, Object>) globals;
    	String hash = null;
        long time = System.currentTimeMillis();


        if (args.has("seed")) {
    		hash = run(getConn(cglobals), args.getAsJsonPrimitive("seed").getAsInt(), getBuffer(cglobals));
    	}

    	JsonObject response = new JsonObject();
    	response.addProperty("hash", hash);
    	response.addProperty("time", System.currentTimeMillis() - time);
    	return response;
    }
}


