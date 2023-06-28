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

import co.elastic.clients.elasticsearch._types.KnnQuery;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.AmazonS3URI;
import com.amazonaws.util.IOUtils;
import com.uncharted.mugshots.config.MugshotConfig;
import com.uncharted.mugshots.model.ImageData;
import com.uncharted.mugshots.model.Mugshot;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageDataService {

    private final MugshotConfig config;
    private final ElasticsearchService elasticsearchService;

    private AmazonS3Client s3Client;

    @PostConstruct
    public void init() {
        AwsClientBuilder.EndpointConfiguration cfg = new AwsClientBuilder.EndpointConfiguration(config.getAwsUrlOverride(), "us-east-1");

        s3Client = (AmazonS3Client) AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(config.getAwsKeyId(), config.getAwsAccessKey())))
                .withEndpointConfiguration(cfg).build();

        s3Client.createBucket(config.getAwsBucketName());
    }

    private byte[] getImage(String url) throws IOException {
        AmazonS3URI s3URI = new AmazonS3URI(url);
        var obj = s3Client.getObject(s3URI.getBucket(), s3URI.getKey());
        return IOUtils.toByteArray(obj.getObjectContent());
    }

    public List<byte[]> findByImage(MultipartFile image) throws IOException {

        var imageData = getImageDataFromFile(image);

        var vector = getVector(imageData);

        var query = new KnnQuery.Builder().field("vector").queryVector(vector).k(config.getNumberOfKnnResults()).numCandidates(config.getNumberOfKnnCandidates()).build();

        var results = elasticsearchService.getClient().search(s ->
                s.index(config.getEsIndex())
                        .size(10)
                        .knn(query), Mugshot.class);


        var urls = results.hits().hits().stream().map(Hit::source).filter(Objects::nonNull).map(Mugshot::getUrl).collect(Collectors.toList());

        var data = urls.stream().map(u -> {
            try {
                return getImage(u);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());

        return data;
    }

    public void storeImages(MultipartFile[] files) throws Exception {
        var mugshots = Arrays.stream(files).map(file -> {
            try {
                var data = getImageDataFromFile(file);
                var url = storeImageInS3(data);
                var vector = getVector(data);

                var mug = new Mugshot();
                mug.setName(data.getName());
                mug.setUrl(url);
                mug.setVector(vector);
                return mug;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());

        elasticsearchService.index(config.getEsIndex(), mugshots);
    }

    private ImageData getImageDataFromFile(MultipartFile file) throws IOException {
        ImageData data = new ImageData();
        data.setType(file.getContentType());
        data.setName(file.getOriginalFilename());
        data.setImageData(file.getBytes());
        return data;
    }


    private List<Float> getVector(ImageData image) {
        ArrayList<Float> vector = new ArrayList<>();
        Random rand = new Random();
        for (int i = 0; i < 512; i++) {
            vector.add(rand.nextFloat());
        }
        return vector;
    }

    private String storeImageInS3(ImageData image) throws IOException {
        File toS3 = File.createTempFile(image.getName(), "png");
        FileOutputStream outputStream = new FileOutputStream(toS3);
        outputStream.write(image.getImageData());
        var key = String.valueOf(Arrays.hashCode(image.getImageData())); //might not want to do this?
        s3Client.putObject(config.getAwsBucketName(), key, toS3);
        return s3Client.getResourceUrl(config.getAwsBucketName(), key);
    }

}
