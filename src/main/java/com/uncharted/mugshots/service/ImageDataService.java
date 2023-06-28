/*
 *
 * Copyright 2017 Uncharted Software Inc.
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
 *
 */

package com.uncharted.mugshots.service;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.AmazonS3URI;
import com.uncharted.mugshots.config.MugshotConfig;
import com.uncharted.mugshots.model.ImageData;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageDataService {


    private final MugshotConfig config;
    private AmazonS3 s3Client;

    @PostConstruct
    public void init() {
        AwsClientBuilder.EndpointConfiguration cfg = new AwsClientBuilder.EndpointConfiguration(config.getAwsUrlOverride(), "us-east-1");

        s3Client = AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(config.getAwsKeyId(), config.getAwsAccessKey())))
                .withEndpointConfiguration(cfg).build();

        var buckets = s3Client.listBuckets();
        int x = 0;
    }

    public void getImage(String url) {
        AmazonS3URI s3URI = new AmazonS3URI(url);

    }

    public void storeImage(ImageData image) throws IOException {


        File toS3 = File.createTempFile(image.getName(), "png");

        FileOutputStream outputStream = new FileOutputStream(toS3);
        outputStream.write(image.getImageData());

        var key = Arrays.hashCode(image.getImageData()); //might not want to do this?

        s3Client.putObject("mugshots", String.valueOf(key), toS3);

        //todo call out to python to get vector for ES
        //write image data to s3
    }

}
