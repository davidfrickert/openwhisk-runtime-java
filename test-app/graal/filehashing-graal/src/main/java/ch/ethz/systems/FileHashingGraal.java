package ch.ethz.systems;

import com.dfrickert.simpleminioclient.SimpleMinioClient;
import com.dfrickert.simpleminioclient.auth.Credentials;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.*;
import java.lang.reflect.Array;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.bind.DatatypeConverter;

public class FileHashingGraal {
	private static final ObjectMapper mapper = new ObjectMapper();

	private static final int size = 2*1024*1024;
	private static final String storage = "http://146.193.41.231:8999";

	private static SimpleMinioClient minioClient;

	private static SimpleMinioClient connect(String minioLocation, Credentials accessCredentials) {
		try {
			return new SimpleMinioClient(minioLocation, accessCredentials);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	// TODO - is this Minioclient thread safe?
	private static String run(SimpleMinioClient minioClient, int seed) throws

			IOException,
			NoSuchAlgorithmException, InterruptedException {
		final var buffer = new byte[size];
		/*
		Request{method=GET, url=http://146.193.41.231:8999/files?location=, headers=[Host:146.193.41.231:8999, Accept-Encoding:identity, User-Agent:MinIO (Linux; amd64) minio-java/dev, Content-MD5:1B2M2Y8AsgTpgAmY7PhCfg==, x-amz-content-sha256:e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855, x-amz-date:20210610T171938Z, Authorization:AWS4-HMAC-SHA256 Credential=minio/20210610/us-east-1/s3/aws4_request, SignedHeaders=content-md5;host;x-amz-content-sha256;x-amz-date, Signature=1427e44b517bda011563c0fda359bd1338c5f33b5fc328a9030fc46b708b9487]}
		Request{method=GET, url=http://146.193.41.231:8999/files/file-1.dat, headers=[Host:146.193.41.231:8999, Accept-Encoding:identity, User-Agent:MinIO (Linux; amd64) minio-java/dev, Content-MD5:1B2M2Y8AsgTpgAmY7PhCfg==, x-amz-content-sha256:e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855, x-amz-date:20210610T171938Z, Authorization:AWS4-HMAC-SHA256 Credential=minio/20210610/us-east-1/s3/aws4_request, SignedHeaders=content-md5;host;x-amz-content-sha256;x-amz-date, Signature=b57b17c16f8b6479aab1daf6e5d5c7900110898766c25dd7e4cedbf6a0f1e731]}
		 */
		try (InputStream stream = minioClient.get("files",String.format("file-%d.dat", seed)).getEntity().getContent()) {
			for (int bytesread = 0; bytesread < size; bytesread += stream.read(buffer, bytesread, size - bytesread));
		}
		return DatatypeConverter.printHexBinary(MessageDigest.getInstance("MD5").digest(buffer));
	}

	public static ObjectNode main(JsonNode args) {
		ObjectNode response = mapper.createObjectNode();

		String hash = null;
		long time = System.currentTimeMillis();

		if (args.has("seed")) {
			final Instant beforeClientInit = Instant.now();

			if (minioClient == null) {
				minioClient =  connect(storage, new Credentials("minio", "minio123"));
			}
			final Duration clientInit = Duration.between(beforeClientInit, Instant.now());
			try {

				final Instant beforeRun = Instant.now();
				hash = run(minioClient, args.get("seed").asInt());
				final Duration run = Duration.between(beforeRun, Instant.now());

				final ArrayNode metrics = mapper.createArrayNode();

				metrics.add(mapper.createObjectNode()
						.put("name", "filehashing.httpclient.init")
						.put("value", clientInit.toMillis()));

				metrics.add(mapper.createObjectNode()
						.put("name", "filehashing.run")
						.put("value", run.toMillis()));

				response.set("metrics", metrics);
			} catch (Exception e) {
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				e.printStackTrace(pw);
				response.put("error", sw.toString());
				response.put("errorType", e.getClass().getName());
			}
		} else {
			response.put("error", "hash not supplied");
			response.set("request", args);
		}

		response.put("hash", hash);
		response.put("time", System.currentTimeMillis() - time);
		/*
		for (Thread t : Thread.getAllStackTraces().keySet()) {
			System.out.println("WARN: Thread still running! " + t.toString());
		}
		 */
		return response;
	}

}
