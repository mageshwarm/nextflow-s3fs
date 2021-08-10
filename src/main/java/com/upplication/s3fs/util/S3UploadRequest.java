/*
 * Copyright 2020, Seqera Labs
 * Copyright 2013-2019, Centre for Genomic Regulation (CRG)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.upplication.s3fs.util;

import java.util.List;
import java.util.Properties;

import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3ObjectId;
import com.amazonaws.services.s3.model.SSEAlgorithm;
import com.amazonaws.services.s3.model.StorageClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Model a S3 multipart upload request
 */
public class S3UploadRequest extends S3MultipartOptions<S3UploadRequest> {

    private static final Logger log = LoggerFactory.getLogger(S3UploadRequest.class);

    /**
     * ID of the S3 object to store data into.
     */
    private S3ObjectId objectId;

    /**
     * Amazon S3 storage class to apply to the newly created S3 object, if any.
     */
    private StorageClass storageClass;

    /**
     * Amazon S3 storage class to apply to the newly created S3 object, if any.
     */
    private String storageEncryptionKey;

    /**
     * Metadata that will be attached to the stored S3 object.
     */
    private ObjectMetadata metadata;



    public S3UploadRequest() {

    }

    public S3UploadRequest(Properties props) {
        super(props);
        setStorageClass(props.getProperty("upload_storage_class"));
        setStorageEncryption(props.getProperty("storage_encryption"));
        setStorageEncryptionKey(props.getProperty("storage_encryption_key"));
    }

    public S3ObjectId getObjectId() {
        return objectId;
    }

    public StorageClass getStorageClass() {
        return storageClass;
    }

    public String getStorageEncryptionKey() { return storageEncryptionKey; }

    public ObjectMetadata getMetadata() {
        return metadata;
    }


    public S3UploadRequest setObjectId(S3ObjectId objectId) {
        this.objectId = objectId;
        return this;
    }

    public S3UploadRequest setStorageClass(StorageClass storageClass) {
        this.storageClass = storageClass;
        return this;
    }

    public S3UploadRequest setStorageClass(String storageClass) {
        if( storageClass==null ) return this;

        try {
            setStorageClass( StorageClass.fromValue(storageClass) );
        }
        catch( IllegalArgumentException e ) {
            log.warn("Not a valid AWS S3 storage class: `{}` -- Using default", storageClass);
        }
        return this;
    }


    public S3UploadRequest setStorageEncryption(String storageEncryption) {
        if( storageEncryption == null) {
            return this;
        } else if(CommonUtils.isAES256Enabled(storageEncryption)) {
            ObjectMetadata objectMetadata = new ObjectMetadata();
            objectMetadata.setSSEAlgorithm(SSEAlgorithm.AES256.getAlgorithm());
            this.setMetadata(objectMetadata);
        } else if(CommonUtils.isKMSEnabled(storageEncryption)) {
            ObjectMetadata objectMetadata = new ObjectMetadata();
            objectMetadata.setSSEAlgorithm(SSEAlgorithm.KMS.getAlgorithm());
            this.setMetadata(objectMetadata);
        } else {
            log.warn("Not a valid S3 server-side encryption type: `{}` -- Currently only AES256 or aws:kms is supported", storageEncryption);
        }
        return this;
    }

    public S3UploadRequest setStorageEncryptionKey(String storageEncryptionKey) {
        if( storageEncryptionKey == null) {
            return this;
        }
        this.storageEncryptionKey = storageEncryptionKey;
        return this;
    }

    public S3UploadRequest setMetadata(ObjectMetadata metadata) {
        this.metadata = metadata;
        return this;
    }

    public String toString() {
        return "objectId=" + objectId +
                "storageClass=" + storageClass +
                "metadata=" + metadata +
                "storageEncryptionKey=" + storageEncryptionKey +
                super.toString();
    }

}
