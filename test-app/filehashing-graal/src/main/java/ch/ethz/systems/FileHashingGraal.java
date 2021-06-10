package ch.ethz.systems;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.InsufficientDataException;
import io.minio.errors.InternalException;
import io.minio.errors.InvalidResponseException;
import io.minio.errors.ServerException;
import io.minio.errors.XmlParserException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import javax.xml.bind.DatatypeConverter;
import okhttp3.Cache;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.internal.http.CallServerInterceptor;
import org.jetbrains.annotations.NotNull;

public class FileHashingGraal {
	private static final ObjectMapper mapper = new ObjectMapper();

	private static final int size = 2*1024*1024;
	private static final String storage = "http:///10.147.18.27:8999";

	private static MinioClient connect(final OkHttpClient httpClient) {
		try {
			return MinioClient.builder()
							  .credentials("minio", "minio123")
							  .endpoint(storage)
							  .httpClient(httpClient)
							  .build();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	// TODO - is this Minioclient thread safe?
	private static String run(MinioClient minioClient, int seed) throws
																				ServerException,
																				InsufficientDataException,
																				ErrorResponseException,
																				IOException,
																				NoSuchAlgorithmException,
																				InvalidKeyException,
																				InvalidResponseException,
																				XmlParserException,
																				InternalException {
		final var buffer = new byte[size];
		try (InputStream stream = minioClient.getObject(GetObjectArgs.builder()
																	 .bucket("files")
																	 .object(String.format("file-%d.dat", seed))
																	 .build())) {
			for (int bytesread = 0; bytesread < size; bytesread += stream.read(buffer, bytesread, size - bytesread));
		}
		return DatatypeConverter.printHexBinary(MessageDigest.getInstance("MD5").digest(buffer));
	}

	public static ObjectNode main(JsonNode args) {
		ObjectNode response = mapper.createObjectNode();

		String hash = null;
		long time = System.currentTimeMillis();

		if (args.has("seed")) {
			OkHttpClient client = null;
			try {
				try {
					client = create();
					hash = run(connect(client), args.get("seed").asInt());
				} finally {
					close(client);
				}
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

		for (Thread t : Thread.getAllStackTraces().keySet()) {
			System.out.println("WARN: Thread still running! " + t.toString());
		}
		return response;
	}

	private static OkHttpClient create() {
		final Duration timeoutBase = Duration.ofSeconds(15);
		final Interceptor logger = new Interceptor() {

			@NotNull
			@Override
			public Response intercept(@NotNull final Chain chain) throws IOException {
				var req = chain.request();
				System.out.println(req.toString());
				return chain.proceed(req);
			}
		};

		return new OkHttpClient().newBuilder()
								 .writeTimeout(timeoutBase)
								 .readTimeout(timeoutBase)
								 .callTimeout(timeoutBase)
								 .addInterceptor(logger)
								 .build();
	}

	private static void close(OkHttpClient client) throws IOException {
		if (client != null) {
			client.dispatcher().executorService().shutdownNow();
			client.connectionPool().evictAll();
			final Cache cache = client.cache();
			if (cache != null) {
				cache.close();
			}
		}
	}

}
