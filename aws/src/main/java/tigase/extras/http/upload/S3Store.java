/*
 * Tigase Server Extras for AWS - Extra modules to Tigase Server
 * Copyright (C) 2007 Tigase, Inc. (office@tigase.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 */
package tigase.extras.http.upload;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.*;
import tigase.http.upload.store.Store;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.config.ConfigField;
import tigase.kernel.beans.config.ConfigurationChangedAware;
import tigase.xmpp.jid.BareJID;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Bean(name = "store", active = true, exportable = true)
public class S3Store implements Store, ConfigurationChangedAware {

	private static Logger log = Logger.getLogger(S3Store.class.getCanonicalName());

	protected AmazonS3 s3;

	@ConfigField(desc = "AWS region")
	private Regions region;
	@ConfigField(desc = "S3 bucket")
	protected String bucket;
	@ConfigField(desc = "S3 bucket key prefix")
	private String bucketKeyPrefix;
	@ConfigField(desc = "Autocreate bucket")
	protected boolean autocreateBucket = false;
	@ConfigField(desc = "S3 access key id")
	protected String accessKeyId;
	@ConfigField(desc = "S3 secret key")
	protected String secretKey;
	@ConfigField(desc = "S3 endpoint url")
	protected String endpointUrl;
	@ConfigField(desc = "AWS path style access")
	protected Boolean pathStyleAccess = null;
	@ConfigField(desc = "S3 signer override")
	protected String signerOverride;

	public String getRegion() {
		if (region != null) {
			return region.getName();
		}
		return null;
	}

	public void setRegion(String region) {
		if (region == null) {
			this.region = null;
		} else {
			this.region = Regions.fromName(region);
		}
	}

	@Override
	public long count() throws IOException {
		try {
			return s3.listObjectsV2(bucket, bucketKeyPrefix).getKeyCount();
		} catch (AmazonServiceException ex) {
			throw new IOException("Could not count files", ex);
		}
	}

	@Override
	public long size() throws IOException {
		try {
			return s3.listObjectsV2(bucket, bucketKeyPrefix).getObjectSummaries().stream().mapToLong( summary -> summary.getSize() ).sum();
		} catch (AmazonServiceException ex) {
			throw new IOException("Could not count files", ex);
		}
	}

	@Override
	public ReadableByteChannel getContent(BareJID uploader, String slotId, String filename) throws IOException {
		try {
			// encode filename to make GET request URI
			return Channels.newChannel(
					s3.getObject(new GetObjectRequest(bucket, createKey(slotId, URLEncoder.encode(filename, StandardCharsets.UTF_8)))).getObjectContent());
		} catch (AmazonServiceException ex) {
			// as we changed encoding of keys of uploaded files now, we should (for older files) keys which were not encoded..
			try {
				return Channels.newChannel(s3.getObject(new GetObjectRequest(bucket, createKey(slotId,
						filename))).getObjectContent());
			} catch (AmazonServiceException ex1) {
				throw new IOException("Could not download the file " + slotId + " from S3", ex);
			}
		}
	}

	@Override
	public void setContent(BareJID uploader, String slotId, String filename, long size, ReadableByteChannel source)
			throws IOException {
		File tmp = File.createTempFile("upload-", ".tmp");
		try (FileChannel destination = FileChannel.open(tmp.toPath(), StandardOpenOption.CREATE,
														StandardOpenOption.WRITE)) {
			destination.transferFrom(source, 0, size);
		}
		try {
			// encode filename to make GET request URI
			s3.putObject(bucket, createKey(slotId, URLEncoder.encode(filename, StandardCharsets.UTF_8)), tmp);
		} catch (AmazonServiceException ex) {
			tmp.delete();
			throw new IOException("Could not upload the file " + slotId + " to S3", ex);
		}
	}

	@Override
	public void remove(BareJID uploader, String slotId) throws IOException {
		try {
			List<DeleteObjectsRequest.KeyVersion> toRemove = s3.listObjectsV2(bucket, createKeyPrefix(slotId))
					.getObjectSummaries()
					.stream()
					.map(S3ObjectSummary::getKey)
					.map(DeleteObjectsRequest.KeyVersion::new)
					.collect(Collectors.toList());
			if (!toRemove.isEmpty()) {
				s3.deleteObjects(new DeleteObjectsRequest(bucket).withKeys(toRemove));
			}
		} catch (AmazonServiceException ex) {
			throw new IOException("Could not remove file " + slotId + " from S3", ex);
		}
	}

	@Override
	public void beanConfigurationChanged(Collection<String> collection) {
		log.log(Level.CONFIG, "Initiating S3 storage at " + region + ", collection: " + collection);
		AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard();
		if (pathStyleAccess != null) {
			builder.setPathStyleAccessEnabled(pathStyleAccess);
		}
		if (endpointUrl != null && (!endpointUrl.isEmpty())) {
			builder.withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endpointUrl, Optional.ofNullable(region).orElse(Regions.US_EAST_1).getName()));
		} else if (region != null) {
			builder.withRegion(region);
		}
		if (signerOverride != null && (!signerOverride.isEmpty())) {
			ClientConfiguration clientConfiguration = new ClientConfiguration();
			clientConfiguration.setSignerOverride(signerOverride);
			builder.withClientConfiguration(clientConfiguration);
		}
		if (accessKeyId != null && (!accessKeyId.isEmpty())&& secretKey != null && (!secretKey.isEmpty())) {
			builder.setCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKeyId, secretKey)));
		}
		Optional.ofNullable(s3).ifPresent(AmazonS3::shutdown);
		s3 = builder.build();
		log.log(Level.CONFIG, "Initiated S3 storage at " + s3.getRegionName());

		if (!s3.doesBucketExistV2(bucket)) {
			if (log.isLoggable(Level.FINE)) {
				log.log(Level.FINE, "S3 bucket " + bucket + " does not exist");
			}
			if (!autocreateBucket) {
				if (log.isLoggable(Level.WARNING)) {
					log.log(Level.WARNING, "S3 bucket " + bucket + " does not exist and automatic creation of bucket is not enabled. File storage in S3 will not work!");
				}
			} else {
				if (log.isLoggable(Level.FINE)) {
					log.log(Level.FINE, "S3 bucket " + bucket + " does not exist, trying automatic creation of a bucket..");
				}
				try {
					CreateBucketRequest request = new CreateBucketRequest(bucket);
					request.setCannedAcl(CannedAccessControlList.PublicRead);
					s3.createBucket(request);
				} catch (AmazonServiceException ex) {
					if (log.isLoggable(Level.WARNING)) {
						log.log(Level.WARNING, "Automatic creation of S3 bucket " + bucket +
								" failed. File storage in S3 will not work!", ex);
					}
				}
			}
		}

	}

	protected String createKeyPrefix(String slotId) {
		if (bucketKeyPrefix == null || bucketKeyPrefix.isBlank()) {
			return slotId;
		} else {
			return bucketKeyPrefix + "/" + slotId;
		}
	}

	protected String createKey(String slotId, String filename) {
		return createKeyPrefix(slotId) + "/" + filename;
	}
}
