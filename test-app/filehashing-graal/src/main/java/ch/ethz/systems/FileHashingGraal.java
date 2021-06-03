package ch.ethz.systems;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.minio.MinioClient;
import java.io.InputStream;
import java.security.MessageDigest;
import javax.xml.bind.DatatypeConverter;

public class FileHashingGraal {
	private static final ObjectMapper mapper = new ObjectMapper();

	private static final int size = 2*1024*1024;
	private static final String storage = "http://r630-01:9000";

	private static MinioClient createconn() {
		try {
			return new MinioClient(storage, "minio", "minio123");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	// TODO - is this Minioclient thread safe?
	private static String run(MinioClient minioClient, int seed, byte[] buffer) {
		try {
			InputStream stream = minioClient.getObject("files", String.format("file-%d.dat", seed));
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

	private static MinioClient getConn() {
		return createconn();
	}

	private static byte[] getBuffer() {
		return new byte[size];
	}


	public static ObjectNode main(JsonNode args) {
		String hash = null;
		long time = System.currentTimeMillis();

		if (args.has("seed")) {
			hash = run(getConn(), args.get("seed").asInt(), getBuffer());
		}

		ObjectNode response = mapper.createObjectNode();
		response.put("hash", hash);
		response.put("time", System.currentTimeMillis() - time);
		return response;
	}

}
