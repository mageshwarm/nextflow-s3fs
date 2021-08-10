package com.upplication.s3fs.util;

import com.amazonaws.services.s3.model.SSEAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class CommonUtils {
    private static Logger log = LoggerFactory.getLogger(CommonUtils.class);

    public static boolean isValidString(String str) {
        return str != null && !str.trim().isEmpty();
    }

    public static boolean isAES256Enabled(String storageEncryption) {
        if ( SSEAlgorithm.AES256.getAlgorithm().equalsIgnoreCase(storageEncryption) ) {
            return true;
        }
        if( CommonUtils.isValidString(storageEncryption) && !isKMSEnabled(storageEncryption) ) {
            log.warn("Not a valid S3 server-side encryption type: `{}` -- Currently only aws:kms or AES256 is supported", storageEncryption);
        }
        return false;
    }

    public static boolean isKMSEnabled(String storageEncryption) {
        if ( SSEAlgorithm.KMS.getAlgorithm().equalsIgnoreCase(storageEncryption)) {
//            if(!CommonUtils.isValidString(props.getProperty("storage_encryption_key"))) {
//                log.warn("KMS key for S3 server-side encryption type: `{}` is not passed, so it will use default (aws/s3).", encryption);
//            }
            return true;

        } else if( CommonUtils.isValidString(storageEncryption) && !isAES256Enabled(storageEncryption) ) {
            log.warn("Not a valid S3 server-side encryption type: `{}` -- Currently only AES256 or aws:kms is supported", storageEncryption);
        }
        return false;
    }
}
