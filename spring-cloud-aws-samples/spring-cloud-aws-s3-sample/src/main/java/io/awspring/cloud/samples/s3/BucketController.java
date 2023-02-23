package io.awspring.cloud.samples.s3;

import io.awspring.cloud.s3.S3Operations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * An example on how to create and delete S3 buckets.
 */
@RestController
@RequestMapping("/bucket")
public class BucketController {
	private final S3Operations s3Operations;

	public BucketController(S3Operations s3Operations) {
		this.s3Operations = s3Operations;
	}

	@PutMapping
	ResponseEntity createBucket(@RequestParam String bucketName) {

		String location=s3Operations.createBucket(bucketName);
		return new ResponseEntity<>("Bucket created at : "+location, HttpStatus.CREATED);

	}

	@DeleteMapping
	ResponseEntity deleteBucket(@RequestParam String bucketName) {
		s3Operations.deleteBucket(bucketName);
		return new ResponseEntity<>("Bucket "+bucketName+" deleted", HttpStatus.NO_CONTENT);
	}

}
